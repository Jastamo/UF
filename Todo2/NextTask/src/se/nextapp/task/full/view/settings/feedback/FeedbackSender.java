package se.nextapp.task.full.view.settings.feedback;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import se.nextapp.task.full.internet.JSONParser;
import se.nextapp.task.full.internet.Tags;
import se.nextapp.task.full.internet.URL;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;


public class FeedbackSender extends AsyncTask<Void, Void, Void> {

	private JSONParser parser;
	private JSONObject result;
	private Context context;
	private FeedbackView feedbackView;
	private String msg, mail = null;

	private boolean success = false;

	public FeedbackSender(Context context, FeedbackView feedbackView, String msg) {
		this.context = context;
		this.feedbackView = feedbackView;
		this.msg = msg;
		parser = new JSONParser();

		execute();
	}
	
	public FeedbackSender(Context context, FeedbackView feedbackView, String msg, String mail) {
		this.context = context;
		this.feedbackView = feedbackView;
		this.msg = msg;
		this.mail = mail;
		parser = new JSONParser();
		
		execute();
	}

	protected void onPreExecute() {
		super.onPreExecute();
	}

	protected Void doInBackground(Void... nothing) {
		try {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair(Tags.MESSAGE, msg));
			params.add(new BasicNameValuePair(Tags.API_LEVEL, Build.VERSION.SDK_INT + ""));
			try {
				params.add(new BasicNameValuePair(Tags.APP_VERSION, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName));
			} catch (NameNotFoundException e) {
			}
			params.add(new BasicNameValuePair(Tags.DEVICE, Build.MODEL));
			if(mail != null) params.add(new BasicNameValuePair(Tags.MAIL, mail));
			
			Log.i("Sending feedback", msg);
			
			result = parser.makeHttpRequest(URL.FEEDBACK_URL, "POST", params);
			Log.i("result json", result.toString());
			success = result.has(Tags.SUCCESS) && result.getInt(Tags.SUCCESS) == 1;
			
			Log.i("Result from feedback: ", result.getString(Tags.MESSAGE));
		} catch (JSONException e) {
			success = false;
		}
		return null;
	}

	protected void onPostExecute(Void result) {
		super.onPostExecute(result);

		feedbackView.feedbackSent(success);
	}
}
