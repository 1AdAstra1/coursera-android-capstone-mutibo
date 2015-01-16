package com.courseraproject.mutibo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.courseraproject.mutibo.model.GameActionType;
import com.courseraproject.mutibo.model.Movie;
import com.courseraproject.mutibo.model.Set;
import com.courseraproject.mutibo.model.TaskType;

import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class AddSetActivity extends ActionBarActivity {
	private RadioGroup movieButtons;
	private MovieListAdapter autoCompleteAdapter;
	private AutoCompleteTextView searchBox;
	private EditText explanationBox;
	private Button addSetButton;
	private SharedPreferences settings;
	private Set currentSet;
	private static final String IMAGES_PATH_KEY = "IMAGES_PATH";
	private static final int MAX_MOVIES_IN_SET = 4;


	private static class MessageHandler extends Handler {
		WeakReference<AddSetActivity> outerClass;

		public MessageHandler(AddSetActivity outer) {
			outerClass = new WeakReference<AddSetActivity>(outer);
		}

		@Override
		public void handleMessage(Message msg) {

			final AddSetActivity activity = outerClass.get();

			if (activity != null) {
				switch(msg.what){
				case MutiboGameClientService.CONNECTION_ERROR_CODE:
					String gameErrorDescription = activity.getString(R.string.error_game_server_unreachable) +
							activity.getString(R.string.error_add_set);
					Toast.makeText(activity, gameErrorDescription, Toast.LENGTH_SHORT).show();
					break;
				case MovieDatabaseService.CONNECTION_ERROR_CODE:
					Toast.makeText(activity, msg.getData().getString(
							DownloadUtils.PATHNAME_KEY), Toast.LENGTH_SHORT).show();
					break;
				case MovieDatabaseService.CONFIG_MESSAGE:
					activity.saveImagesUrl(msg.getData().getString(
							DownloadUtils.PATHNAME_KEY));
					break;
				case MovieDatabaseService.SEARCH_MESSAGE:
					activity.populateList(msg.getData().getString(
							DownloadUtils.PATHNAME_KEY));
					break;
				case MutiboGameClientService.ADD_SET_CODE:
					Toast.makeText(activity, R.string.add_set_submit_message, Toast.LENGTH_SHORT).show();
					activity.finish();
					break;
				}
			}
		}
	}

	MessageHandler handler = new MessageHandler(this);

	private static class SearchInputWatcher implements TextWatcher {

		WeakReference<AddSetActivity> outerClass;
		MessageHandler handler;

		public SearchInputWatcher(AddSetActivity addSetActivity,
				MessageHandler handler) {
			outerClass = new WeakReference<AddSetActivity>(addSetActivity);
			this.handler = handler;
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() >= 4) {
				String searchUrl = MovieDatabaseService
						.getDatabaseRequestURL(String.valueOf(s));
				final AddSetActivity activity = outerClass.get();
				activity.startService(MovieDatabaseService.makeIntent(
						activity.getApplicationContext(), this.handler,
						searchUrl, TaskType.SEARCH_MOVIE));
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}
	}

	TextWatcher searcher;
	
	TextWatcher explanationChecker = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {}

		@Override
		public void afterTextChanged(Editable s) {
			checkSetCompleteness();
		}
	};
	
	OnItemClickListener movieChooser = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Movie boundMovie = (Movie) view.getTag();
			RadioButton rb = new RadioButton(getApplicationContext());
			rb.setText(boundMovie.getTitle());
			//style workaround
			rb.setTextColor(0xff000000);
			rb.setTag(boundMovie);
			if(movieButtons.getChildCount() < MAX_MOVIES_IN_SET) {
				movieButtons.addView(rb);
			}
			searchBox.setText("");
			checkSetCompleteness();
		}
		
	};
	
	OnCheckedChangeListener answerSelector = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			checkSetCompleteness();
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_set);
		settings = getPreferences(MODE_PRIVATE);
		checkImageSettings();
		
		movieButtons = (RadioGroup) findViewById(R.id.addMovieButtonsContainer);
		movieButtons.setOnCheckedChangeListener(answerSelector);
		
		explanationBox = (EditText) findViewById(R.id.explanation);
		explanationBox.addTextChangedListener(explanationChecker);
		
		addSetButton = (Button) findViewById(R.id.addSetButton);
		
		searchBox = (AutoCompleteTextView) findViewById(R.id.movieSearchBox);
		searchBox.setThreshold(4);

		autoCompleteAdapter = new MovieListAdapter(this,
				android.R.layout.simple_dropdown_item_1line, new ArrayList<Movie>());
		autoCompleteAdapter.setNotifyOnChange(true);
		searchBox.setAdapter(autoCompleteAdapter);
		searcher = new SearchInputWatcher(this, handler);
		searchBox.addTextChangedListener(searcher);
		searchBox.setOnItemClickListener(movieChooser);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.add_set, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void addSet(View v) {
		Messenger messenger = new Messenger(handler);
		Intent i = new Intent(this, MutiboGameClientService.class);
		i.putExtra(MutiboGameClientService.ACTION_TYPE_KEY, GameActionType.ADD_SET);
		i.putExtra(MutiboGameClientService.SET_KEY, currentSet);
		i.putExtra(DownloadUtils.MESSENGER_KEY, messenger);
		startService(i);
	}

	public void populateList(String jsonString) {
		JSONObject data = DownloadUtils.getJSONData(jsonString);
		JSONArray resultsArray;
		String movieUrl = null;
		try {
			resultsArray = data.getJSONArray("results");
			if (resultsArray.length() == 0)
				return;
			autoCompleteAdapter.clear();
			for (int i = 0; i < resultsArray.length(); i++) {
				JSONObject res = resultsArray.getJSONObject(i);
				if(res.getString("poster_path") != null && res.getString("poster_path") != "null") {
					movieUrl = settings.getString(IMAGES_PATH_KEY, "") + res.getString("poster_path");
				}
				Movie m = new Movie(String.valueOf(res.getInt("id")),
						res.getString("title"), movieUrl);
				autoCompleteAdapter.add(m);
			}
		} catch (JSONException e) {
			Log.e(AddSetActivity.class.toString(), "Unparsable search results JSON");
		}

	}
	
	public void saveImagesUrl(String jsonString) {
		JSONObject data = DownloadUtils.getJSONData(jsonString);
		try {
			String baseUrl = data.getJSONObject("images").getString("base_url");
			String posterWidth = data.getJSONObject("images").getJSONArray("poster_sizes").getString(3);
			Editor editor = settings.edit();
			Log.d(AddSetActivity.class.toString(), "new images base URL: " + baseUrl + posterWidth);
			editor.putString(IMAGES_PATH_KEY, baseUrl + posterWidth);
			editor.commit();
		} catch (JSONException e) {
			e.printStackTrace();
		}		
	}
	
	private void checkImageSettings() {
		if(!settings.contains(IMAGES_PATH_KEY)) {
			startService(MovieDatabaseService.makeIntent(
					getApplicationContext(), 
					handler,
					MovieDatabaseService.CONFIGURATION_URL, TaskType.MOVIE_DATABASE_CONFIGURATION));
		}
	}
	
	private void checkSetCompleteness() {
		Integer selection = movieButtons.getCheckedRadioButtonId();
		String explanation = String.valueOf(explanationBox.getText().toString().trim());
		String answer = null;
		if(movieButtons.getChildCount() == MAX_MOVIES_IN_SET && selection != -1 && explanation.length() > 0) {
			ArrayList<Movie> movies = new ArrayList<Movie>();
			for(int i = 0; i < movieButtons.getChildCount(); i++) {
				RadioButton rb = (RadioButton) movieButtons.getChildAt(i);
				Movie m = (Movie) rb.getTag();
				movies.add(m);
				if(rb.getId() == selection) {
					answer = m.getId();
				}
			}
			currentSet = new Set(movies, answer, explanation);
			addSetButton.setEnabled(true);
		} else {
			currentSet = null;
			addSetButton.setEnabled(false);
		}
	}
}
