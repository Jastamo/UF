package se.nextapp.task.full.item;

import android.content.Intent;
import android.content.res.Resources;

public class ContentItem {

	protected Intent mIntent;
	protected int mIconId;
	protected CharSequence title;
	// protected CharSequence mCondText;
	protected int id;
	protected int parentId;
	protected String type;
	protected boolean prioritized = false;

	protected long timestampCreated = -1;
	protected boolean mEnabled = true;

	public int getIconId() {
		return mIconId;
	}

	public Intent getIntent() {
		return mIntent;
	}

	public int getId() {
		return id;
	}

	public CharSequence getTitle() {
		return title;
	}

	public int getParentId() {
		return parentId;
	}

	public long getTimestampCreated() {
		return timestampCreated;
	}

	public String getType() {
		return type;
	}

	public boolean isPrioritized() {
		return prioritized;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public ContentItem setEnabled(boolean enabled) {
		mEnabled = enabled;
		return this;
	}

	public ContentItem setIcon(int iconRes) {
		mIconId = iconRes;
		return this;
	}

	public ContentItem setIntent(Intent intent) {
		mIntent = intent;
		return this;
	}

	public ContentItem setTitle(CharSequence title) {
		this.title = title;
		return this;
	}

	public ContentItem setTitle(int title, Resources resc) {
		this.title = resc.getString(title);
		return this;
	}

	public ContentItem isPrioritized(boolean b) {
		prioritized = b;
		return this;
	}

	public ContentItem setId(int id) {
		this.id = id;
		return this;
	}

	public ContentItem setParentId(int i) {
		parentId = i;
		return this;
	}

	public ContentItem setTimestampCreated(int i) {
		timestampCreated = i;
		return this;
	}

	public ContentItem setType(String type) {
		this.type = type;
		return this;
	}
}
