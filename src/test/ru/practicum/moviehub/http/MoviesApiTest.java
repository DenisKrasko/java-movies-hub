package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
	private static final String BASE = "http://localhost:8080";
	private static MoviesServer server;
	private static HttpClient client;
	private static MoviesStore moviesStore = new MoviesStore();
	private Gson gson = new Gson();

	@BeforeAll
	static void beforeAll() {
		client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(2))
				.build();
		server = new MoviesServer(moviesStore, 8080);
		server.start();
	}

	@BeforeEach
	void beforeEach() {
		moviesStore.clear();
	}

	@AfterAll
	static void afterAll() {
		server.stop();
	}

	@Test
	void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		assertEquals("{}", body, "Ожидается передача пустого JSON-объекта");
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() == 2,
				"Ожидается пустой JSON-массив");
	}

	@Test
	void getMovies_whenNotEmpty_returnsNotEmptyArray() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println(body);
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() > 2,
				"Ожидается JSON-массив");
	}

	@Test
	void addMoviesInStore_whenNoEmpty_returnMovieInResponseBody() throws Exception {
		String title = "Inception";
		int year = 1958;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() > 2,
				"Ожидается JSON-массив");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie = new Movie("Inception", 1958);
		expMap.put(movie, moviesStore.getMovies().get(movie));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должен быть фильм Inception, 1958 года");
		String respBody = resp.body();
		Movie movieFromResp = new Gson().fromJson(respBody, Movie.class);
		assertTrue(movieFromResp.getId() > 0);
	}

	@Test
	void notAddMovies_whenEmptyTitle_ReturnError() throws Exception {
		String title = "";
		int year = 1958;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "HashMap movieStore должен быть пустым");

	}

	@Test
	void notAddMovies_whenTitleMore100Symbols_ReturnError() throws Exception {
		String title = "asd".repeat(101);
		int year = 1985;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "HashMap movieStore должен быть пустым");
	}

	@Test
	void notAddMovies_whenYearMore2027_ReturnError() throws Exception {
		String title = "Sun";
		int year = 2028;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "HashMap movieStore должен быть пустым");
	}

	@Test
	void addMovies_whenYearMore2027_ReturnMovie() throws Exception {
		String title = "Inception";
		int year = 2027;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() > 2,
				"Ожидается JSON-массив");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie = new Movie("Inception", 2027);
		expMap.put(movie, moviesStore.getMovies().get(movie));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должен быть фильм Inception, 2027 года");
		String respBody = resp.body();
		Movie movieFromResp = new Gson().fromJson(respBody, Movie.class);
		assertTrue(movieFromResp.getId() > 0);
	}

	@Test
	void notAddMovies_whenYearLess1888_ReturnError() throws Exception {
		String title = "Sun";
		int year = 1887;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "HashMap movieStore должен быть пустым");
	}

	@Test
	void addMovies_whenYearLess1888_ReturnMovie() throws Exception {
		String title = "Inception";
		int year = 1888;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() > 2,
				"Ожидается JSON-массив");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie = new Movie("Inception", 1888);
		expMap.put(movie, moviesStore.getMovies().get(movie));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должен быть фильм Inception, 1888 года");
		String respBody = resp.body();
		Movie movieFromResp = new Gson().fromJson(respBody, Movie.class);
		assertTrue(movieFromResp.getId() > 0);
	}

	@Test
	void notAddMovies_whenTypeIsWrong_ReturnError() throws Exception {
		String title = "Sun";
		int year = 1887;
		String requestBody = String.format("{\"title\":\"%s\", \"year\": %s}", title, year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-16")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertNotEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "ArrayList movieStore должен быть пустым");
	}

	@Test
	void notAddMovies_whenNoYearValue_ReturnError() throws Exception {
		String title = "Sun";
		String requestBody = String.format("{\"title\":\"%s\"}", title);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "ArrayList movieStore должен быть пустым");
	}

	@Test
	void notAddMovies_whenNoTitleValue_ReturnError() throws Exception {
		int year = 1887;
		String requestBody = String.format("{\"year\": %s}", year);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		System.out.println("Тело ответа:\n" + resp.body());
		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		assertTrue(moviesStore.getMovies().isEmpty(), "ArrayList movieStore должен быть пустым");
	}

	@Test
	void getMovie_whenMovieInStore_returnsMovieForId() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		int idFromPath = moviesStore.getMovies().get(movie2);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, resp.statusCode(), "GET /movies/2 должен вернуть 200");
		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		String jsonString = resp.body();
		Movie movie = gson.fromJson(jsonString, Movie.class);
		String title = movie.getTitle();
		int year = movie.getYear();
		assertEquals("Batman", title, "Ожидается название фильма по ID: Batman");
		assertEquals(1995, year, "Ожидается год фильмы по ID: 1995");
	}

	@Test
	void getNoMovie_whenMovieNotInStore_returnsError() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		int idFromPath = 4;
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(404, resp.statusCode(), "GET /movies/3 должен вернуть 404");
		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertEquals("Фильм не найден", body, "В теле ответа должно быть сообщение: Фильм не найден");
	}

	@Test
	void getNoMovie_whenIdIsNotInteger_returnsError() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		String idFromPath = "sd";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(400, resp.statusCode(), "GET /movies/sd должен вернуть 400");
		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertEquals("Некорректный ID", body, "В теле ответа должно быть сообщение: Некорректный ID");
	}

	@Test
	void d1elMoviesInStoreById_whenNoEmpty() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		String idFromPath = movie1.getId() + "";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.DELETE()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie = new Movie("Batman", 1995);
		expMap.put(movie, moviesStore.getMovies().get(movie));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должен быть фильм Batman, 1995 года");
	}

	@Test
	void notDelMoviesInStoreById_whenNoMovieIdInStore_returnError() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		String idFromPath = 195 + "";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.DELETE()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(404, resp.statusCode(), "DELETE /movies/195 должен вернуть 404");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie3 = new Movie("Inception", 2010);
		Movie movie4 = new Movie("Batman", 1995);
		expMap.put(movie3, moviesStore.getMovies().get(movie3));
		expMap.put(movie4, moviesStore.getMovies().get(movie4));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должны быть 2 фильма");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertEquals("Фильм не найден", body, "В теле ответа должно быть сообщение: Фильм не найден");
	}

	@Test
	void notDelMoviesInStoreById_whenNoMovieIdIsString_returnError() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		String idFromPath = "sdf";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + idFromPath))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.DELETE()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(400, resp.statusCode(), "DELETE /movies/sdf должен вернуть 400");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie3 = new Movie("Inception", 2010);
		Movie movie4 = new Movie("Batman", 1995);
		expMap.put(movie3, moviesStore.getMovies().get(movie3));
		expMap.put(movie4, moviesStore.getMovies().get(movie4));
		assertEquals(expMap, moviesStore.getMovies(), "В HashMap movieStore должны быть 2 фильма");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertEquals("Некорректный ID", body, "В теле ответа должно быть сообщение: Некорректный ID");
	}

	@Test
	void getArrayMovies_whenYearIsValid_returnArrayMovies() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		Movie movie3 = new Movie("Batman2", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		moviesStore.addMovie(movie3);
		String year = "1995";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies" + "?year=" + year))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(200, resp.statusCode(), "GET /movies/1995 должен вернуть 200");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		Map<Movie, Integer> expMap = new HashMap<>();
		Movie movie4 = new Movie("Batman", 1995);
		Movie movie5 = new Movie("Batman2", 1995);
		expMap.put(movie4, moviesStore.getMovies().get(movie4));
		expMap.put(movie5, moviesStore.getMovies().get(movie5));
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() > 2,
				"Ожидается JSON-объект");
	}

	@Test
	void notGetArrayMovies_whenYearIsNotValid_returnArrayMovies() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		Movie movie3 = new Movie("Batman2", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		moviesStore.addMovie(movie3);
		String year = "asd";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies?year=" + year))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(400, resp.statusCode(), "GET /movies/asd должен вернуть 400");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		assertEquals("Некорректный год. Здесь должны быть цифры", body, "В теле ответа должно быть сообщение: Некорректный год");
	}

	@Test
	void getEmptyMapMovies_whenNoHaveMoviesWithThisYear_returnEmptyHashMap() throws Exception {
		Movie movie1 = new Movie("Inception", 2010);                 // Создаём обычный Java-объект Movie
		Movie movie2 = new Movie("Batman", 1995);
		Movie movie3 = new Movie("Batman2", 1995);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		moviesStore.addMovie(movie3);
		String year = "1998";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies?year=" + year))
				.header("Content-Type", "application/json; charset=UTF-8")      // Устанавливаем заголовок
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // Отправляем запрос
		assertEquals(200, resp.statusCode(), "GET /movies/1998 должен вернуть 200");
		String contentTypeHeaderValue =
				req.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");
		String body = resp.body().trim();
		System.out.println("Тело ответа:\n" + body);
		Map<Movie, Integer> moviesYears = new HashMap<>();
		Map<Movie, Integer> pars = gson.fromJson(resp.body(), new ListOfMoviesTypeToken());
		assertEquals(moviesYears, pars, "В теле должен быть объект HashMap пустой");
		assertTrue(body.startsWith("{") && body.endsWith("}") && body.length() == 2,
				"Ожидается JSON-объект");
	}
}