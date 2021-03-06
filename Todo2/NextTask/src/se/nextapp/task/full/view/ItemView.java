package se.nextapp.task.full.view;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import se.nextapp.task.full.MainActivity;
import se.nextapp.task.full.R;
import se.nextapp.task.full.adapter.ItemAdapter;
import se.nextapp.task.full.animation.CollapseAnimation;
import se.nextapp.task.full.animation.ExpandAnimation;
import se.nextapp.task.full.dialog.FolderSelectionDialog;
import se.nextapp.task.full.dialog.TextLineDialog;
import se.nextapp.task.full.item.ContentItem;
import se.nextapp.task.full.item.FolderItem;
import se.nextapp.task.full.item.NoteItem;
import se.nextapp.task.full.item.TaskItem;
import se.nextapp.task.full.misc.App;
import se.nextapp.task.full.misc.Sort;
import se.nextapp.task.full.tutorial.Tutorial;
import se.nextapp.task.full.tutorial.TutorialState;
import se.nextapp.task.full.xml.dynamic.DynamicListView;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class ItemView extends ContentView {

	private DynamicListView listView;
	private TextView empty;

	private ArrayList<ContentItem> contentItems;
	private ArrayList<ContentItem> selectedItems;

	private ItemAdapter adapter;

	private int itemHeight;
	private int expandingItemId = -1;

	private boolean optionsMode = false;
	private boolean sortPrioritized = false;
	private boolean includeSubFolders = false;

	public ItemView(MainActivity activity, int parentId) {
		super(activity, parentId);
		init();
	}

	protected void init() {
		super.init();
		
		LayoutInflater.from(activity).inflate(R.layout.item_view, this, true);
		setLayoutParams(new LayoutParams(activity.getContentWidth(), LayoutParams.FILL_PARENT));

		contentItems = new ArrayList<ContentItem>();
		selectedItems = new ArrayList<ContentItem>();

		listView = (DynamicListView) findViewById(R.id.listview);
		listView.setItemView(this);

		adapter = new ItemAdapter(activity, this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (activity.isMoving()) return;
				if (isInOptionsMode()) return;
				if(activity.getTutorialState() == TutorialState.ENTER_TASK) if(Tutorial.itemToBeClicked != view.getId()) return;

				try {
					JSONObject object = new JSONObject(activity.getData().getString(view.getId() + ""));
					activity.open(object.getInt(App.ID));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});

		empty = (TextView) findViewById(R.id.empty);
		empty.setText(getContext().getResources().getString(R.string.empty));

		itemHeight = (int) activity.getResources().getDimension(R.dimen.item_height);

		setColors();
	}

	public void setColors() {
		Resources r = activity.getResources();
		boolean dark = activity.isDarkTheme();
		listView.setBackgroundColor((dark) ? r.getColor(R.color.background_color_dark) : r.getColor(R.color.background_color_light));
		listView.setDivider((dark) ? r.getDrawable(R.color.divider_color_dark) : r.getDrawable(R.color.divider_color_light));
		listView.setDividerHeight(r.getDimensionPixelSize(R.dimen.divider_height));
	}

	public void update(JSONObject data) {
		try {
			contentItems.clear();

			// if (includeSubFolders) {
			// if (data.has(parentId + "")) {
			// JSONObject parent = new JSONObject(data.getString(parentId +
			// ""));
			// if (parent.has(App.CHILDREN_IDS))
			// getItemsIncludingSubFolders(data,
			// parent.getString(App.CHILDREN_IDS).split(","));
			// }
			// } else
			getItemsFromThisFolder(data);

			if (!isInOptionsMode() && activity.getTutorialState() == TutorialState.END) sortItems();
			
			adapter.notifyDataSetChanged();

			listView.setContentItems(contentItems);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void getItemsIncludingSubFolders(JSONObject data, String[] childrenIds) throws JSONException {
		for (String id : childrenIds) {
			if (!data.has(id)) continue;

			JSONObject child = new JSONObject(data.getString(id));

			if (child.getString(App.TYPE).equals(App.FOLDER)) getItemsIncludingSubFolders(data, child.getString(App.CHILDREN_IDS).split(","));
			else {
				if (child.getString(App.TYPE).equals(App.TASK)) {
					TaskItem item = new TaskItem();
					item.setTitle(child.getInt(App.PARENT_ID) + " : " + child.getString(App.NAME));
					item.setId(child.getInt(App.ID));
					item.setType(App.TASK);
					item.setParentId(parentId);

					if (child.has(App.PRIORITIZED) && child.getBoolean(App.PRIORITIZED)) item.isPrioritized(true);
					else item.isPrioritized(false);

					if (child.has(App.TIMESTAMP_CREATED)) item.setTimestampCreated(child.getInt(App.TIMESTAMP_CREATED));
					if (child.has(App.TIMESTAMP_COMPLETED)) item.setTimestampChecked(child.getInt(App.TIMESTAMP_COMPLETED));
					if (child.has(App.DUE_DATE)) item.setDueDate(child.getLong(App.DUE_DATE));
					if (child.has(App.COMPLETED) && child.getBoolean(App.COMPLETED)) item.completed(true);
					else item.completed(false);

					contentItems.add(item);
				}
			}
		}
	}

	private void getItemsFromThisFolder(JSONObject data) throws JSONException {
		if (!data.has(parentId + "")) return;
		JSONObject parent = new JSONObject(data.getString(parentId + ""));

		String childrenIds[];

		if (parent.has(App.CHILDREN_IDS)) childrenIds = parent.getString(App.CHILDREN_IDS).split(",");
		else childrenIds = new String[0];

		for (String id : childrenIds) {
			if (!data.has(id)) continue;

			JSONObject object = new JSONObject(data.getString(id));

			if (object.getString(App.TYPE).equals(App.TASK)) {
				TaskItem item = new TaskItem();
				item.setTitle(object.getString(App.NAME));
				item.setId(object.getInt(App.ID));
				item.setType(App.TASK);
				item.setParentId(parentId);

				if (object.has(App.PRIORITIZED) && object.getBoolean(App.PRIORITIZED)) item.isPrioritized(true);
				else item.isPrioritized(false);

				if (object.has(App.TIMESTAMP_CREATED)) item.setTimestampCreated(object.getInt(App.TIMESTAMP_CREATED));
				if (object.has(App.TIMESTAMP_COMPLETED)) item.setTimestampChecked(object.getInt(App.TIMESTAMP_COMPLETED));
				if (object.has(App.DUE_DATE)) item.setDueDate(object.getLong(App.DUE_DATE));
				if (object.has(App.COMPLETED) && object.getBoolean(App.COMPLETED)) item.completed(true);
				else item.completed(false);

				contentItems.add(item);
			} else if (object.getString(App.TYPE).equals(App.FOLDER)) {
				FolderItem item = new FolderItem();
				item.setTitle(object.getString(App.NAME));
				item.setParentId(parentId);
				item.setId(object.getInt(App.ID));
				item.setType(App.FOLDER);
				if (object.has(App.TIMESTAMP_CREATED)) item.setTimestampCreated(object.getInt(App.TIMESTAMP_CREATED));

				if (object.has(App.PRIORITIZED) && object.getBoolean(App.PRIORITIZED)) item.isPrioritized(true);
				else item.isPrioritized(false);

				contentItems.add(item);
			} else if (object.getString(App.TYPE).equals(App.NOTE)) {
				NoteItem item = new NoteItem();
				item.setTitle(object.getString(App.NAME));
				item.setParentId(parentId);
				item.setId(object.getInt(App.ID));
				item.setType(App.NOTE);
				if (object.has(App.TIMESTAMP_CREATED)) item.setTimestampCreated(object.getInt(App.TIMESTAMP_CREATED));

				if (object.has(App.PRIORITIZED) && object.getBoolean(App.PRIORITIZED)) item.isPrioritized(true);
				else item.isPrioritized(false);

				contentItems.add(item);
			}
		}
	}

	public void expandView(final View view) {
		new ExpandAnimation(view, App.ANIMATION_DURATION, itemHeight).animate();
	}

	public void collapseView(final View view) {
		new CollapseAnimation(view, App.ANIMATION_DURATION, itemHeight).animate();
	}

	public void updateContentItemsOrder() {
		String order = "";
		for (int i = 0; i < contentItems.size(); i++) {
			order += adapter.getItemId(i) + ",";
		}
		// Removes the last ',' from the string
		order = order.substring(0, order.length() - 1);
		activity.updateChildrenOrder(order, parentId);
	}
 
	public void toggleItem(int id) {
		// If the selected items contains the id it is removed
		for (ContentItem i : selectedItems) {
			if (i.getId() == id) {
				selectedItems.remove(i);
				updateBackgroundColor(id);

				activity.getOptionsBar().updateSelectedCount(selectedItems.size(), selectedItems.size() == contentItems.size());
				return;
			}
		}

		// If it does not exist, it is added to the selected items
		for (ContentItem i : contentItems) {
			if (i.getId() == id && !selectedItems.contains(i)) {
				selectedItems.add(i);
				updateBackgroundColor(id);

				activity.getOptionsBar().updateSelectedCount(selectedItems.size(), selectedItems.size() == contentItems.size());
				return;
			}
		}
	}

	private void updateBackgroundColor(int id) {
		View v = getViewById(id);
		if (v == null) return;

		if (isSelected(id)) v.setBackgroundDrawable(activity.getResources().getDrawable(activity.isDarkTheme() ? R.drawable.blue_item_selector_dark : R.drawable.blue_item_selector_light));
		else if (activity.isDarkTheme()) v.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.item_selector_dark));
		else v.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.item_selector_white));
	}

	public void performActionOnSelectedItems(int id) {
		if (id == App.OPTIONS_REMOVE) removeSelectedItems();
		if (id == App.OPTIONS_GROUP_ITEMS) groupSelectedItems();
		if (id == App.OPTIONS_SELECT_ALL) toggleSelection();
		if (id == App.OPTIONS_MOVE) moveSelectedItems();
		
		updateOptions();
	}
	
	private void updateOptions() {
		activity.getOptionsBar().updateSelectedCount(selectedItems.size(), selectedItems.size() == contentItems.size());
	}

	private void removeSelectedItems() {
		if (selectedItems.size() == 0) return;

		for (ContentItem i : selectedItems) {
			View v = getViewById(i.getId());
			if (v != null) collapseView(v);
		}

		// Since the items should not be removed until they have collapsed
		// the removing of the items is delayed by the animation duration.
		// The selected items array is then emptied.
		new Handler().postDelayed(new Runnable() {
			public void run() {
				for (ContentItem i : selectedItems) {
					activity.removeWithChildren(i.getId());
				}
				
				selectedItems.clear();
				updateOptions();
			}
		}, App.ANIMATION_DURATION);
	}

	private void groupSelectedItems() {
		if (selectedItems.size() == 0) return;

		Resources r = getContext().getResources();
		TextLineDialog d = new TextLineDialog(activity, r.getString(R.string.group_selected_items_in_new_folder), r.getString(R.string.name_the_new_folder), true, r.getString(R.string.add), r.getString(R.string.cancel)) {
			protected void onResult(Object result) {

				final String name;
				if (result instanceof Object) name = (String) result;
				else name = "";

				final int[] selectedItemIds = new int[selectedItems.size()];

				for (int i = 0; i < selectedItems.size(); i++) {
					selectedItemIds[i] = selectedItems.get(i).getId();

					collapseView(getViewById(selectedItems.get(i).getId()));
				}

				selectedItems.clear();

				new Handler().postDelayed(new Runnable() {
					public void run() {
						activity.groupItemsInNewFolder(name, selectedItemIds);
						updateOptions();
					}
				}, App.ANIMATION_DURATION);
			}
		};

		d.show();

	}

	private void toggleSelection() {
		if (selectedItems.size() != contentItems.size()) {
			selectedItems.clear();
			selectedItems.addAll(contentItems);
		} else selectedItems.clear();

		for (ContentItem i : contentItems)
			updateBackgroundColor(i.getId());
	}

	private void moveSelectedItems() {
		if (selectedItems.size() == 0) return;

		Resources r = getContext().getResources();
		FolderSelectionDialog d = new FolderSelectionDialog(activity, //
				r.getString(R.string.move_selected_items), //
				r.getString(R.string.select_a_folder_to_move_to), //
				false, selectedItems, activity.getData(), //
				r.getString(R.string.move), //
				r.getString(R.string.cancel)) {
			protected void onResult(Object result) {
				getAlertDialog().dismiss();

				final Integer id;
				if (result instanceof Integer) id = (Integer) result;
				else return;

				for (ContentItem i : selectedItems)
					collapseView(getViewById(i.getId()));

				new Handler().postDelayed(new Runnable() {
					public void run() {
						for (ContentItem i : selectedItems) {
							// Checks that the parent id is not the same
							if (i.getParentId() != id) activity.move(i.getId(), id);
						}

						selectedItems.clear();
						updateOptions();
					}
				}, App.ANIMATION_DURATION);
			}
		};

		d.show();
	}

	private void sortItems() {
		int sortType = activity.getSortType();

		if (sortType == Sort.SORT_PRIORITIZED) {
			Sort.sortPrioritized(contentItems, sortPrioritized);
		} else if (sortType == Sort.SORT_TIMESTAMP_CREATED) {
			Sort.sortTimestampCreated(contentItems, activity.getLocaleString());
		} else if (sortType == Sort.SORT_COMPLETED) {
			Sort.sortCompleted(contentItems);
		} else if (sortType == Sort.SORT_ALPHABETICALLY) {
			Sort.sortAlphabetically(contentItems);
		} else if (sortType == Sort.SORT_DUE_DATE) {
			Sort.sortDueDate(contentItems, activity);
		}
	}

	public View getViewById(int id) {
		for (int i = 0; i < listView.getChildCount(); i++) {
			View v = listView.getChildAt(i);
			if (v.getId() == id) return v;
		}

		return null;
	}

	public boolean isSelected(int id) {
		for (ContentItem i : selectedItems)
			if (i.getId() == id) return true;

		return false;
	}

	public void enterOptionsMode() {
		optionsMode = true;
		// Clears the selected items every time the options mode is entered
		selectedItems.clear();
	}

	public void exitOptionsMode() {
		optionsMode = false;
	}

	public boolean isInOptionsMode() {
		return optionsMode;
	}

	public void leave() {

	}

	public void setExpandingItemId(int id) {
		expandingItemId = id;
	}

	public int getExpandingItemId() {
		return expandingItemId;
	}

	public void invalidateExpandingItemId() {
		expandingItemId = -1;
	}

	public ArrayList<ContentItem> getContentItems() {
		return contentItems;
	}

	public int getItemHeight() {
		return itemHeight;
	}

	public DynamicListView getListView() {
		return listView;
	}
	
	public int getNumberOfItems() {
		return contentItems.size();
	}
}