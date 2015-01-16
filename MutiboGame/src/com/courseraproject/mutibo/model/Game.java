package com.courseraproject.mutibo.model;

import java.io.Serializable;

public class Game implements Serializable{
	private static final long serialVersionUID = -1314641520546614338L;
	private long id;
	private int score = 0;
	private int wrongAnswers = 0;
	
	public Game(long id) {
		super();
		this.id = id;
	}
	
	public Game(long id, int score, int wrongAnswers) {
		super();
		this.id = id;
		this.score = score;
		this.wrongAnswers = wrongAnswers;
	}
	
	public long getId() {
		return id;
	}

	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public int getWrongAnswers() {
		return wrongAnswers;
	}
	
	public void incrementWrongAnswers() {
		this.wrongAnswers += 1;
	}
}
