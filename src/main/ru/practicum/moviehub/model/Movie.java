package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
	private int id;
	private String title;
	private int year;

	public Movie(String title, int year) {
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
		return Objects.hash(year, title);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setYear(int year) {
		this.year = year;
	}
}