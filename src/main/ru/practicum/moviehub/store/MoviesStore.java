package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
	private List<Movie> movies = new ArrayList<>();
	private int idSequence = 1;

	public void addMovie(Movie movie) {
		movie.setId(idSequence++);
		movies.add(movie);
	}

	public void addMovies(List<Movie> newMovies) {
		for (Movie movie : newMovies) {
			addMovie(movie);
		}
	}

	public Movie getMoviesById(int id) {
		for (Movie movie : movies) {
			if (movie.getId() == id) {
				return movie;
			}
		}
		return null;
	}

	public List<Movie> getMovies() {
		return this.movies;
	}

	public void clear() {
		this.movies.clear();
	}

	public boolean containsMovieById(int id) {
		for (Movie movie : movies) {
			if (movie.getId() == id) {
				return true;
			}
		}
		return false;
	}

	public void delMovieById(int id) {
		movies.removeIf(movie -> movie.getId() == id);
	}

	public boolean containsMovieByMovie(Movie movie) {
		return getMovies().contains(movie);
	}
}