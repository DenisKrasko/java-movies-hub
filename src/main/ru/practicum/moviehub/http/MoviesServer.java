package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoviesServer {
	private final HttpServer server;

	abstract class BaseHttpHandler implements HttpHandler {
		protected static final String CT_JSON = "application/json; charset=UTF-8";

		protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().set("Content-Type", CT_JSON);
			ex.sendResponseHeaders(status, bytes.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(bytes);
			}
		}

		protected void sendNoContent(HttpExchange ex, int status) throws java.io.IOException {
			ex.getResponseHeaders().set("Content-Type", CT_JSON);
			ex.sendResponseHeaders(status, -1);
		}

		protected void responseSender(HttpExchange ex, int status, String json) throws IOException {
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().set("Content-Type", CT_JSON);
			ex.sendResponseHeaders(status, bytes.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	class MoviesHandler extends BaseHttpHandler {
		private MoviesStore moviesStore;
		private Gson gson = new Gson();

		public MoviesHandler(MoviesStore moviesStore) {
			this.moviesStore = moviesStore;
		}

		@Override
		public void handle(HttpExchange ex) throws IOException {
			String method = ex.getRequestMethod();
			System.out.println("Началась обработка " + method + " /movies запроса от клиента.");
			if (method.equalsIgnoreCase("GET") && ex.getRequestURI().getPath().split("/").length == 2
					&& ex.getRequestURI().getQuery() == null) {
				handleGetRequest(ex);
			} else if (method.equalsIgnoreCase("POST") && ex.getRequestURI().getPath().split("/").length == 2) {
				handlePostRequest(ex);
			} else if (method.equalsIgnoreCase("GET") && ex.getRequestURI().getPath().split("/").length == 3 && ex.getRequestURI().getPath().split("/")[1].equals("movies")) {
				handleGetRequestWithId(ex);
			} else if (method.equalsIgnoreCase("DELETE") && ex.getRequestURI().getPath().split("/").length == 3) {
				handleDeleteRequestWithId(ex);
			} else if (method.equalsIgnoreCase("GET") && ex.getRequestURI().getQuery() != null) {
				handleGetRequestWithQuery(ex);
			}
		}

		private void handleGetRequestWithQuery(HttpExchange ex) throws IOException {

			if (ex.getRequestURI().getQuery().startsWith("year=")) {
				String query = ex.getRequestURI().getQuery();
				String yearStr = query.split("=")[1];
				if (isNotInteger(yearStr)) {
					responseSender(ex, 400, "Некорректный год. Здесь должны быть цифры");
				} else {
					int yearInt = Integer.parseInt(yearStr);
					if (hasMoviesFromYear(yearInt)) {
						Map<Movie, Integer> moviesYears = moviesStore.getMovies().entrySet().stream()
								.filter(movie -> movie.getKey().getYear() == yearInt)
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
						String jsonResponse = gson.toJson(moviesYears);
						sendJson(ex, 200, jsonResponse);
					} else {
						Map<Movie, Integer> moviesYears = new HashMap<>();
						String jsonResponse = gson.toJson(moviesYears);
						sendJson(ex, 200,jsonResponse);
					}
				}
			}
		}

		private void handleDeleteRequestWithId(HttpExchange ex) throws IOException {
			String path = ex.getRequestURI().getPath();
			String idRequest = path.split("/")[2];
			if (isNotInteger(idRequest)) {
				responseSender(ex, 400, "Некорректный ID");
			} else {
				int idRequestInt = Integer.parseInt(idRequest);
				if (moviesStore.containsMovieById(idRequestInt)) {
					moviesStore.delMovieById(idRequestInt);
					sendNoContent(ex, 204);
				} else {
					sendJson(ex, 404, "Фильм не найден");
				}
			}
		}

		private void handlePostRequest(HttpExchange ex) throws IOException {
			List<String> contentTypeValues = ex.getRequestHeaders().get("Content-Type");
			if ((contentTypeValues != null) && (!contentTypeValues.contains(CT_JSON))) {
				sendNoContent(ex, 415);
			}
			ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
			InputStream inputStream = ex.getRequestBody();
			String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			System.out.println("Тело запроса:\n" + body);
			JsonElement jsonElement = JsonParser.parseString(body);
			if (!jsonElement.isJsonObject()) {
				errorResponse.addDetail("запрос от клиента не соответствует ожидаемому JSON-объекту");
			}
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			if (!jsonObject.has("title")) {
				errorResponse.addDetail("JSON должен содержать поле title");
				String jsonError = gson.toJson(errorResponse);
				sendJson(ex, 422, jsonError);
				return;
			}
			if (!jsonObject.has("year")) {
				errorResponse.addDetail("JSON должен содержать поле year");
				String jsonError = gson.toJson(errorResponse);
				sendJson(ex, 422, jsonError);
				return;
			}
			String title = jsonObject.get("title").getAsString();
			int year = jsonObject.get("year").getAsInt();
			if (title.isEmpty()) {
				errorResponse.addDetail("название не должно быть пустым");
			}
			if (title.length() > 100) {
				errorResponse.addDetail("название не должно быть более 100 символов");
			}
			if (year < 1888 || year > (LocalDate.now().getYear() + 1)) {
				errorResponse.addDetail("год должен быть между 1888 и " + (LocalDate.now().getYear() + 1));
			}
			Movie movie = new Movie(title, year);
			if (moviesStore.containsMovieByMovie(movie)) {
				errorResponse.addDetail("такой фильм уже есть в списке");
			}
			if (!errorResponse.getDetails().isEmpty()) {
				String jsonError = gson.toJson(errorResponse);
				sendJson(ex, 422, jsonError);
			} else {
				moviesStore.addMovie(movie);
				String json = gson.toJson(movie);
				sendJson(ex, 201, json);
			}
		}

		private void handleGetRequest(HttpExchange ex) throws IOException {
			String json = gson.toJson(moviesStore.getMovies());
			sendJson(ex, 200, json);
		}

		private void handleGetRequestWithId(HttpExchange ex) throws IOException {
			String path = ex.getRequestURI().getPath();
			String idRequest = path.split("/")[2];
			if (isNotInteger(idRequest)) {
				responseSender(ex, 400, "Некорректный ID");
			} else {
				int idRequestInt = Integer.parseInt(idRequest);
				if (moviesStore.containsMovieById(idRequestInt)) {
					Movie movie = moviesStore.getMoviesById(idRequestInt);
					String jsonMovie = gson.toJson(movie);
					sendJson(ex, 200, jsonMovie);
				} else {
					sendJson(ex, 404, "Фильм не найден");
				}
			}
		}

		private boolean isNotInteger(String str) {
			if (str == null) {
				return true;
			}
			try {
				Integer.parseInt(str);
				return false;
			} catch (NumberFormatException e) {
				return true;
			}
		}

		private boolean hasMoviesFromYear(int targetYear) {
			return moviesStore.getMovies().keySet().stream()
					.anyMatch(movie -> movie.getYear() == targetYear);
		}
	}

	public MoviesServer(MoviesStore moviesStore, int port) {
		try {

			server = HttpServer.create(new InetSocketAddress(port), 0);  // создали сервер
			server.createContext("/movies", new MoviesHandler(moviesStore));
		} catch (IOException e) {
			throw new RuntimeException("Не удалось создать HTTP-сервер", e);
		}
	}

	public void start() {
		server.start();
		System.out.println("Сервер запущен");
	}

	public void stop() {
		server.stop(0);
		System.out.println("Сервер остановлен");
	}
}