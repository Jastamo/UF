package com.todo.code3.xml;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.todo.code3.MainActivity;
import com.todo.code3.gesture.SimpleGestureFilter;
import com.todo.code3.gesture.SimpleGestureFilter.SimpleGestureListener;
import com.todo.code3.misc.App;

public class Wrapper extends RelativeLayout implements SimpleGestureListener {

	private int x, y, startX, startY, lastX;

	private long startTime;

	private int dragStartLocation = -1;

	private static final int MENU_CLOSED = 0;
	private static final int MENU_OPEN = 1;

	private boolean isDragging = false;
	private boolean hasStarted = false;

	private boolean addTouch = false;
	private Button[] touchButtons;

	private ViewConfiguration viewConfig;
	private MainActivity activity;
	private SimpleGestureFilter detector;

	public Wrapper(Context context) {
		super(context);
		init();
	}

	public Wrapper(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	private void init() {
		viewConfig = ViewConfiguration.get(getContext());
		detector = new SimpleGestureFilter(getContext(), this);
	}

	public boolean onTouchEvent(MotionEvent e) {
		if (activity == null) return true;

		if (addTouch) return onAddTouchEvent(e);

		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			startX = x = lastX = (int) e.getRawX();
			startY = y = (int) e.getRawY() - App.getStatusBarHeight(activity.getResources());

			startTime = System.currentTimeMillis();
			hasStarted = true;

			// If the swipe started between 0 and
			// 30 dp from the screens right side,
			// the user started the swipe at
			// the correct location to be able to
			// open the menu
			if (x <= App.dpToPx(App.BEZEL_AREA_DP, getContext().getResources()) && x >= 0) dragStartLocation = MENU_CLOSED;
			if (activity.getMenuWidth() < e.getRawX()) dragStartLocation = MENU_OPEN;

		} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			if (!hasStarted) return true;

			x = (int) e.getRawX();
			y = (int) e.getRawY() - App.getStatusBarHeight(activity.getResources());

			if (dragStartLocation != -1) {
				// If the swipe is long enough to be considered a scroll
				if (Math.hypot(x - startX, y - startY) > viewConfig.getScaledTouchSlop()) isDragging = true;

				if (isDragging) {
					int dx = x - lastX;
					activity.getFlyInMenu().moveMenu(dx);
					lastX = x;
				}
			}
		} else if (e.getAction() == MotionEvent.ACTION_UP) {
			if (!hasStarted) return true;

			if (isDragging) {
				// delta time
				double dt = (System.currentTimeMillis() - startTime) / 1000D;

				if (Math.abs(x - startX) / dt > viewConfig.getScaledMinimumFlingVelocity() * 4) {
					if (dragStartLocation == MENU_OPEN) activity.hideMenu();
					else activity.showMenu();
				} else {
					if (activity.getFlyInMenu().getContentOffset() > activity.getContentWidth() / 2) activity.showMenu();
					else activity.hideMenu();
				}
			} else {
				FrameLayout b = activity.getDragButton();

				if (dragStartLocation == MENU_OPEN) activity.hideMenu();
				else if (b.getLeft() < startX && startX < b.getRight() //
						&& b.getTop() < startY && startY < b.getBottom()) activity.toggleMenu();

			}

			isDragging = hasStarted = false;
			dragStartLocation = -1;
		}

		return true;
	}

	private boolean onAddTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			showAddOptions();
			startX = x = (int) e.getRawX();
			startY = y = (int) e.getRawY() - App.getStatusBarHeight(activity.getResources());
		} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			x = (int) e.getRawX();
			y = (int) e.getRawY() - App.getStatusBarHeight(activity.getResources());

			if (y != startY && x != startX) {
				double v = Math.atan((y - startY) * 1.0 / (x - startX) * 1.0);
				v = Math.toDegrees(v);

				for (Button b : touchButtons)
					b.setBackgroundDrawable(getContext().getResources().getDrawable(com.todo.code3.R.drawable.rounded_corners));
				
				int selected = 0;

				if (-90 < v && v < -60) selected = 2;
				else if (-60 < v && v < -30) selected = 1;
				else if (-30 < v && v < 0) selected = 0;
				
				touchButtons[selected].setBackgroundDrawable(getContext().getResources().getDrawable(com.todo.code3.R.drawable.rounded_corners_selected));
			}
		} else if (e.getAction() == MotionEvent.ACTION_UP) {
			addTouch = false;

			if (Math.hypot(x - startX, y - startY) < viewConfig.getScaledTouchSlop()) activity.getAddButton().performClick();
			else if (Math.hypot(x - startX, y - startY) > 50 && y != startY && x != startX) {
				double v = Math.atan((y - startY) * 1.0 / (x - startX) * 1.0);
				v = Math.toDegrees(v);

				if (-90 < v && v < -60) activity.addDialog(App.FOLDER);
				else if (-60 < v && v < -30) activity.addDialog(App.NOTE);
				else if (-30 < v && v < 0) activity.addDialog(App.TASK);
			}
		}

		return true;
	}

	public boolean onInterceptTouchEvent(MotionEvent e) {
		// If the menu is visible, the user is able to drag,
		// as long as he does not drag on the menu
		if (!isDragging && activity.getFlyInMenu().isVisible()) {
			return true;
		}

		FrameLayout b = activity.getDragButton();
		// if the back button is visible and the user touches
		// the button, the menu should not open
		if (e.getRawX() < b.getRight() && e.getRawY() - App.getStatusBarHeight(activity.getResources()) < b.getBottom()) {
			if (activity.getPosInWrapper() != 0) return false;
			else return true;
		}

		// If the touch is inside the touch area for the drag, the user should
		// be able to drag
		if (e.getRawX() <= App.dpToPx(App.BEZEL_AREA_DP, getContext().getResources())) return true;

		FrameLayout add = activity.getAddButton();
		if (add.getLeft() < e.getRawX() && e.getRawX() < add.getRight()) {
			if (add.getTop() < e.getRawY() - App.getStatusBarHeight(activity.getResources()) && e.getRawY() - App.getStatusBarHeight(activity.getResources()) < add.getBottom()) {
				addTouch = true;
				return true;
			}
		}

		return false;
	}

	public void setActivity(MainActivity a) {
		activity = a;
	}

	public boolean dispatchTouchEvent(MotionEvent e) {
		detector.onTouchEvent(e);
		return super.dispatchTouchEvent(e);
	}

	public void onSwipe(int direction) {
		if (!isDragging) {
			if (direction == SimpleGestureFilter.SWIPE_RIGHT) {
				if (activity.getPosInWrapper() == 0) activity.showMenu();
				else activity.goBack();
			}
		}
	}

	private void showAddOptions() {
		if (touchButtons == null) touchButtons = new Button[3];

		int x, y;
		int numButtons = 3;
		FrameLayout add = activity.getAddButton();
		x = (add.getRight() + add.getLeft()) / 2;
		y = (add.getTop() + add.getBottom()) / 2;

		int d = App.dpToPx(150, activity.getResources());
		double v[] = { Math.toRadians(15), Math.toRadians(45), Math.toRadians(75) };
		int w = App.dpToPx(40, activity.getResources());
		int hw = w / 2;

		for (int i = 0; i < numButtons; i++) {
			touchButtons[i] = new Button(getContext());
			touchButtons[i].setBackgroundDrawable(getContext().getResources().getDrawable(com.todo.code3.R.drawable.rounded_corners));
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(w, w);
			// p.leftMargin = x - 5;
			// p.topMargin = y - 5;
			// p.addRule(RelativeLayout.RIGHT_OF, com.todo.code3.R.id.line2);
			p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			p.addRule(RelativeLayout.ALIGN_PARENT_TOP);

			p.rightMargin = (int) (d * Math.cos(v[i])) - hw;
			p.topMargin = (int) (d * Math.sin(v[i])) - hw;

			touchButtons[i].setLayoutParams(p);
			addView(touchButtons[i]);
		}
	}
}
