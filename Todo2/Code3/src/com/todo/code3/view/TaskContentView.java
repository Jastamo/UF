package com.todo.code3.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.todo.code3.MainActivity;
import com.todo.code3.R;
import com.todo.code3.misc.App;

public class TaskContentView extends ContentView {

	private TextView descTV;
	private EditText descET, focusDummy;
	private Button saveButton;

	private int id;

	public TaskContentView(MainActivity activity, int id) {
		super(activity);
		this.id = id;
	}

	protected void init() {
		LayoutInflater.from(activity).inflate(R.layout.task_content_view, this, true);
		
		setLayoutParams(new LayoutParams(activity.getContentWidth(), LayoutParams.FILL_PARENT));

		descTV = (TextView) findViewById(R.id.descTV);
		descET = (EditText) findViewById(R.id.descET);
		focusDummy = (EditText) findViewById(R.id.focusDummy);
		saveButton = (Button) findViewById(R.id.saveButton);

		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				descTV.setVisibility(View.VISIBLE);
				descET.setVisibility(View.GONE);
				saveButton.setVisibility(View.GONE);

				focusDummy.requestFocus();

				saveDescription(descET.getText().toString());
				descTV.setText(descET.getText().toString());
			}
		});

		descTV.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				descTV.setVisibility(View.GONE);
				descET.setVisibility(View.VISIBLE);
				saveButton.setVisibility(View.VISIBLE);

				descET.setText(descTV.getText());

				descET.requestFocus();
			}
		});
		descET.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				// This has not been tested on a real device
				if (hasFocus) App.showKeyboard(getContext());
				else App.hideKeyboard(getContext(), focusDummy);
			}
		});

		focusDummy.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) App.hideKeyboard(getContext(), focusDummy);
			}
		});
	}

	public void update(JSONObject data) {
		try {
			JSONObject task = new JSONObject(data.getString(id + ""));
			if (task.getString(App.TYPE).equals(App.TASK)) {
				if (task.has(App.DESCRIPTION)) {
					descTV.setText(task.getString(App.DESCRIPTION));
				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void leave() {
		focusDummy.requestFocus();
		App.hideKeyboard(getContext(), focusDummy);
	}

	private void saveDescription(String desc) {
		App.hideKeyboard(getContext(), focusDummy);

		activity.setTaskDescription(desc, id);
	}

	// not used
	public void updateContentItemsOrder() {
	}

	public int getParentId() {
		return id;
	}
}