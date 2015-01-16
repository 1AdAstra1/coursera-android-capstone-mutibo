package com.courseraproject.mutibo;

import java.lang.ref.WeakReference;

import java.util.EnumSet;
import java.util.HashMap;

import com.amazon.ags.api.AGResponseCallback;
import com.amazon.ags.api.AGResponseHandle;
import com.amazon.ags.api.AmazonGamesCallback;
import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.ags.api.AmazonGamesStatus;
import com.amazon.ags.api.leaderboards.LeaderboardsClient;
import com.amazon.ags.api.leaderboards.SubmitScoreResponse;
import com.courseraproject.mutibo.model.Game;
import com.courseraproject.mutibo.model.GameActionType;
import com.courseraproject.mutibo.model.Movie;
import com.courseraproject.mutibo.model.Set;
import com.courseraproject.mutibo.model.TaskType;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends ActionBarActivity {
	public static final String GAME_KEY = "CURRENT_GAME";

	AmazonGamesClient agsClient;

	AmazonGamesCallback callback = new AmazonGamesCallback() {
		@Override
		public void onServiceNotReady(AmazonGamesStatus status) {
			//unable to use service
		}
		@Override
		public void onServiceReady(AmazonGamesClient amazonGamesClient) {
			agsClient = amazonGamesClient;
		}
	};

	EnumSet<AmazonGamesFeature> gameFeatures = EnumSet.of(AmazonGamesFeature.Leaderboards);

	private enum ActionResult {
		CORRECT(0), INCORRECT(1), GAME_OVER(2);
		private int statusCode;
		ActionResult(int statusCode) {
			this.statusCode = statusCode;
		}

		public int getStatusCode() {
			return this.statusCode;
		}
	}

	private SharedPreferences settings;

	private Button submitButton;
	private Button nextButton;
	private Button backButton;
	private LinearLayout ratingContainer;
	private LinearLayout explanationContainer;
	private RadioGroup moviesContainer;
	private ImageView resultIcon;
	private TextView explanationText;
	private Game currentGame;
	private Set currentSet;
	private ActionBar actionBar;

	private Bitmap posterImage;

	private ImageView posterImageView;

	private static class MessageHandler extends Handler {

		// A weak reference to the enclosing class
		WeakReference<GameActivity> outerClass;

		public MessageHandler(GameActivity outer) {
			outerClass = new WeakReference<GameActivity>(outer);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {

			final GameActivity activity = outerClass.get();

			if (activity != null) {
				switch(msg.what) {
				case MutiboGameClientService.CONNECTION_ERROR_CODE:
					String errorDescription = activity.getString(R.string.error_game_server_unreachable) +
							activity.getString(R.string.error_game_play);
					AlertDialog.Builder alert  = new AlertDialog.Builder(activity);
					alert.setMessage(errorDescription);
					alert.setTitle(activity.getString(R.string.app_name));
					alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int whichButton) {
					    	activity.finish();
					    }
					});
					alert.setCancelable(false);
					alert.create().show();	
					break;
				case MutiboGameClientService.NEXT_SET_CODE:
					Set newSet = (Set) msg.getData().getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY);
					activity.setCurrentSet(newSet);
					activity.displaySet();
					break;
				case MutiboGameClientService.NO_SET_CODE:
					Toast.makeText(activity, (String) msg.getData().
							getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY), Toast.LENGTH_SHORT).show();
					activity.saveGameResult();
					activity.finish();
					break;
				case MutiboGameClientService.ACTION_CODE:
					activity.displayAnswerResult((HashMap<String, Integer>) msg.getData().
							getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY));
					break;
				case MutiboGameClientService.ALREADY_VOTED_CODE:
				case MutiboGameClientService.VOTE_CODE:
					activity.displayVoteResult((String) msg.getData().
							getSerializable(MutiboGameClientService.MESSAGE_DATA_KEY));
					break;
				default:
					activity.displayBitmap(msg.getData().getString(
							DownloadUtils.PATHNAME_KEY));
					break;
				}

			}
		}
	}

	MessageHandler handler = new MessageHandler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences(StartScreenActivity.PREFS_NAME, 0);
		setContentView(R.layout.activity_game);
		resultIcon = (ImageView) findViewById(R.id.resultIcon);
		ratingContainer = (LinearLayout) findViewById(R.id.ratingButtonsContainer);
		explanationContainer = (LinearLayout) findViewById(R.id.explanationContainer);
		moviesContainer = (RadioGroup) findViewById(R.id.movieButtonsContainer);
		submitButton = (Button) findViewById(R.id.submitButton);
		backButton = (Button) findViewById(R.id.backButton);
		nextButton = (Button) findViewById(R.id.nextButton);
		explanationText = (TextView) findViewById(R.id.explanationText);
		posterImageView = (ImageView) findViewById(R.id.moviePoster);
		Intent i = getIntent();
		currentGame = (Game) i.getSerializableExtra(GAME_KEY);
		getNewSet();
		displayGameData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
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
		if (agsClient == null) {
			AmazonGamesClient.initialize(this, callback, gameFeatures);
		}
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void onPause() {
	    super.onPause();
	    if (agsClient != null) {
	        agsClient.release();
	    }
	}

	@Override
	public void onDestroy() {
		//we need to get rid of the loaded image 
		//to avoid consuming too much memory
		if(posterImage != null) {
			posterImage.recycle();
		}		
		super.onDestroy();
	}

	public void submitAnswer(View v) {
		Integer checkedId = moviesContainer.getCheckedRadioButtonId();
		if (checkedId != null) {
			RadioButton checkedButton = (RadioButton) findViewById(checkedId);
			String guess = (String) checkedButton.getTag();
			Intent i = new Intent(this, MutiboGameClientService.class);
			Messenger messenger = new Messenger(handler);
			i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.ACTION);
			i.putExtra(MutiboGameClientService.GAME_ID_KEY, currentGame.getId());
			i.putExtra(MutiboGameClientService.SET_KEY, currentSet.getId());
			i.putExtra(MutiboGameClientService.ANSWER_KEY, guess);
			i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
			startService(i);
		}
	}
	
	public void saveGameResult() {
		int highScore = settings.getInt(StartScreenActivity.HIGH_SCORE_KEY, 0);
		int gameScore = currentGame.getScore();
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(StartScreenActivity.LAST_GAME_SCORE_KEY, gameScore);
		if (gameScore > highScore) {
			editor.putInt(StartScreenActivity.HIGH_SCORE_KEY, gameScore);
			if(agsClient != null) { //only send the result if GameCircle is connected
				LeaderboardsClient lbClient = agsClient.getLeaderboardsClient();
				AGResponseHandle<SubmitScoreResponse> handle = lbClient.submitScore("high_scores_leaderboard", gameScore);
				handle.setCallback(new AGResponseCallback<SubmitScoreResponse>() {
				    @Override
				    public void onComplete(SubmitScoreResponse result) {
				        if (result.isError()) {
				            Log.d(GameActivity.class.toString(), "Cannot submit score to the leaderboard: " + result.getError());
				        } 
				    }
				});
			}
		}
		editor.commit();
	}

	public void backToStartScreen(View v) {
		finish();
	}

	public void goToNextSet(View v) {
		Intent i = new Intent(this, GameActivity.class);
		i.putExtra(GAME_KEY, currentGame);
		startActivity(i);
	}

	public void rateSet(View v) {
		boolean vote = false;
		switch(v.getId()) {
		case R.id.voteUpButton:
			vote = true;
			break;
		case R.id.voteDownButton:
			break;
		}

		Messenger messenger = new Messenger(handler);
		Intent i = new Intent(this, MutiboGameClientService.class);
		i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.RATE_SET);
		i.putExtra(MutiboGameClientService.SET_KEY, currentSet.getId());
		i.putExtra(MutiboGameClientService.VOTE_KEY, vote);
		i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
		startService(i);
	}

	private void getNewSet() {
		Messenger messenger = new Messenger(handler);
		Intent i = new Intent(this, MutiboGameClientService.class);
		i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.NEXT_SET);
		i.putExtra(MutiboGameClientService.GAME_ID_KEY, currentGame.getId());
		i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
		startService(i);
	}

	public void displaySet() {
		String imageUrl = null;
		for (Movie m : currentSet.getMovies()) {
			RadioButton rb = new RadioButton(this);
			rb.setText(m.getTitle());
			rb.setTag(m.getId());
			moviesContainer.addView(rb);
			if (m.getId().equals(currentSet.getAnswer())) {
				imageUrl = m.getPosterUrl();
			}
		}
		moviesContainer.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				submitButton.setEnabled(true);
			}
		});
		// we download the image before it will actually be displayed, to avoid
		// slow loading
		if(imageUrl != null) {
			startService(MovieDatabaseService.makeIntent(
					this.getApplicationContext(), handler, imageUrl,
					TaskType.DOWNLOAD_IMAGE));
		}
		explanationText.setText(currentSet.getExplanation());
	}

	public void setCurrentSet(Set currentSet) {
		this.currentSet = currentSet;
	}

	private void displayGameData() {
		actionBar = getSupportActionBar();
		actionBar.setSubtitle(this.getString(R.string.game_status, 
				currentGame.getScore(), 
				currentGame.getWrongAnswers()));

	}

	public void displayAnswerResult(HashMap<String, Integer> result) {
		int status = result.get("status");
		Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		if (status == ActionResult.CORRECT.getStatusCode()) {
			currentGame.setScore(result.get("score"));
			resultIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.correct));
		} else {
			currentGame.incrementWrongAnswers();
			resultIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.incorrect));
		}
		explanationContainer.setVisibility(View.VISIBLE);
		posterImageView.startAnimation(fadeInAnimation);
		resultIcon.setVisibility(View.VISIBLE);
		resultIcon.startAnimation(fadeInAnimation);

		if (status == ActionResult.GAME_OVER.getStatusCode()) {
			saveGameResult();
			backButton.setVisibility(View.VISIBLE);
		} else {
			nextButton.setVisibility(View.VISIBLE);
		}
		if(result.get("hasBeenRated") == 0) {
			ratingContainer.setVisibility(View.VISIBLE);
		}
		submitButton.setEnabled(false);
		moviesContainer.setEnabled(false);
		displayGameData();
	}

	public void displayVoteResult(String statusText) {
		Toast.makeText(this, statusText, Toast.LENGTH_SHORT).show();
		ratingContainer.findViewById(R.id.voteDownButton).setEnabled(false);
		ratingContainer.findViewById(R.id.voteUpButton).setEnabled(false);
	}

	public void displayBitmap(String pathname) {
		posterImage = BitmapFactory.decodeFile(pathname);

		posterImageView.setImageBitmap(posterImage);
	}
}
