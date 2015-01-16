package com.courseraproject.mutibo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.courseraproject.mutibo.model.TaskType;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class MovieDatabaseService extends IntentService {
	private static final String API_KEY = "ee38d73e2f28b166d8d8567c1f2b0ad2";
	public static final String CONFIGURATION_URL = "http://api.themoviedb.org/3/configuration?api_key="
			+ API_KEY;
	public static final String MOVIE_SEARCH_URL = "http://api.themoviedb.org/3/search/movie";

	public static final int CONFIG_MESSAGE = 1;
	public static final int SEARCH_MESSAGE = 2;
	public static final int CONNECTION_ERROR_CODE = 3;

	public MovieDatabaseService() {
		super("Movie Database worker service");
	}

	public MovieDatabaseService(String name) {
		super(name);
	}

	public static Intent makeIntent(Context context, Handler handler,
			String uri, TaskType type) {

		return DownloadUtils.makeMessengerIntent(context,
				MovieDatabaseService.class, handler, uri, type);
	}

	public static String getDatabaseRequestURL(String searchString) {
		try {
			return MOVIE_SEARCH_URL + "?api_key=" + API_KEY + "&query="
					+ URLEncoder.encode(searchString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(MovieDatabaseService.class.toString(), "Unsupported encoding");
			return null;
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MovieDatabaseService.class.toString(), "starting");
		Bundle extras = intent.getExtras();
		TaskType taskType = (TaskType) extras
				.getSerializable(DownloadUtils.TASK_TYPE_KEY);
		Messenger messenger = (Messenger) extras
				.get(DownloadUtils.MESSENGER_KEY);
		try {
			switch (taskType) {
			case DOWNLOAD_IMAGE:
				downloadImage(intent.getData(), messenger);
				break;
			case MOVIE_DATABASE_CONFIGURATION:
				getConfiguration(intent.getData(), messenger);
				break;
			case SEARCH_MOVIE:
				searchMovie(intent.getData(), messenger);
				break;
			default:
				break;
			}
		} catch(IOException e) {
			String errorDescription = getApplicationContext().getString(R.string.error_movie_server_unreachable) +
					getApplicationContext().getString(R.string.error_add_set);
			DownloadUtils.sendStringMessage(errorDescription, messenger, CONNECTION_ERROR_CODE);
		}
		
	}
	
	private void downloadImage(Uri url, Messenger messenger) throws IOException {
		final Context me = this.getApplicationContext();
		DownloadUtils.downloadAndRespond(me, url, messenger);
	}
	
	private void getConfiguration(Uri url, Messenger messenger) throws IOException {
		final Context me = this.getApplicationContext();
		DownloadUtils.getStringAndRespond(me, url, messenger,
				CONFIG_MESSAGE);
	}
	
	private void searchMovie(Uri url, Messenger messenger) throws IOException {
		final Context me = this.getApplicationContext();
		DownloadUtils.getStringAndRespond(me, url, messenger,
				SEARCH_MESSAGE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// it's a started service, so we do not really need it here
		return null;
	}

}
