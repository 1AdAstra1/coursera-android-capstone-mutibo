package com.courseraproject.mutibo.http;

import java.util.HashMap;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.PUT;
import retrofit.http.Path;

import com.courseraproject.mutibo.model.Game;
import retrofit.http.GET;
import com.courseraproject.mutibo.model.Set;

import retrofit.http.POST;
import retrofit.http.Query;

public interface MutiboGameApi {
	public static final String PASSWORD_PARAMETER = "password";
	public static final String USERNAME_PARAMETER = "username";
	public static final String ANSWER_PARAMETER = "answer";
	public static final String VOTE_PARAMETER = "vote";

	public static final String TOKEN_PATH = "/oauth/token";
	public static final String GAME_PATH = "/game";
	public static final String SET_PATH = "/set";

	@POST(SET_PATH)
	public Set addSet(@Body Set newSet);
	
	@PUT(SET_PATH + "/{id}")
	public void rateSet(@Path("id") long setId, @Query(VOTE_PARAMETER) boolean vote, Callback<Object> cb);
	
	@POST(GAME_PATH)
	public Game startGame();
	
	@GET(GAME_PATH + "/{id}" + SET_PATH)
	public Set getNextSet(@Path("id") long gameId);
	
	@POST(GAME_PATH + "/{id}" + SET_PATH + "/{set_id}")
	public HashMap<String, Integer> gameAction(@Path("id") long gameId, 
			@Path("set_id") long setId, @Query(ANSWER_PARAMETER) String answer);
}