package com.courseraproject.mutibo.http;

import com.courseraproject.mutibo.model.LoginType;
import com.courseraproject.mutibo.model.User;

//import retrofit.http.Body;
//import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public interface MutiboGameUserApi {
	public static final String PASSWORD_PARAMETER = "password";
	public static final String USERNAME_PARAMETER = "username";
	public static final String APP_TOKEN_PARAMETER = "app_token";
	public static final String AUTH_TYPE_PARAMETER = "auth_type";

	public static final String TOKEN_PATH = "/oauth/token";
	public static final String REGISTER_PATH = "/register";

	@POST(REGISTER_PATH)
	public User registerUser(@Query(AUTH_TYPE_PARAMETER) LoginType type,
			@Query(APP_TOKEN_PARAMETER) String appToken);
}
