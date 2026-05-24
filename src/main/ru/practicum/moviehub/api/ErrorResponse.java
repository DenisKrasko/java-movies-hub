package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
	private String error;
	private List<String> details;

	public ErrorResponse(String error) {
		this.error = error;
		details = new ArrayList<>();
	}

	public void addDetail(String detail) {
		details.add(detail);
	}

	public String getError() {
		return error;
	}

	public List<String> getDetails() {
		return details;
	}
}