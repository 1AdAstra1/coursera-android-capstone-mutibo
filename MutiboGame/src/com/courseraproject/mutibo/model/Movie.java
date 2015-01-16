package com.courseraproject.mutibo.model;

import java.io.Serializable;

public class Movie implements Serializable {

	private static final long serialVersionUID = 1844285270980297132L;
	private String id;
	private String title;
	private String posterUrl;
	
	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public Movie(String id, String title, String imageUrl) {
		super();
		this.id = id;
		this.title = title;
		this.posterUrl = imageUrl;
	}	
	
}
