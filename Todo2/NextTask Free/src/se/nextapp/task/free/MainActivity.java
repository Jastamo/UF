package se.nextapp.task.free;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import se.nextapp.task.free.dialog.AddItemDialog;
import se.nextapp.task.free.dialog.TextLineDialog;
import se.nextapp.task.free.misc.App;
import se.nextapp.task.free.misc.Reminder;
import se.nextapp.task.free.misc.SPEditor;
import se.nextapp.task.free.misc.Sort;
import se.nextapp.task.free.notification.NotificationReceiver;
import se.nextapp.task.free.view.ContentView;
import se.nextapp.task.free.view.DownloadFullVersionView;
import se.nextapp.task.free.view.ItemView;
import se.nextapp.task.free.view.NoteView;
import se.nextapp.task.free.view.TaskView;
import se.nextapp.task.free.view.settings.SelectLanguage;
import se.nextapp.task.free.view.settings.SettingsView;
import se.nextapp.task.free.view.settings.feedback.FeedbackView;
import se.nextapp.task.free.xml.OptionsBar;
import se.nextapp.task.free.xml.Wrapper;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Scroller;
import android.widget.TextView;

import com.espian.flyin.library.FlyInFragmentActivity;
import com.espian.flyin.library.FlyInMenu;
import com.espian.flyin.library.FlyInMenuItem;

public class MainActivity extends FlyInFragmentActivity {

	private SharedPreferences prefs;
	private SPEditor editor;

	private LinearLayout wrapper;
	private TextView nameTV;
	private EditText focusDummy, nameET;
	private OptionsBar options;
	private FrameLayout dragButton, backButton;
	private LinearLayout titleBar;

	private JSONObject data = new JSONObject();

	private int openObjectId = -1;

	public ArrayList<ContentView> contentViews;

	// For scrolling between a checklist and its tasks
	private Scroller scroller;
	private Runnable scrollRunnable;
	private Handler scrollHandler;
	private boolean isMoving = false;
	private int currentContentOffset = 0;
	private int posInWrapper = 0;
	private long scrollFps = 1000 / 60;

	private int width, height;

	private boolean freeVersion = true;
	private boolean canRun = true;

	@SuppressLint("InlinedApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.wrapper);

		scroller = new Scroller(this, AnimationUtils.loadInterpolator(this, android.R.anim.decelerate_interpolator));
		scrollRunnable = new AnimationRunnable();
		scrollHandler = new Handler();

		DisplayMetrics dm = getResources().getDisplayMetrics();
		width = dm.widthPixels;
		height = dm.heightPixels - App.getStatusBarHeight(getResources());

		// Recalculates the size of the content (and offsets it) if the screen
		// is wide enough to show the master view (the menu is visible at all
		// times)
		if (isInMasterView()) {
			FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) ((Wrapper) findViewById(R.id.bigWrapper)).getLayoutParams();
			p.width = getContentWidth();
			p.setMargins(getMenuWidth(), 0, 0, 0);
			((Wrapper) findViewById(R.id.bigWrapper)).setLayoutParams(p);
			((Wrapper) findViewById(R.id.bigWrapper)).requestLayout();
		}

		prefs = getSharedPreferences(App.PREFERENCES_NAME, Context.MODE_PRIVATE);
		editor = new SPEditor(prefs);

		contentViews = new ArrayList<ContentView>();

		// Setting correct language (locale)
		Configuration c = getBaseContext().getResources().getConfiguration();
		Locale.setDefault(new Locale(getLocaleString()));
		c.locale = new Locale(getLocaleString());
		getBaseContext().getResources().updateConfiguration(c, getResources().getDisplayMetrics());

		initXML();
		initBars();
		loadFlyInMenu(getMenuWidth());

		canRun = true;
		// Sets the time created (for checking that the free version will be
		// locked)
		if (prefs.getLong(App.TIME_CREATED, -1) == -1) {
			editor.put(App.TIME_CREATED, System.currentTimeMillis() / 1000);
			getDataFromSharedPreferences();
		} else if (prefs.getLong(App.TIME_CREATED, -1) < System.currentTimeMillis() / 1000 - 3600 * 24 * 4 && freeVersion) {
			canRun = false;
			setTitle("NextTask");

			posInWrapper = 0;
			contentViews.clear();

			scroller.startScroll(currentContentOffset, 0, -currentContentOffset, 0, 0);
			scrollHandler.postDelayed(scrollRunnable, scrollFps);

			contentViews.add(posInWrapper, new DownloadFullVersionView(this));

			updateData();
		} else {
			getDataFromSharedPreferences();
		}

		if (getIntent().hasExtra(App.OPEN)) {
			if (getIntent().getIntExtra(App.OPEN, -1) == App.SETTINGS) {
				openSettings();
				if (getIntent().hasExtra(App.TYPE) && getIntent().getStringExtra(App.TYPE).equals(App.SETTINGS_APP_LANGUAGE)) openSettingsItem(SettingsView.SELECT_APP_LANGUAGE, getResources().getString(R.string.set_language), false);
			} else openFromIntent(getIntent().getIntExtra(App.OPEN, -1));
		}

		// Set theme
		if (Build.VERSION.SDK_INT >= 11) {
			if (!isDarkTheme()) setTheme(android.R.style.Theme_Holo_Light);
			else setTheme(android.R.style.Theme_Holo_Panel);
		} else {
			if (!isDarkTheme()) setTheme(android.R.style.Theme_Light);
			else setTheme(android.R.style.Theme_Black);
		}
	}

	private void getDataFromSharedPreferences() {
		try {
			String d = prefs.getString(App.DATA, null);

			if (d == null) {
				data = new JSONObject();
				data.put(App.NUM_IDS, 0);

				addMenuItem("Inbox", App.FOLDER);
				openMenuItem(0);

				if (freeVersion) {
					add("After that, you will have to buy the full version", App.NOTE);
					add("You have a 4 day demo period", App.NOTE);
				}
				add("Check out the settings in the menu", App.TASK);
				add("Tap a task to set due dates and reminders", App.TASK);
				add("Tap the title to edit it", App.TASK);
				add("Enable options by holding down items", App.TASK);
				add("Swipe right to see the menu", App.TASK);
				add("Click the + to add tasks", App.TASK);
			} else {
				data = new JSONObject(d);
			}

			boolean hasOpened = false;

			String[] childrenIds;
			if (data.has(App.CHILDREN_IDS)) childrenIds = data.getString(App.CHILDREN_IDS).split(",");
			else childrenIds = new String[0];

			for (int i = 0; i < childrenIds.length; i++) {
				if (data.has(childrenIds[i])) {
					JSONObject o = new JSONObject(data.getString(childrenIds[i]));
					if (o.getString(App.NAME).equals("Inbox")) {
						openMenuItem(Integer.parseInt(childrenIds[i]));
						hasOpened = true;
						break;

					}
				}
			}

			// Opens the folder at the top of the menu
			// The order is reversed since the order of the menu is reversed
			if (!hasOpened) {
				for (int i = childrenIds.length - 1; i >= 0; i--) {
					if (data.has(childrenIds[i])) {
						JSONObject o = new JSONObject(data.getString(childrenIds[i]));
						if (o.getString(App.TYPE).equals(App.FOLDER)) {
							openMenuItem(Integer.parseInt(childrenIds[i]));
							break;
						}
					}
				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		Log.i("asdasd", data.toString());

		updateData();
	}

	private void initXML() {
		// Gives the wrapper (which makes swiping the menu open) possible
		((Wrapper) findViewById(R.id.bigWrapper)).setActivity(this);

		wrapper = (LinearLayout) findViewById(R.id.wrapper);

		titleBar = (LinearLayout) findViewById(R.id.titleBar);

		nameTV = (TextView) findViewById(R.id.name);
		nameET = (EditText) findViewById(R.id.nameET);
		focusDummy = (EditText) findViewById(R.id.focusDummy);

		dragButton = (FrameLayout) findViewById(R.id.dragButton);
		backButton = (FrameLayout) findViewById(R.id.backButton);

		options = (OptionsBar) findViewById(R.id.optionsBar);
		options.setMainActivity(this);

		// This is the view that encapsulates the title
		((LinearLayout) findViewById(R.id.nameTouchArea)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startEditTitle();
			}
		});

		nameET.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) App.showKeyboard(MainActivity.this);
				else {
					App.hideKeyboard(MainActivity.this, focusDummy);
					endEditTitle(true);
				}
			}
		});

		focusDummy.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) App.hideKeyboard(MainActivity.this, focusDummy);
			}
		});

		initMenuButtons();
		setColors();
	}

	private void initMenuButtons() {
		// Makes the custom view
		LinearLayout customView = new LinearLayout(this);
		customView.setOrientation(LinearLayout.VERTICAL);

		TextView addFolderButton = new TextView(this);
		addFolderButton.setText("  " + getResources().getString(R.string.add_new_folder));
		addFolderButton.setTextSize(App.pxToDp(getResources().getDimension(R.dimen.item_text_size), getResources()));
		addFolderButton.setTextColor(getResources().getColor(R.color.item_text_color));

		LinearLayout lll = new LinearLayout(this);
		lll.setOrientation(LinearLayout.HORIZONTAL);
		lll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int) getResources().getDimension(R.dimen.item_height)));
		lll.setGravity(Gravity.CENTER);
		ImageView ii = new ImageView(this);
		ii.setLayoutParams(new LinearLayout.LayoutParams((int) (getResources().getDimension(R.dimen.item_height) / 2), (int) (getResources().getDimension(R.dimen.item_height) / 2)));
		ii.setBackgroundResource(R.drawable.ic_plus_small);
		ii.getBackground().setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.item_text_color), PorterDuff.Mode.MULTIPLY));
		lll.addView(ii);
		lll.addView(addFolderButton);
		lll.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_selector));
		lll.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				TextLineDialog b = new TextLineDialog(MainActivity.this, getResources().getString(R.string.add_new_folder), null, true, getResources().getString(R.string.add), getResources().getString(R.string.cancel)) {
					protected void onResult(Object result) {
						super.onResult(result);

						if (result instanceof String) addMenuItem((String) result, App.FOLDER);
					}
				};

				b.show();
			}
		});

		TextView settingsButton = new TextView(this);
		settingsButton.setText("  " + getResources().getString(R.string.settings));
		settingsButton.setTextSize(App.pxToDp(getResources().getDimension(R.dimen.item_text_size), getResources()));
		settingsButton.setTextColor(getResources().getColor(R.color.item_text_color));

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int) getResources().getDimension(R.dimen.item_height)));
		ll.setGravity(Gravity.CENTER);
		ImageView i = new ImageView(this);
		i.setBackgroundResource(R.drawable.ic_settings);
		i.getBackground().setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.item_text_color), PorterDuff.Mode.MULTIPLY));
		i.setLayoutParams(new LinearLayout.LayoutParams((int) (getResources().getDimension(R.dimen.item_height) / 2), (int) (getResources().getDimension(R.dimen.item_height) / 2)));
		ll.addView(i);
		ll.addView(settingsButton);
		ll.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_selector));
		ll.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openSettings();
			}
		});

		LinearLayout l = new LinearLayout(this);
		l.setOrientation(LinearLayout.VERTICAL);

		l.addView(lll);
		l.addView(ll);

		customView.addView(l);
		getFlyInMenu().setCustomView(customView);
		getFlyInMenu().getCustomView().setVisibility(View.VISIBLE);
	}

	private void initBars() {
		int barHeight = getBarHeight();
		int borderHeight = getBarBorderHeight();

		// FrameLayout[] buttons = { dragButton, backButton, (FrameLayout)
		// findViewById(R.id.addButton) };
		//
		// for (int i = 0; i < buttons.length; i++) {
		// buttons[i].getLayoutParams().height = barHeight;
		// buttons[i].getLayoutParams().width = barHeight;
		// }

		((LinearLayout) findViewById(R.id.barBorder)).getLayoutParams().height = borderHeight;
		((LinearLayout) findViewById(R.id.titleBar)).getLayoutParams().height = barHeight;

		options.getLayoutParams().height = barHeight;
	}

	private void updateData() {
		// Log.i("Updating data...", data.toString());

		// removes the view that are not next
		// to the right of the view the user sees
		for (int i = 0; i < contentViews.size(); i++) {
			if (i > posInWrapper + 1) contentViews.remove(i);
		}

		wrapper.removeAllViews();
		for (ContentView view : contentViews) {
			wrapper.addView(view);
			view.update(data);
		}

		updateMenu();

		try {
			data.put(App.TIMESTAMP_LAST_UPDATED, (int) (System.currentTimeMillis() / 1000));
		} catch (JSONException e) {
		}
	}

	private void updateMenu() {
		FlyInMenu menu = getFlyInMenu();

		menu.clearMenuItems();

		try {
			String[] childrenIds;

			if (data.has(App.CHILDREN_IDS)) childrenIds = data.getString(App.CHILDREN_IDS).split(",");
			else childrenIds = new String[0];

			for (int i = childrenIds.length - 1; i >= 0; i--) {
				String id = childrenIds[i];
				if (data.has(id)) {
					JSONObject folder = new JSONObject(data.getString(id));
					if (folder.getString(App.TYPE).equals(App.FOLDER)) {
						// If the folder does not have a parent
						// it should be in the menu.
						if (folder.getString(App.TYPE).equals(App.FOLDER)) {
							if (folder.has(App.PARENT_ID) && folder.getInt(App.PARENT_ID) != -1) continue;
						}

						FlyInMenuItem mi = new FlyInMenuItem();
						// int numChildren =
						// (folder.getString(App.CHILDREN_IDS).length() == 0) ?
						// 0 :
						// folder.getString(App.CHILDREN_IDS).split(",").length;
						mi.setTitle(folder.getString(App.NAME));
						mi.setId(folder.getInt(App.ID));
						mi.setType(folder.getString(App.TYPE));
						if (!isInSettings()) mi.isOpen(App.isParentOf(openObjectId, folder.getInt(App.ID), data));
						menu.addMenuItem(mi);
					}
				}
			}
		} catch (JSONException e) {
		}

		menu.setMenuItems();
	}

	public boolean onFlyInItemClick(FlyInMenuItem item, int position) {
		try {
			JSONObject object = new JSONObject(data.getString(item.getId() + ""));

			if (object.getString(App.TYPE).equals(App.FOLDER)) openMenuItem(object.getInt(App.ID));

		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void toggleMenu() {
		if (!canRun()) return;
		if (!isMoving) getFlyInMenu().toggleMenu();
	}

	public void showMenu() {
		if (!canRun()) return;
		if (!isMoving) getFlyInMenu().showMenu();

		hideOptions();
		if (isEditingTitle()) endEditTitle(false);
		App.hideKeyboard(this, focusDummy);
	}

	public void hideMenu() {
		if (!canRun()) return;
		if (!isMoving) getFlyInMenu().hideMenu();
	}

	// When clicking on the button
	public void addDialog(View v) {
		if (!canRun()) return;
		if (isInSettings()) return;
		AddItemDialog i = new AddItemDialog(this, getResources().getString(R.string.add_new), null, null, getResources().getString(R.string.cancel)) {
			public void onResult(String name, String type) {
				super.onResult(name, type);
				add(name, type);
			}
		};

		i.show();
	}

	// When dragging to an item
	public void addDialog(final String type) {
		if (!canRun()) return;
		String t = "";
		if (type.equals(App.TASK)) t = getResources().getString(R.string.task);
		else if (type.equals(App.NOTE)) t = getResources().getString(R.string.note);
		else if (type.equals(App.FOLDER)) t = getResources().getString(R.string.folder);

		TextLineDialog i = new TextLineDialog(this, getResources().getString(R.string.add_new) + " " + t, null, true, getResources().getString(R.string.add), getResources().getString(R.string.cancel)) {
			public void onResult(Object result) {
				super.onResult(result);

				if (result instanceof String) add((String) result, type);
			}
		};

		i.show();
	}

	private void add(String name, String type) {
		int parent = -1;
		try {
			JSONObject o = new JSONObject(data.getString(openObjectId + ""));
			while (o.getString(App.TYPE).equals(App.TASK)) {
				if (!data.has(o.getString(App.PARENT_ID))) break;

				o = new JSONObject(data.getString(o.getString(App.PARENT_ID)));
			}

			parent = o.getInt(App.ID);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (parent == -1) return;
		data = App.add(name, type, parent, data);
		editor.put(App.DATA, data.toString());

		try {
			int id = data.getInt(App.NUM_IDS) - 1;

			if (contentViews.get(posInWrapper) instanceof ItemView) {
				((ItemView) contentViews.get(posInWrapper)).setExpandingItemId(id);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		updateData();
	}

	public void addMenuItem(String name, String type) {
		data = App.add(name, type, -1, data);
		editor.put(App.DATA, data.toString());

		try {
			// This gets the id of the newly added folder
			getFlyInMenu().setExpandingItemId(data.getInt(App.NUM_IDS) - 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		updateMenu();
	}

	public void remove(int id) {
		remove(id, true);

		cancelNotification(id);
	}

	public void remove(int id, boolean update) {
		data = App.remove(id, data);
		editor.put(App.DATA, data.toString());

		if (update) updateData();
	}

	public void cancelNotification(int id) {
		Intent i = new Intent(this, NotificationReceiver.class);

		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(PendingIntent.getBroadcast(this, id, i, PendingIntent.FLAG_CANCEL_CURRENT));
	}

	public void move(int id, int parentId) {
		move(id, parentId, true);
	}

	public void move(int id, int parentId, boolean update) {
		data = App.move(id, parentId, data);
		editor.put(App.DATA, data.toString());

		if (update) updateData();
	}

	public void removeWithChildren(int id) {
		removeWithChildrenLoop(id);

		updateData();
	}

	public void removeWithChildrenLoop(int id) {
		try {
			JSONObject object = new JSONObject(data.getString(id + ""));

			if (object.has(App.CHILDREN_IDS) && !object.getString(App.CHILDREN_IDS).equals("")) {
				String[] childrenIds = object.getString(App.CHILDREN_IDS).split(",");

				for (String string : childrenIds) {
					removeWithChildrenLoop(Integer.parseInt(string));
				}
			} else remove(id, false);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		data = App.remove(id, data);
		editor.put(App.DATA, data.toString());
	}

	public void groupItemsInNewFolder(String newFolderName, int[] itemIds) {
		add(newFolderName, App.FOLDER);

		if (itemIds.length == 0) return;

		try {
			JSONObject newFolder = new JSONObject(data.getString((data.getInt(App.NUM_IDS) - 1) + ""));

			// Gets the old folder from the first item in the itemIds array's
			// parent id
			JSONObject child = new JSONObject(data.getString(itemIds[0] + ""));
			JSONObject oldFolder = new JSONObject(data.getString(child.getInt(App.PARENT_ID) + ""));

			String newChildrenString = "";
			for (int id : itemIds) {
				// builds the new children string
				newChildrenString += id + ",";

				// removes the children ids from the old parent's string
				if (data.has(id + "")) {
					data = App.setProperty(App.PARENT_ID, newFolder.getInt(App.ID), id, data);
				}
			}

			newChildrenString = newChildrenString.substring(0, newChildrenString.length() - 1);
			data = App.setProperty(App.CHILDREN_IDS, newChildrenString, newFolder.getInt(App.ID), data);

			String oldChildrenString = App.removeFromChildrenString(oldFolder, itemIds);
			data = App.setProperty(App.CHILDREN_IDS, oldChildrenString, oldFolder.getInt(App.ID), data);

			editor.put(App.DATA, data.toString());

			updateData();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void checkTask(int taskId, boolean isChecked) {
		data = App.checkTask(taskId, isChecked, data);

		if (isChecked) cancelNotification(taskId);
		else {
			try {
				JSONObject task = new JSONObject(data.getString(taskId + ""));
				if (task.has(Reminder.REMINDER_INFO)) Reminder.startReminder(task.getString(Reminder.REMINDER_INFO), this, task.getInt(App.ID), task);
			} catch (JSONException e) {

			}
		}

		editor.put(App.DATA, data.toString());
		updateData();
	}

	public void setProperty(String key, Object value, int id) {
		data = App.setProperty(key, value, id, data);
		editor.put(App.DATA, data.toString());
		updateData();
	}

	public void removeProperty(String key, int id) {
		data = App.removeProperty(key, id, data);
		editor.put(App.DATA, data.toString());
		updateData();
	}

	public void saveTask(View v) {
		if (contentViews.get(posInWrapper) instanceof TaskView) ((TaskView) contentViews.get(posInWrapper)).endEditDescription(true);
		else if (contentViews.get(posInWrapper) instanceof NoteView) ((NoteView) contentViews.get(posInWrapper)).endEditDescription(true);
		else endEditTitle(true);

		if (isEditingTitle()) endEditTitle(true);
	}

	private void openMenuItem(int id) {
		if (!canRun()) return;
		if (isMoving) return;
		hideOptions();
		App.hideKeyboard(this, focusDummy);

		try {
			if (openObjectId == id && !isInSettings()) return;

			openObjectId = id;

			JSONObject object = new JSONObject(data.getString(id + ""));

			setTitle(object.getString(App.NAME));

			posInWrapper = 0;
			contentViews.clear();

			scroller.startScroll(currentContentOffset, 0, -currentContentOffset, 0, 0);
			scrollHandler.postDelayed(scrollRunnable, scrollFps);

			contentViews.add(posInWrapper, new ItemView(this, openObjectId));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		updateData();
	}

	private void openFromIntent(int id) {
		String parentHierarchy = App.getParentHierarchyString(id, data);
		if (parentHierarchy.length() > 0) parentHierarchy = parentHierarchy.substring(0, parentHierarchy.length() - 1);

		String[] parentIds = parentHierarchy.split(",");
		int[] ids = new int[parentIds.length];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = Integer.parseInt(parentIds[i]);
		}
		openContentViewsFromParentIds(ids);
	}

	public void open(int id) {
		open(id, true);
	}

	public void open(int id, boolean animate) {
		if (isMoving && animate) return;
		hideOptions();
		if (isEditingTitle()) endEditTitle(false);
		App.hideKeyboard(this, focusDummy);

		try {
			openObjectId = id;

			JSONObject object = new JSONObject(data.getString(id + ""));

			setTitle(object.getString(App.NAME));

			if (animate) {
				scroller.startScroll(currentContentOffset, 0, -getContentWidth(), 0, App.ANIMATION_DURATION);
				scrollHandler.postDelayed(scrollRunnable, scrollFps);
			}

			posInWrapper++;

			if (object.getString(App.TYPE).equals(App.TASK)) contentViews.add(posInWrapper, new TaskView(this, openObjectId));
			else if (object.getString(App.TYPE).equals(App.FOLDER)) contentViews.add(posInWrapper, new ItemView(this, openObjectId));
			else if (object.getString(App.TYPE).equals(App.NOTE)) contentViews.add(posInWrapper, new NoteView(this, openObjectId));

			if (object.getString(App.TYPE).equals(App.TASK) || object.getString(App.TYPE).equals(App.NOTE)) {
				showCheck();
			} else {
				hideCheck();
			}

			backButton.setVisibility(View.VISIBLE);
			dragButton.setVisibility(View.GONE);
			updateData();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void openSettings() {
		if (getFlyInMenu().isVisible()) hideMenu();
		if (isInOptions()) hideOptions();
		if (contentViews.get(posInWrapper) instanceof SettingsView) return;

		posInWrapper = 0;
		contentViews.clear();

		setTitle(getResources().getString(R.string.settings));

		scroller.startScroll(currentContentOffset, 0, -currentContentOffset, 0, 0);
		scrollHandler.postDelayed(scrollRunnable, scrollFps);

		contentViews.add(posInWrapper, new SettingsView(this));

		updateData();
	}

	public void openSettingsItem(int id, String title) {
		openSettingsItem(id, title, true);
	}

	public void openSettingsItem(int id, String title, boolean animate) {
		if (isMoving) return;
		hideOptions();
		if (isEditingTitle()) endEditTitle(false);
		App.hideKeyboard(this, focusDummy);

		posInWrapper++;

		setTitle(title);

		if (id == SettingsView.SELECT_APP_LANGUAGE) contentViews.add(posInWrapper, new SelectLanguage(this));
		else if (id == SettingsView.SEND_FEEDBACK) contentViews.add(posInWrapper, new FeedbackView(this));

		scroller.startScroll(currentContentOffset, 0, -getContentWidth(), 0, (animate) ? App.ANIMATION_DURATION : 0);
		scrollHandler.postDelayed(scrollRunnable, scrollFps);

		backButton.setVisibility(View.VISIBLE);
		dragButton.setVisibility(View.GONE);
		updateData();
	}

	private void openContentViewsFromParentIds(int[] parentIds) {
		// Opens the first item as a menu item (since the first item among the
		// content items should be a menu item)
		openMenuItem(parentIds[0]);

		// Opens all the items without animating
		for (int i = 1; i < parentIds.length; i++) {
			open(parentIds[i], false);
		}

		// Scrolls to the content view which should be visible
		scroller.startScroll(currentContentOffset, 0, -posInWrapper * getContentWidth(), 0);
		scrollHandler.postDelayed(scrollRunnable, scrollFps);
	}

	public void goBack(View v) {
		goBack();
	}

	public void goBack() {
		if (isMoving) return;

		hideOptions();
		if (isEditingTitle()) endEditTitle(false);
		App.hideKeyboard(this, focusDummy);

		if (isInSettings()) {
			scroller.startScroll(currentContentOffset, 0, getContentWidth(), 0, App.ANIMATION_DURATION);
			scrollHandler.postDelayed(scrollRunnable, scrollFps);

			setTitle(getResources().getString(R.string.settings));

			if (contentViews.size() > posInWrapper) contentViews.get(posInWrapper).leave();
			posInWrapper--;
			return;
		}

		try {
			JSONObject object = new JSONObject(data.getString(openObjectId + ""));

			if (object.has(App.PARENT_ID + "") && data.has(object.getInt(App.PARENT_ID) + "")) setTitle(new JSONObject(data.getString(object.getInt(App.PARENT_ID) + "")).getString(App.NAME));

			openObjectId = object.getInt(App.PARENT_ID);

			object = new JSONObject(data.getString(openObjectId + ""));

			if (object.getString(App.TYPE).equals(App.FOLDER)) {
				JSONObject o = new JSONObject(data.getString(openObjectId + ""));
				if (!o.has(App.PARENT_ID) || o.getInt(App.PARENT_ID) == -1) {
					backButton.setVisibility(View.GONE);

					if (!isInMasterView()) dragButton.setVisibility(View.VISIBLE);
				}
			}

			scroller.startScroll(currentContentOffset, 0, getContentWidth(), 0, App.ANIMATION_DURATION);
			scrollHandler.postDelayed(scrollRunnable, scrollFps);

			if (contentViews.size() > posInWrapper) contentViews.get(posInWrapper).leave();
			posInWrapper--;

			if (posInWrapper >= 0 && posInWrapper < contentViews.size()) if (contentViews.get(posInWrapper) == null) contentViews.add(posInWrapper, new ItemView(this, openObjectId));

			// Toggles the check and add button (just in case)
			hideCheck();

			updateData();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void adjustContentPosition(boolean isAnimationOngoing) {
		int offset = scroller.getCurrX();

		for (int i = 0; i < wrapper.getChildCount(); i++) {
			LayoutParams params;
			if (wrapper.getChildAt(i).getLayoutParams() != null) params = (LayoutParams) wrapper.getChildAt(i).getLayoutParams();
			else params = new LayoutParams(getContentWidth(), LayoutParams.FILL_PARENT);

			params.setMargins(offset, 0, -offset, 0);
			wrapper.getChildAt(i).setLayoutParams(params);
		}

		wrapper.invalidate();

		if (isAnimationOngoing) scrollHandler.postDelayed(scrollRunnable, 16);
		else {
			currentContentOffset = offset;
			isMoving = false;
		}

	}

	private void setTitle(String name) {
		nameTV.setText(name);
	}

	private void startEditTitle() {
		if (!canRun()) return;
		if (isInSettings()) return;
		showCheck();
		enableCheck();

		nameTV.setVisibility(View.GONE);
		nameET.setVisibility(View.VISIBLE);

		String name = nameTV.getText().toString();

		// Gets the real name of the open object (just in case)
		try {
			JSONObject o = new JSONObject(data.getString(openObjectId + ""));
			if (o.has(App.NAME)) name = o.getString(App.NAME);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		nameET.setText(name);
		nameET.requestFocus();
	}

	private void endEditTitle(boolean save) {
		nameTV.setVisibility(View.VISIBLE);
		nameET.setVisibility(View.GONE);
		hideCheck();

		focusDummy.requestFocus();

		if (save) {
			setProperty(App.NAME, nameET.getText().toString(), openObjectId);
			nameTV.setText(nameET.getText().toString());
		}
	}

	public boolean isEditingTitle() {
		return nameET.getVisibility() == View.VISIBLE;
	}

	public void toggleOptions() {
		if (options.getVisibility() == View.GONE) showOptions();
		else hideOptions();
	}

	public void showOptions() {
		if (isEditingTitle()) endEditTitle(false);

		options.setVisibility(View.VISIBLE);
		titleBar.setVisibility(View.GONE);

		if (contentViews.get(posInWrapper) instanceof ItemView) ((ItemView) contentViews.get(posInWrapper)).enterOptionsMode();

		options.clearOptionsItems();
		options.addOptionsItem(App.OPTIONS_REMOVE);
		options.addOptionsItem(App.OPTIONS_GROUP_ITEMS);
		options.addOptionsItem(App.OPTIONS_SELECT_ALL);
		options.addOptionsItem(App.OPTIONS_MOVE);

		updateData();
	}

	public void hideOptions() {
		if (0 <= posInWrapper && posInWrapper < contentViews.size()) //
		if (contentViews.get(posInWrapper) instanceof ItemView) ((ItemView) contentViews.get(posInWrapper)).exitOptionsMode();

		titleBar.setVisibility(View.VISIBLE);
		options.setVisibility(View.GONE);

		updateData();
	}

	public boolean isInOptions() {
		if (contentViews.get(posInWrapper) instanceof ItemView) return ((ItemView) contentViews.get(posInWrapper)).isInOptionsMode();
		return false;
	}

	public void updateChildrenOrder(String children, int parentId) {
		data = App.updateChildrenOrder(children, parentId, data);

		editor.put(App.DATA, data.toString());
		updateData();
	}

	public void onBackPressed() {
		if (!canRun()) return;
		if (isMoving) return;

		if (getFlyInMenu().isVisible()) {
			if (getFlyInMenu().isInOptionsMode()) getFlyInMenu().disableOptions();
			else hideMenu();
			return;
		}

		if (isEditingTitle()) {
			endEditTitle(true);
			return;
		}

		if (posInWrapper < contentViews.size()) if (contentViews.get(posInWrapper) instanceof ItemView) {
			if (((ItemView) contentViews.get(posInWrapper)).isInOptionsMode()) {
				hideOptions();
				return;
			}
		}

		if (posInWrapper != 0) {
			goBack();
			return;
		}

		super.onBackPressed();
	}

	protected class AnimationRunnable implements Runnable {
		public void run() {
			isMoving = true;
			boolean isAnimationOngoing = scroller.computeScrollOffset();

			adjustContentPosition(isAnimationOngoing);
		}
	}

	public FlyInMenu getFlyInMenu() {
		return super.getFlyInMenu();
	}

	public int getMenuWidth() {
		if (width * 0.8 > App.dpToPx(300, getResources()) && App.dpToPx(300, getResources()) < width) return App.dpToPx(300, getResources());
		else return (int) (width * 0.8);
	}

	public boolean isMoving() {
		return isMoving;
	}

	public void isMoving(boolean b) {
		isMoving = b;
	}

	public boolean isInMasterView() {
		return getResources().getString(com.espian.flyin.library.R.string.is_in_master_view).equals("true");
	}

	public int getPosInWrapper() {
		return posInWrapper;
	}

	public FrameLayout getDragButton() {
		return dragButton;
	}

	public FrameLayout getAddButton() {
		return (FrameLayout) findViewById(R.id.addButton);
	}

	public ContentView getOpenContentView() {
		return contentViews.get(posInWrapper);
	}

	public OptionsBar getOptionsBar() {
		return options;
	}

	public int getBarHeight() {
		return (int) getResources().getDimension(R.dimen.item_height);
	}

	public int getBarBorderHeight() {
		return (int) getResources().getDimension(R.dimen.bar_border_height);
	}

	public int getContentWidth() {
		if (isInMasterView()) return width - getMenuWidth();
		else return width;
	}

	public int getContentHeight() {
		return height - getBarHeight() - getBarBorderHeight();
	}

	public JSONObject getData() {
		return data;
	}

	public void updateContentItemsOrder() {
		String order = "";

		// Menu items are listed in reverse order when compared to
		// the other items. Therefore, I put the last item first.
		for (int i = getFlyInMenu().getMenuItems().size() - 1; i >= 0; i--) {
			order += ((BaseAdapter) getFlyInMenu().getListView().getAdapter()).getItemId(i) + ",";
		}

		// Removes the last ',' from the string
		order = order.substring(0, order.length() - 1);

		updateChildrenOrder(order, -1);
	}

	// These functions saves the open item and opens it when the app is
	// recreated (eg. when the device is tilted)
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		// Saves all the parent ids of the content views from the first to the
		// visible view (posInWrapper)
		int[] parentIds = new int[posInWrapper + 1];
		for (int i = 0; i < parentIds.length; i++) {
			if (contentViews.size() > i) {
				if (contentViews.get(i) != null) parentIds[i] = contentViews.get(i).getParentId();
			}
		}

		savedInstanceState.putIntArray("contentViewsOpen", parentIds);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		int parentIds[] = savedInstanceState.getIntArray("contentViewsOpen");

		openContentViewsFromParentIds(parentIds);
	}

	public boolean isInSettings() {
		for (ContentView i : contentViews) {
			if (i instanceof SettingsView) {
				return true;
			}
		}
		return false;
	}

	public String getColorTheme() {
		return prefs.getString(App.SETTINGS_THEME, App.SETTINGS_THEME_LIGHT);
	}

	public boolean isDarkTheme() {
		return getColorTheme().equals(App.SETTINGS_THEME_DARK);
	}

	public boolean is24HourMode() {
		return prefs.getBoolean(App.SETTINGS_24_HOUR_CLOCK, true);
	}

	public int getSortType() {
		return prefs.getInt(App.SETTINGS_SORT_TYPE, Sort.SORT_NOTHING);
	}

	// not tested yet
	public void setLocale(String lang) {
		Locale l = new Locale(lang);
		Resources r = getResources();
		DisplayMetrics dm = r.getDisplayMetrics();
		Configuration c = r.getConfiguration();
		c.locale = l;
		r.updateConfiguration(c, dm);

		editor.put(App.LANGUAGE, lang);

		Intent refresh = new Intent(this, MainActivity.class);
		refresh.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		overridePendingTransition(0, 0);
		refresh.putExtra(App.OPEN, App.SETTINGS);
		refresh.putExtra(App.TYPE, App.SETTINGS_APP_LANGUAGE);
		startActivity(refresh);
	}

	public Locale getLocale() {
		String lang = prefs.getString(App.LANGUAGE, "en");
		Configuration c = getBaseContext().getResources().getConfiguration();
		if (!lang.equals("") && !c.locale.getLanguage().equals(lang)) return new Locale(lang);

		return null;
	}

	public String getLocaleString() {
		return prefs.getString(App.LANGUAGE, "en");
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Locale l = getLocale();
		if (l == null) return;
		newConfig.locale = l;
		Locale.setDefault(l);
		getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
	}

	public void changeTheme(String theme) {
		saveSetting(App.SETTINGS_THEME, theme);

		for (ContentView i : contentViews)
			i.setColors();

		if (Build.VERSION.SDK_INT >= 11) {
			if (!isDarkTheme()) setTheme(android.R.style.Theme_Holo_Light);
			else setTheme(android.R.style.Theme_Holo_Panel);
		} else {
			if (!isDarkTheme()) setTheme(android.R.style.Theme_Light);
			else setTheme(android.R.style.Theme_Black);
		}

		setColors();
	}

	private void setColors() {
		titleBar.setBackgroundColor(getResources().getColor(isDarkTheme() ? R.color.background_color_dark : R.color.aqua_blue));
		nameTV.setTextColor(getResources().getColor(isDarkTheme() ? R.color.aqua_blue : R.color.white));
		nameET.setTextColor(getResources().getColor(isDarkTheme() ? R.color.aqua_blue : R.color.white));
		int iconColor = getResources().getColor(isDarkTheme() ? R.color.aqua_blue : R.color.white);
		((ImageView) findViewById(R.id.drag_icon)).getBackground().setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
		((ImageView) findViewById(R.id.back_icon)).getBackground().setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
		((ImageView) findViewById(R.id.add_icon)).getBackground().setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
		((ImageView) findViewById(R.id.save_icon)).getBackground().setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
		getFlyInMenu().setColors();
	}

	public void showCheck() {
		if (contentViews.get(posInWrapper) instanceof TaskView || contentViews.get(posInWrapper) instanceof NoteView) {
			findViewById(R.id.saveTask).setVisibility(View.VISIBLE);
			findViewById(R.id.addButton).setVisibility(View.GONE);
			disableCheck();
		} else if (contentViews.get(posInWrapper) instanceof ItemView) {
			findViewById(R.id.saveTask).setVisibility(View.VISIBLE);
			findViewById(R.id.addButton).setVisibility(View.GONE);
		}
	}

	public void hideCheck() {
		if (contentViews.get(posInWrapper) instanceof TaskView || contentViews.get(posInWrapper) instanceof NoteView) disableCheck();
		else {
			findViewById(R.id.addButton).setVisibility(View.VISIBLE);
			findViewById(R.id.saveTask).setVisibility(View.GONE);
		}
	}

	public void disableCheck() {
		((FrameLayout) findViewById(R.id.saveTask)).findViewById(R.id.save_icon).getBackground().setAlpha((int) (0.3 * 255));
	}

	public void enableCheck() {
		((FrameLayout) findViewById(R.id.saveTask)).findViewById(R.id.save_icon).getBackground().setAlpha(255);
	}

	public void saveSetting(String settingName, boolean settingValue) {
		editor.put(settingName, settingValue);
	}

	public void saveSetting(String settingName, int settingValue) {
		editor.put(settingName, settingValue);
	}

	public void saveSetting(String settingName, String settingValue) {
		editor.put(settingName, settingValue);
	}

	public String getSetting(String settingName) {
		return prefs.getString(settingName, "");
	}

	public boolean canRun() {
		return canRun || !freeVersion;
	}
}