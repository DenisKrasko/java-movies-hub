package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
	private static int idCounter = 1;
	private final int id;
	private String title;
	private int year;

	public Movie(String title, int year) {
		this.id = idCounter++;
		this.title = title;
		this.year = year;
	}

	public String getTitle() {
		return title;
	}

	public int getYear() {
		return year;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Movie movie = (Movie) o;
		return year == movie.year && Objects.equals(title, movie.title);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(title);
		result = 31 * result + year;
		return result;
	}

	public int getId() {
		return id;
	}
}