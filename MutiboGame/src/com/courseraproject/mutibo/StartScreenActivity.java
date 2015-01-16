package com.courseraproject.mutibo;

import java.lang.ref.WeakReference;
import java.util.EnumSet;

import com.amazon.ags.api.AmazonGamesCallback;
import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.ags.api.AmazonGamesStatus;
import com.amazon.ags.api.leaderboards.LeaderboardsClient;
import com.courseraproject.mutibo.model.Game;
import com.courseraproject.mutibo.model.GameActionType;
import com.courseraproject.mutibo.model.LoginType;
import com.courseraproject.mutibo.model.User;
import com.facebook.Session;
import com.facebook.widget.LoginButton;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class StartScreenActivity extends ActionBarActivity {
	public static final String PREFS_NAME = "UserPrefs";
	public static final String USER_TOKEN_KEY = "UserToken";
	public static final String USER_ID_KEY = "UserId";
	public static final String USER_NAME_KEY = "UserName";
	public static final String USER_LOGIN_TYPE_KEY = "UserLoginType";
	public static final String HIGH_SCORE_KEY = "HighScore";
	public static final String LAST_GAME_SCORE_KEY = "LastGameScore";

	private static SharedPreferences settings;

	private static class MessageHandler extends Handler {

		WeakReference<StartScreenActivity> outerClass;

		public MessageHandler(StartScreenActivity outer) {
			outerClass = new WeakReference<StartScreenActivity>(outer);
		}

		@Override
		public void handleMessage(Message msg) {
			final StartScreenActivity activity = outerClass.get();

			if (activity != null) {
				Bundle data = msg.getData();
				switch(msg.what) {
				case MutiboGameClientService.CONNECTION_ERROR_CODE:
					String errorDescription = activity.getString(R.string.error_game_server_unreachable) +
							activity.getString(R.string.error_game_play);
					Toast.makeText(activity, errorDescription, Toast.LENGTH_SHORT).show();
					//preventing locking the app on unfinished sign-in, when connection drops
					//between getting Facebook token and authenticating at Mutibo server
					if(!settings.contains(USER_ID_KEY) && Session.getActiveSession() != null) {
						activity.logout(null);
					}
					break;
				case MutiboGameClientService.REGISTER_USER_CODE:
					User u = (User) data.getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY);
					activity.authenticate(u);
					break;
				case MutiboGameClientService.START_GAME_CODE:
					Game newGame = (Game) data.getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY);
					activity.playGame(newGame);
					break;
				}
			}
		}
	}

	MessageHandler handler = new MessageHandler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences(PREFS_NAME, 0);
		checkAuthStatus();
		setContentView(R.layout.activity_start_screen);
		ActionBar ab = getSupportActionBar();
		ab.hide();
		TextView logo = (TextView) findViewById(R.id.gameTitle);
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/CaliBrush.otf");
		logo.setTypeface(font);
		changeAuthScreen();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	    
	}

	public void authenticate(User ul) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(USER_ID_KEY, ul.getId());
		editor.putString(USER_NAME_KEY, ul.getUsername());
		editor.putString(USER_TOKEN_KEY, ul.getPassword());
		editor.putString(USER_LOGIN_TYPE_KEY, String.valueOf(ul.getAuthType()));
		editor.putInt(HIGH_SCORE_KEY, ul.getHighScore());
		editor.putInt(LAST_GAME_SCORE_KEY, ul.getLastScore());
		editor.commit();
		changeAuthScreen();
	}


	public void changeAuthScreen() {
		Fragment activeFragment;
		FragmentManager fm = getSupportFragmentManager();
		if(checkAuthStatus()) {
			activeFragment = new MainFragment();
		} else {
			activeFragment = new AuthenticationFragment();
		}
		fm.beginTransaction().replace(R.id.container, activeFragment).commit();	
	}

	public void playGame(Game newGame) {
		Intent i = new Intent(this, GameActivity.class);
		i.putExtra(GameActivity.GAME_KEY, newGame);
		startActivity(i);
	}

	public void startGame(View v) {
		Intent i = new Intent(this, MutiboGameClientService.class);
		Messenger messenger = new Messenger(handler);
		i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.START_GAME);
		i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
		startService(i);
	}

	public void addSet(View v) {
		Intent i = new Intent(this, AddSetActivity.class);
		startActivity(i);
	}

	public void logout(View v) {
		LoginType type = LoginType.valueOf(settings.getString(USER_LOGIN_TYPE_KEY, LoginType.FACEBOOK.toString()));
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.commit();
		switch(type) {
		case FACEBOOK:
			//logging out from Facebook
			Session session = Session.getActiveSession();
			if (session != null) {
				session.closeAndClearTokenInformation();
			} else {
				session = Session.openActiveSession(this, false, null);
				if(session != null)
					session.closeAndClearTokenInformation();
			}
			Session.setActiveSession(null);
			break;
		default:
			break;
		}
		changeAuthScreen();
	}

	private boolean checkAuthStatus() {
		return settings.contains(USER_TOKEN_KEY);
	}


	/**
	 * A fragment with authenticated user's controls
	 */
	public static class MainFragment extends Fragment {
		private View rootView;
		
		AmazonGamesClient agsClient;

		AmazonGamesCallback callback = new AmazonGamesCallback() {
			@Override
			public void onServiceNotReady(AmazonGamesStatus status) {
			}
			@Override
			public void onServiceReady(AmazonGamesClient amazonGamesClient) {
				agsClient = amazonGamesClient;
				Button leaderboardButton = (Button) rootView.findViewById(R.id.leaderboardButton);
				leaderboardButton.setVisibility(View.VISIBLE);
				leaderboardButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						LeaderboardsClient lbClient = agsClient.getLeaderboardsClient();
						lbClient.showLeaderboardsOverlay();
					}
					
				});
			}
		};

		EnumSet<AmazonGamesFeature> gameFeatures = EnumSet.of(AmazonGamesFeature.Leaderboards);
		public MainFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			rootView = inflater.inflate(R.layout.fragment_start_screen,
					container, false);	
			return rootView;
		}

		@Override
		public void onResume() {
			super.onResume();
			TextView lastScoreView = (TextView) rootView.findViewById(R.id.lastGameScore);
			TextView highScoreView = (TextView) rootView.findViewById(R.id.highScore);

			int highScore = settings.getInt(HIGH_SCORE_KEY, 0);
			int lastScore = settings.getInt(LAST_GAME_SCORE_KEY, 0);

			lastScoreView.setText(getString(R.string.main_last_score, lastScore));
			highScoreView.setText(getString(R.string.main_high_score, highScore));
			AmazonGamesClient.initialize(this.getActivity(), callback, gameFeatures);
		}
		
		@SuppressWarnings("static-access")
		@Override
		public void onPause() {
		    super.onPause();
		    if (agsClient != null) {
		        agsClient.release();
		    }
		}

	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class AuthenticationFragment extends Fragment {
		private View rootView;

		public AuthenticationFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			rootView = inflater.inflate(R.layout.fragment_start_screen_authentication,
					container, false);	

			return rootView;
		}
		
		@Override
        public void onResume() {
            super.onResume();
            LoginButton authButton = (LoginButton) rootView.findViewById(R.id.authButton);
            View loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
            Session currentSession = Session.getActiveSession();
            if(currentSession != null && currentSession.isOpened()) {
                authButton.setVisibility(View.GONE);
                loadingIndicator.setVisibility(View.VISIBLE);
            } else {
                authButton.setVisibility(View.VISIBLE);
                authButton.setReadPermissions("email");
                loadingIndicator.setVisibility(View.GONE);
            }
        }
		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch(requestCode) {
		case Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE:
			Session currentSession = Session.getActiveSession();
			currentSession.onActivityResult(this, requestCode, resultCode, data);
			if(currentSession.isOpened()) {
				Messenger messenger = new Messenger(handler);
				Intent i = new Intent(this, MutiboGameClientService.class);
				i.putExtra(MutiboGameClientService.LOGIN_TYPE_KEY, LoginType.FACEBOOK);
				i.putExtra(MutiboGameClientService.APP_TOKEN_KEY, currentSession.getAccessToken());
				i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.REGISTER_USER);
				i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
				startService(i);
			} else {
				Toast.makeText(this, getString(R.string.com_facebook_requesterror_reconnect), 
						Toast.LENGTH_SHORT).show();
				Log.e("Facebook", "Login unsuccessful");
			}

		break;
		}
	}
}
