package com.todoapp.code;

import misc.Data;
import misc.SharedPreferencesEditor;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private LinearLayout folderList;
	private Button addFolder;

	private SharedPreferences prefs;
	private SharedPreferencesEditor editor;

	private JSONObject data;

	private int numberOfFolders = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initXMLElements();

		prefs = getSharedPreferences(Data.PREFERENCES_NAME, Context.MODE_PRIVATE);
		editor = new SharedPreferencesEditor(prefs);

		getData();

	}

	private void getData() {
		String taskData = prefs.getString(Data.TASK_DATA, null);

		// Change data to taskData
		// when data is compared, all the earlier data is cleared every time the
		// Main Activity is created
		if (taskData == null) {
			// If no data is received, the number of folders is set to 0
			try {
				data = new JSONObject();
				data.put(Data.NUMBER_OF_FOLDERS, 0);
				editor.put(Data.TASK_DATA, data.toString());
			} catch (JSONException e) {
			}
		} else {
			try {
				data = new JSONObject(taskData);
			} catch (JSONException e) {
			}
		}

		updateData();
	}

	private void initXMLElements() {
		folderList = (LinearLayout) findViewById(R.id.folderList);

		addFolder = (Button) findViewById(R.id.addFolder);
		addFolder.setOnClickListener(this);
	}

	private void updateData() {
		Log.i("dsafsafas", data.toString());

		updateFolderButtons();
	}

	private void updateFolderButtons() {
		
		folderList.removeAllViews();

		try {
			numberOfFolders = Integer.parseInt(data.getString(Data.NUMBER_OF_FOLDERS));

			for (int i = 0; i < numberOfFolders; i++) {

				if (data.has(Data.FOLDER + i)) {
					final JSONObject folderData = new JSONObject(data.getString(Data.FOLDER + i));

					LinearLayout l = new LinearLayout(this);
					TextView t = new TextView(this);
					Button b = new Button(this);

					l.setWeightSum(1);

					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					p.weight = 0.8f;

					LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					pp.weight = 0.2f;

					l.addView(t, p);
					l.addView(b, pp);

					t.setText(folderData.getString(Data.FOLDER_NAME));

					t.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Intent i = new Intent(MainActivity.this, FolderActivity.class);
							i.putExtra(Data.FOLDER_DATA, folderData.toString());
							startActivity(i);
						}
					});

					b.setText("Remove");

					b.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
								removeFolder(folderData.getInt(Data.FOLDER_ID));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					});

					folderList.addView(l);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (folderList.getChildCount() == 0) {
			TextView tv = new TextView(this);
			tv.setText("You do not have any folders! Tap the button above to add a new folder.");
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			tv.setLayoutParams(p);

			folderList.addView(tv);
		}
	}

	public void onClick(View v) {
		if (v.getId() == R.id.addFolder) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Add a new folder");
			alert.setMessage("Name the folder!");

			// Set edit text view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);
			alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String name = input.getText().toString();
					addFolder(name);
				}
			});

			alert.setNegativeButton("Cancel", null);

			alert.show();
		}
	}

	private void addFolder(String name) {
		try {
			JSONObject folderData = new JSONObject();
			folderData.put(Data.FOLDER_NAME, name);
			folderData.put(Data.NUMBER_OF_TASKS, 0);
			folderData.put(Data.FOLDER_ID, numberOfFolders);

			data.put(Data.NUMBER_OF_FOLDERS, numberOfFolders + 1);
			data.put(Data.FOLDER + numberOfFolders, folderData.toString());
			editor.put(Data.TASK_DATA, data.toString());

			updateData();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void removeFolder(final int id) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Removing " + new JSONObject(data.getString(Data.FOLDER + id)).getString(Data.FOLDER_NAME));
			builder.setMessage("This action cannot be undone. Are you sure?");
			
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					data.remove(Data.FOLDER + id);

					editor.put(Data.TASK_DATA, data.toString());

					updateData();
				}
			});
			
			builder.setNegativeButton("Cancel", null);
			builder.show();			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	protected void onRestart() {
		super.onRestart();
		getData();
	}
}
