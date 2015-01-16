package com.courseraproject.mutibo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;

import com.courseraproject.mutibo.http.EasyHttpClient;
import com.courseraproject.mutibo.model.TaskType;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * @class DownloadUtils
 *
 * @brief This class encapsulates several static methods so that all
 *        Services can access them without redefining them in each
 *        Service.
 */
public class DownloadUtils {
    /**
     * Used for debugging.
     */
    static final String TAG = "DownloadUtils";
    
    /**
     * The key used to store/retrieve a Messenger extra from a Bundle.
     */
	public static final String MESSENGER_KEY = "MESSENGER";
	
	/**
     * The key used to store/retrieve a file's pathname from a Bundle.
     */
	public static final String PATHNAME_KEY = "PATHNAME";
	
	public static final String TASK_TYPE_KEY = "TaskType";
    

    
    /**
     * Make an Intent which will start a service if provided as a
     * parameter to startService().
     * 
     * @param context		The context of the calling component
     * @param service		The class of the service to be
     *                          started. (For example, ThreadPoolDownloadService.class) 
     * @param handler		The handler that the service should
     *                          use to return a result. 
     * @param uri		The web URL that the service should download
     * 
     * This method is an example of the Factory Method Pattern,
     * because it creates and returns a different Intent depending on
     * the parameters provided to it.
     * 
     * The Intent is used as the Command Request in the Command
     * Processor Pattern when it is passed to the
     * ThreadPoolDownloadService using startService().
     * 
     * The Handler is used as the Proxy, Future, and Servant in the
     * Active Object Pattern when it is passed a message, which serves
     * as the Active Object, and acts depending on what the message
     * contains.
     * 
     * The handler *must* be wrapped in a Messenger to allow it to
     * operate across process boundaries.
     */
    public static Intent makeMessengerIntent(Context context,
                                             Class<?> service,
                                             Handler handler,
                                             String uri,
                                             TaskType type) {
    	Messenger messenger = new Messenger(handler);
    	
    	
    	Intent intent = new Intent(context,
                                   service);
    	intent.putExtra(MESSENGER_KEY, 
                        messenger);
    	intent.putExtra(TASK_TYPE_KEY, type);
    	intent.setData(Uri.parse(uri));
        return intent;
    }

    /**
     *	Use the provided Messenger to send a Message to a Handler in
     *	another process.
     *
     * 	The provided string, outputPath, should be put into a Bundle
     * 	and included with the sent Message.
     */
    public static void sendStringMessage (String toSend,
                                 Messenger messenger, int what) {
        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putString(PATHNAME_KEY,
                       toSend);
        msg.what = what;
        
        // Make the Bundle the "data" of the Message.
        msg.setData(data);
        
        try {
            // Send the Message back to the client Activity.
            messenger.send(msg);
            Log.d(DownloadUtils.class.toString(), "message sent to actity");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Download a file to the Android file system, then respond with
     * the file location using the provided Messenger.
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public static void downloadAndRespond(Context context,
                                          Uri uri,
                                          Messenger messenger) throws IOException {
    	sendStringMessage(DownloadUtils.downloadFile(context,
                                            uri),
                 messenger, 0);
    }
    
    /**
     * Download a file to the Android file system, then respond with
     * the file location using the provided Messenger.
     * @throws IOException 
     */
    public static void getStringAndRespond(Context context,
                                          Uri uri,
                                          Messenger messenger, int what) throws IOException {
    	sendStringMessage(DownloadUtils.getJSONString(uri),
                 messenger, what);
    }
    
    
    /**
     * Download the file located at the provided internet url using
     * the URL class, store it on the android file system using
     * openFileOutput(), and return the path to the file on disk.
     *
     * @param context	the context in which to write the file
     * @param uri       the web url
     * 
     * @return          the path to the downloaded file on the file system
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public static String downloadFile (Context context,
                                       Uri uri) throws MalformedURLException, IOException {

    		String uriStr = uri.toString();
    		final String resultFilename = uriStr.substring( uriStr.lastIndexOf('/')+1, uriStr.length() );
    		final String filePath = context.getFilesDir().toString() + File.separator + resultFilename;
    		File outFile = new File(filePath);
            // download again only if the file doesn't exist
            if (!outFile.exists()) {
                Log.d(TAG, "    downloading to " + outFile);	
                // Download the contents at the URL, which should
                // reference an image.
                final InputStream in = (InputStream)
                    new URL(uri.toString()).getContent();
                final OutputStream os =
                    new FileOutputStream(outFile);	
                // Copy the contents of the downloaded image to the file.
                copy(in, os);
                in.close();
                os.close();

            } else {
            	Log.d(TAG, outFile + " already exists");
            }
            return filePath;
    }
        

    /**
     * Copy the contents of an InputStream into an OutputStream.
     * 
     * @param in
     * @param out
     * @return
     * @throws IOException
     */
    static public int copy(final InputStream in,
                           final OutputStream out) throws IOException {
        final int BUFFER_LENGTH = 1024;
        final byte[] buffer = new byte[BUFFER_LENGTH];
        int totalRead = 0;
        int read = 0;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            totalRead += read;			
        }

        return totalRead;
    }
    
    public static String getJSONString(Uri uri) throws IOException{
    	String url = uri.toString();
    	Log.d(DownloadUtils.class.toString(), "fetching JSON from " + url);
    	StringBuilder builder = new StringBuilder();
    	HttpClient client = new EasyHttpClient();
    	HttpGet httpGet = new HttpGet(url);
    	try{
    		HttpResponse response = client.execute(httpGet);
    		StatusLine statusLine = response.getStatusLine();
    		int statusCode = statusLine.getStatusCode();
    		if(statusCode == 200){
    			HttpEntity entity = response.getEntity();
    			InputStream content = entity.getContent();
    			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
    			String line;
    			while((line = reader.readLine()) != null){
    				builder.append(line);
    			}
    		} else {
    			Log.e(DownloadUtils.class.toString(),"Failed JSON download");
    		}
    	}catch(ClientProtocolException e){
    		e.printStackTrace();
    	} 
    	Log.d(DownloadUtils.class.toString(), "string received: " + builder.toString());
    	return builder.toString();
    }
    
    public static JSONObject getJSONData(String jsonString) {
    	Log.d(DownloadUtils.class.toString(), "decoding string");
    	JSONObject data = null;
    	try {
			data = new JSONObject(jsonString);
		} catch (JSONException e) {
			Log.e(DownloadUtils.class.toString(),"Failed JSON decoding, probably broken string");
			e.printStackTrace();
		}
		return data;
    }
}
