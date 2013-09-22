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
import android.opengl.Visibility;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FolderActivity extends Activity implements OnClickListener {

	private TextView folderNameET;
	private Button addTask;
	private Button saveFolderName;
	private LinearLayout taskList;

	private JSONObject data;
	private String folderName = "";

	private SharedPreferences prefs;
	private SharedPreferencesEditor editor;

	private int numberOfTasks = 0;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String folderData = intent.getExtras().getString(Data.FOLDER_DATA, null);

		if (folderData == null) {
			onBackPressed();
			finish();
		} else try {
			data = new JSONObject(folderData);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		prefs = getSharedPreferences(Data.PREFERENCES_NAME, Context.MODE_PRIVATE);
		editor = new SharedPreferencesEditor(prefs);

		setContentView(R.layout.activity_folder);

		initXMLElements();

		updateData();

	}

	private void getData() {
		String folderData = prefs.getString(Data.TASK_DATA, null);

		try {
			JSONObject o = new JSONObject(folderData);
			data = new JSONObject(o.getString(Data.FOLDER + data.getString(Data.FOLDER_ID)));
			updateData();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void initXMLElements() {
		folderNameET = (EditText) findViewById(R.id.folderName);
		folderNameET.setOnClickListener(this);

		taskList = (LinearLayout) findViewById(R.id.taskList);

		addTask = (Button) findViewById(R.id.addTask);
		addTask.setOnClickListener(this);

		saveFolderName = (Button) findViewById(R.id.saveFolderName);
		saveFolderName.setOnClickListener(this);
	}

	private void updateData() {
		try {
			folderName = data.getString(Data.FOLDER_NAME);
			folderNameET.setText(folderName);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		updateTaskButtons();
	}

	private void updateTaskButtons() {
		taskList.removeAllViews();
		
		try {
			numberOfTasks = Integer.parseInt(data.getString(Data.NUMBER_OF_TASKS));

			for (int i = 0; i < numberOfTasks; i++) {
				final JSONObject taskData = new JSONObject(data.getString(Data.TASK + i));

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

				t.setText(taskData.getString(Data.TASK_NAME));

				t.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						try {
							Intent i = new Intent(FolderActivity.this, TaskActivity.class);
							i.putExtra(Data.TASK_DATA, taskData.toString());
							i.putExtra(Data.FOLDER_ID, Integer.parseInt(data.getString(Data.FOLDER_ID)));
							startActivity(i);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});

				b.setText("Done");

				b.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						try {
							setTaskCompleted(taskData.getInt(Data.TASK_ID));
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
				
				if (taskData.has(Data.TASK_COMPLETED)) {
					if (taskData.getBoolean(Data.TASK_COMPLETED)) {
						b.setText("Completed!");
						b.setEnabled(false);
						b.setOnClickListener(null);
					}
				}

				taskList.addView(l);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (taskList.getChildCount() == 0) {
			TextView tv = new TextView(this);
			tv.setText("You do not have any folders! Tap the button above to add a new folder.");
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			tv.setLayoutParams(p);

			taskList.addView(tv);
		}
	}

	public void onClick(View v) {
		if (v.getId() == R.id.addTask) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Add a new task");
			alert.setMessage("What do you have to do?");

			// Set edit text view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);
			alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String name = input.getText().toString();
					addTask(name);
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Does nothing
				}
			});

			alert.show();
		}

		if (v.getId() == R.id.folderName) {
			folderNameET.setGravity(Gravity.LEFT);
			saveFolderName.setVisibility(View.VISIBLE);
			saveFolderName.setEnabled(true);
		}

		if (v.getId() == R.id.saveFolderName) {
			if (saveFolderName.isEnabled()) {
				folderNameET.setGravity(Gravity.CENTER);
				saveFolderName.setVisibility(View.INVISIBLE);
				saveFolderName.setEnabled(false);

				updateFolderName();
			}
		}
	}

	private void updateFolderName() {
		try {
			String s = folderNameET.getText().toString();
			if (s == null) return;
			data.put(Data.FOLDER_NAME, s);

			String ss = prefs.getString(Data.TASK_DATA, null);

			JSONObject o = new JSONObject(ss);

			o.put(Data.FOLDER + data.getString(Data.FOLDER_ID), data.toString());
			editor.put(Data.TASK_DATA, o.toString());

			updateData();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void addTask(String name) {
		try {
			JSONObject taskData = new JSONObject();
			taskData.put(Data.TASK_NAME, name);
			taskData.put(Data.TASK_ID, numberOfTasks);

			data.put(Data.NUMBER_OF_TASKS, numberOfTasks + 1);
			data.put(Data.TASK + numberOfTasks, taskData.toString());

			String s = prefs.getString(Data.TASK_DATA, null);

			// This is the json object containing all the data
			JSONObject o = new JSONObject(s);

			o.put(Data.FOLDER + data.getString(Data.FOLDER_ID), data.toString());
			editor.put(Data.TASK_DATA, o.toString());

			updateData();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void setTaskCompleted(int id) {
		try {
			JSONObject taskData = new JSONObject(data.getString(Data.TASK + id));
			taskData.put(Data.TASK_COMPLETED, true);

			data.put(Data.TASK + id, taskData.toString());

			String s = prefs.getString(Data.TASK_DATA, null);

			JSONObject o = new JSONObject(s);
			o.put(Data.FOLDER + data.getString(Data.FOLDER_ID), data.toString());

			editor.put(Data.TASK_DATA, o.toString());

			updateData();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void onRestart() {
		super.onRestart();
		getData();
	}
}
