package com.espian.flyin.library;

import android.content.Intent;
import android.content.res.Resources;

public class FlyInMenuItem {

	private Intent mIntent;
	private int mIconId;
	private CharSequence mText;
	private CharSequence mCondText;
	private int id;
	private String contentType;
	private long timestampCreated = -1;
	private boolean isOpen = false;

	private boolean mEnabled;

	public int getIconId() {
		return mIconId;
	}

	public Intent getIntent() {
		return mIntent;
	}

	public int getId() {
		return id;
	}

	public String getType() {
		return contentType;
	}

	public CharSequence getTitle() {
		return mText;
	}

	public CharSequence getTitleCondensed() {
		return mCondText;
	}

	public long getTimestampCreated() {
		return timestampCreated;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public FlyInMenuItem setEnabled(boolean enabled) {
		mEnabled = enabled;
		return this;
	}

	public FlyInMenuItem setIcon(int iconRes) {
		mIconId = iconRes;
		return this;
	}

	public FlyInMenuItem setIntent(Intent intent) {
		mIntent = intent;
		return this;
	}

	public FlyInMenuItem setTitle(CharSequence title) {
		mText = title;
		return this;
	}

	public FlyInMenuItem setTitle(int title, Resources resc) {
		mText = resc.getString(title);
		return this;
	}

	public FlyInMenuItem setType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public FlyInMenuItem setTitleCondensed(CharSequence title) {
		mCondText = title;
		return this;
	}

	public FlyInMenuItem setId(int id) {
		this.id = id;
		return this;
	}

	public FlyInMenuItem setTimestampCreated(int i) {
		timestampCreated = i;
		return this;
	}

	public FlyInMenuItem isOpen(boolean b) {
		isOpen = b;
		return this;
	}
}
