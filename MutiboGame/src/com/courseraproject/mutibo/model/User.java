package com.courseraproject.mutibo.model;

import java.io.Serializable;


public class User implements Serializable {
	private static final long serialVersionUID = -6278414338887778630L;
	private long id;
	private int lastScore = 0;
	private int highScore = 0;
	private int numGames = 0;

	private String username;
	private String photoUrl;
	
	public User(long id, int lastScore, int highScore, int numGames,
			String name, String photoUrl, String password, LoginType authType,
			String accessToken, String externalId) {
		super();
		this.id = id;
		this.lastScore = lastScore;
		this.highScore = highScore;
		this.numGames = numGames;
		this.username = name;
		this.photoUrl = photoUrl;
		this.password = password;
		this.authType = authType;
		this.accessToken = accessToken;
		this.externalId = externalId;
	}
	
	public long getId() {
		return id;
	}

	public int getLastScore() {
		return lastScore;
	}
	public void setLastScore(int lastScore) {
		this.lastScore = lastScore;
	}
	public int getHighScore() {
		return highScore;
	}
	public void setHighScore(int highScore) {
		this.highScore = highScore;
	}
	public int getNumGames() {
		return numGames;
	}

	public String getUsername() {
		return username;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public String getPassword() {
		return password;
	}

	public LoginType getAuthType() {
		return authType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getExternalId() {
		return externalId;
	}

	private String password;
	
	private LoginType authType;

	private String accessToken;
	private String externalId;
}
