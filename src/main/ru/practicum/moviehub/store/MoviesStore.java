package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
	private Map<Movie, Integer> movies = new HashMap<>();

	public void addMovie(Movie movie) {
		movies.put(movie,movie.getId());
	}

	public void delMovieById(int id) {
		movies.entrySet().removeIf(entry -> entry.getValue() == id);
	}

	public Movie getMoviesById(int id) {
		return movies.entrySet().stream()
				.filter(entry -> entry.getValue() == id)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public Map<Movie, Integer> getMovies() {
		return this.movies;
	}

	public void clear() {
		this.movies.clear();
	}

	public boolean containsMovieById (int id) {
		return movies.containsValue(id);
	}

	public boolean containsMovieByMovie(Movie movie) {
		return getMovies().containsKey(movie);
	}
}