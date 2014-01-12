package com.todo.code3.xml.multi_select_parent;

import java.util.ArrayList;

import android.view.View;
import android.widget.Button;

import com.todo.code3.MainActivity;
import com.todo.code3.dialog.date_and_time.TimeDialog;
import com.todo.code3.misc.Reminder;

public class SelectDaysInWeek extends MultiSelectParent {

	private Button timeButton;
	private int hour = -1, minute = -1;

	private MainActivity activity;

	public SelectDaysInWeek(MainActivity activity) {
		super(activity);
		this.activity = activity;
	}

	public SelectDaysInWeek(MainActivity activity, int oldHour, int oldMinute) {
		super(activity);
		this.activity = activity;
		this.hour = oldHour;
		this.minute = oldMinute;
	}

	protected void init() {
		super.init();
	}

	public void generate() {
		super.generate();

		timeButton = new Button(getContext());
		timeButton.setText("Set weekly reminder time");
		timeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new TimeDialog(activity) {
					public void onResult(int hour, int minute) {
						if (hour != -1) SelectDaysInWeek.this.hour = hour;
						if (minute != -1) SelectDaysInWeek.this.minute = minute;

						if (hour != -1 && minute != -1) changed();
					}
				};
			}
		});
		addView(timeButton);
	}

	public void update(String reminderInfo) {
		super.update(reminderInfo);

		if (Reminder.getType(reminderInfo).equals(type)) {
			if (Reminder.hasPart(reminderInfo, 2)) {
				try {
					hour = Integer.parseInt(Reminder.getPart(reminderInfo, 2));
				} catch (NumberFormatException e) {

				}
			}
			if (Reminder.hasPart(reminderInfo, 3)) {
				try {
					minute = Integer.parseInt(Reminder.getPart(reminderInfo, 3));
				} catch (NumberFormatException e) {

				}
			}
		} else {
			hour = -1;
			minute = -1;
		}

		if (timeButton != null) timeButton.setText(hour + " : " + minute);
	}

	public void onChanged(String type, ArrayList<Integer> selected) {
		if (hour == -1) hour = 12;
		if (minute == -1) minute = 0;
		onChanged(type, selected, hour, minute);
	}

	public void onChanged(String type, ArrayList<Integer> selected, int hour, int minute) {

	}
}
