package com.courseraproject.mutibo.model;

import java.io.Serializable;
import java.util.ArrayList;

public class Set implements Serializable {

	private static final long serialVersionUID = 3713942724149933850L;
	private long id;
	private ArrayList<Movie> movies;
	private String answer;
	private String explanation;
	
	public long getId() {
		return id;
	}

	public ArrayList<Movie> getMovies() {
		return movies;
	}

	public String getAnswer() {
		return answer;
	}

	public String getExplanation() {
		return explanation;
	}
	
	public Set(ArrayList<Movie> movies, String answer,
			String explanation) {
		this.movies = movies;
		this.answer = answer;
		this.explanation = explanation;
	}

	public Set(long id, ArrayList<Movie> movies, String answer,
			String explanation) {
		this(movies, answer, explanation);
		this.id = id;
	}
}
