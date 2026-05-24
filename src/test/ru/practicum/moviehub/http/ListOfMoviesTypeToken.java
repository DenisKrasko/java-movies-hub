package ru.practicum.moviehub.http;

import com.google.gson.reflect.TypeToken;
import ru.practicum.moviehub.model.Movie;
import java.util.HashMap;

public class ListOfMoviesTypeToken extends TypeToken<HashMap<Movie, Integer>> {
}