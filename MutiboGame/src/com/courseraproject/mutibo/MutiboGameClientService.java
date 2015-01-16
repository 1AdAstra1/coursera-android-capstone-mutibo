package com.courseraproject.mutibo;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.courseraproject.mutibo.http.EasyHttpClient;
import com.courseraproject.mutibo.http.MutiboGameApi;
import com.courseraproject.mutibo.http.MutiboGameUserApi;
import com.courseraproject.mutibo.http.SecuredRestBuilder;
import com.courseraproject.mutibo.model.Game;
import com.courseraproject.mutibo.model.GameActionType;
import com.courseraproject.mutibo.model.LoginType;
import com.courseraproject.mutibo.model.Set;
import com.courseraproject.mutibo.model.User;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;
import retrofit.client.ApacheClient;
import retrofit.client.Response;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MutiboGameClientService extends IntentService {
	private final static String CLIENT_ID = "mobile";

	public final static String ACTION_TYPE_KEY = "ACTION_TYPE";
	public final static String LOGIN_TYPE_KEY = "LOGIN_TYPE";
	public final static String APP_TOKEN_KEY = "APP_TOKEN";
	public final static String SET_KEY = "MOVIE_SET";
	public final static String GAME_ID_KEY = "GAME_ID";
	public final static String ANSWER_KEY = "ANSWER";
	public final static String VOTE_KEY = "VOTE";
	public final static String MESSAGE_DATA_KEY = "MESSAGE_DATA";

	public final static int CONNECTION_ERROR_CODE = 100;
	public final static int REGISTER_USER_CODE = 101;
	public final static int ADD_SET_CODE = 102;
	public final static int START_GAME_CODE = 103;
	public final static int NEXT_SET_CODE = 104;
	public final static int NO_SET_CODE = 105;
	public final static int ACTION_CODE = 106;
	public final static int VOTE_CODE = 107;
	public final static int ALREADY_VOTED_CODE = 108;
	
	public final static int CONNECTION_TIMEOUT = 5000;
	public final static int SOCKET_TIMEOUT = 5000;
	
	private String serverUrl;

	private static MutiboGameUserApi gameUserApi;
	private static MutiboGameApi gameActionApi;

	private SharedPreferences settings;
	private EasyHttpClient httpClient = new EasyHttpClient();

	public MutiboGameClientService() {
		super("Mutibo game worker service");
	}

	public MutiboGameClientService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
		serverUrl = getApplicationContext().getString(R.string.mutibo_server_url);
		settings = getApplicationContext().
				getSharedPreferences(StartScreenActivity.PREFS_NAME, MODE_PRIVATE);
		gameUserApi = new RestAdapter.Builder().
				setEndpoint(serverUrl).
				setClient(new ApacheClient(httpClient)).
				setLogLevel(LogLevel.FULL).build().create(MutiboGameUserApi.class);
		if (settings.contains(StartScreenActivity.USER_TOKEN_KEY)) {
			setGameApi(settings.getString(StartScreenActivity.USER_NAME_KEY, null),
					settings.getString(StartScreenActivity.USER_TOKEN_KEY, null));
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void sendMessage(Messenger messenger, Serializable input, int what) {
		Message msg = Message.obtain();
		Bundle data = new Bundle();
		data.putSerializable(MESSAGE_DATA_KEY, input);
		msg.what = what;
		msg.setData(data);
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void setGameApi(String username, String password) {
		gameActionApi = new SecuredRestBuilder().
				setEndpoint(serverUrl).
				setLoginEndpoint(serverUrl + MutiboGameApi.TOKEN_PATH).
				setUsername(username).
				setPassword(password).
				setClientId(CLIENT_ID).
				setClient(new ApacheClient(httpClient)).
				setLogLevel(LogLevel.FULL).build().create(MutiboGameApi.class);	
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MutiboGameClientService.class.toString(), "intent received");
		Bundle extras = intent.getExtras();
		GameActionType action = (GameActionType) extras.getSerializable(ACTION_TYPE_KEY);
		final Messenger messenger = (Messenger) extras
				.get(DownloadUtils.MESSENGER_KEY);
		try {
			switch(action) {
			case REGISTER_USER:
				registerUser(extras, messenger);
				break;
			case ADD_SET:
				addSet(extras, messenger);
				break;
			case START_GAME:
				startGame(extras, messenger);
				break;
			case NEXT_SET:
				nextSet(extras, messenger);
				break;
			case ACTION:
				action(extras, messenger);
				break;
			case RATE_SET:
				rateSet(extras, messenger);			
				break;
			default:
				break;
			}
		} catch(RetrofitError e) {
			sendMessage(messenger, null, CONNECTION_ERROR_CODE);
		}		
	}
	
	private void registerUser(Bundle extras, final Messenger m) {
		LoginType type = (LoginType) extras.getSerializable(LOGIN_TYPE_KEY);
		String appToken = extras.getString(APP_TOKEN_KEY);
		User u = gameUserApi.registerUser(type, appToken);
		sendMessage(m, u, REGISTER_USER_CODE);
		setGameApi(u.getUsername(), u.getPassword());
	}
	
	private void addSet(Bundle extras, final Messenger m) {
		Set newSet = (Set) extras.getSerializable(SET_KEY);
		newSet = gameActionApi.addSet(newSet);
		sendMessage(m, newSet, ADD_SET_CODE);
	}
	
	private void startGame(Bundle extras, final Messenger m) {
		Game newGame = gameActionApi.startGame();
		sendMessage(m, newGame, START_GAME_CODE);
	}
	
	private void nextSet(Bundle extras, final Messenger m) {
		long gameId = extras.getLong(GAME_ID_KEY);
		try {
			Set nextSet = gameActionApi.getNextSet(gameId);
			sendMessage(m, nextSet, NEXT_SET_CODE);
		} catch(Exception e) {
			sendMessage(m, getApplicationContext().getString(R.string.game_no_more_sets), NO_SET_CODE);
		}
	}
	
	private void action(Bundle extras, final Messenger m) {
		long actionGameId = extras.getLong(GAME_ID_KEY);
		long setId = extras.getLong(SET_KEY);
		String answer = extras.getString(ANSWER_KEY);
		HashMap<String, Integer> results = gameActionApi.gameAction(actionGameId, setId, answer);
		sendMessage(m, results, ACTION_CODE);
	}
	
	private void rateSet(Bundle extras, final Messenger m) {
		long rateSetId = extras.getLong(SET_KEY);
		boolean vote = extras.getBoolean(VOTE_KEY);		 

		gameActionApi.rateSet(rateSetId, vote, new Callback<Object>() {
			int resultCode;
			@Override
			public void success(Object o, Response response) {	
				resultCode = VOTE_CODE;
				sendMessage(m, getApplicationContext().getString(R.string.game_voted), resultCode);
			}

			@Override
			public void failure(RetrofitError retrofitError) {
				resultCode = ALREADY_VOTED_CODE;
				sendMessage(m, getApplicationContext().getString(R.string.game_not_voted), resultCode);
			}
		});
	}

}
