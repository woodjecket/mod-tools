package modtools.ui.components.linstener;

import arc.*;
import arc.Graphics.Cursor;
import arc.Graphics.Cursor.SystemCursor;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.layout.Scl;
import arc.util.Reflect;

public class SclListener extends ClickListener {
	public static Element fireElement;
	public        boolean disabled0, disabled1;
	protected boolean isDisabled() {
		return disabled0 || disabled1;
	}
	public static final float defOffset = 8;
	public              float offset    = defOffset;
	public              float defWidth, defHeight, defX, defY, minW, minH;

	public final Element bind;

	public SclListener(Element element, float minW, float minH) {
		if (element == null) throw new IllegalArgumentException("element is null");
		bind = element;
		bind.addCaptureListener(this);
		set(minW, minH);
	}
	public void rebind() {
		bind.addCaptureListener(this);
	}
	public void unbind() {
		;
		bind.removeCaptureListener(this);
	}

	public void set(float minW, float minH) {
		this.minW = Scl.scl(minW);
		this.minH = Scl.scl(minH);
	}

	public boolean left, bottom, right, top;
	public Runnable listener = null;

	public boolean valid(float x, float y) {
		left = Math.abs(x) < offset;
		right = Math.abs(x - bind.getWidth()) < offset;
		bottom = Math.abs(y) < offset;
		top = Math.abs(y - bind.getHeight()) < offset;
		return (left || right || bottom || top) && !isDisabled();
	}

	public Vec2    last   = new Vec2();
	public boolean scling = false;

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
		if (bind.parent == null || isDisabled()) return false;
		last.set(x, y);

		if (valid(x, y)) {
			scling = true;
			change.set(0, 0);
			defWidth = bind.getWidth();
			defHeight = bind.getHeight();
			defX = bind.x;
			defY = bind.y;
			fireElement = event.listenerActor;
			return true;
		}
		return false;
	}

	/** 用于移动时改变坐标的元素（left|bottom） */
	public Vec2 change = new Vec2();
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		if (isDisabled()) return;
		scling = true;
		if (change.x != 0) {
			x += change.x;
			change.x = 0;
		}
		if (change.y != 0) {
			y += change.y;
			change.y = 0;
		}
		Core.graphics.cursor(getCursor());
		if (left) {
			float w = Mathf.clamp(defWidth - x + last.x, minW, Core.graphics.getWidth());
			bind.setWidth(w);
			change.x = defWidth - w;
			bind.x = defX + change.x;
		}
		if (right) {
			bind.setWidth(Mathf.clamp(defWidth + x - last.x, minW, Core.graphics.getWidth()));
		}
		if (bottom) {
			float h = Mathf.clamp(defHeight - y + last.y, minH, Core.graphics.getHeight());
			bind.setHeight(h);
			change.y = defHeight - h;
			bind.y = defY + change.y;
		}
		if (top) {
			bind.setHeight(Mathf.clamp(defHeight + y - last.y, minH, Core.graphics.getHeight()));
		}
		if (listener != null) listener.run();
	}

	public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
		super.touchUp(event, x, y, pointer, button);
		change.set(0, 0);
		scling = false;
		defWidth = defHeight = defX = defY = -1;
		fireElement = null;
		event.cancel();
	}

	public boolean mouseMoved(InputEvent event, float x, float y) {
		if (isDisabled()) return false;

		setCursor(event, x, y);
		return false;
	}
	boolean disabledMove;
	private void setCursor(InputEvent event, float x, float y) {
		boolean valid = valid(x, y);
		if (event.pointer == -1 && valid) {
			Core.graphics.cursor(getCursor());
			disabledMove=false;
		} else if (!valid && !disabledMove) {
			Cursor lastCursor = getLastCursor();
			Core.app.post(() -> {
				if (lastCursor == getLastCursor()) Core.graphics.restoreCursor();
				else disabledMove = true;
			});
		}
	}
	@Override
	public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
		// if (!restoreCursor()) Core.graphics.restoreCursor();
		super.exit(event, x, y, pointer, toActor);
		if (pointer == -1) {
			Core.graphics.restoreCursor();
		}
	}

	Cursor getLastCursor() {
		return Reflect.get(Graphics.class, Core.graphics, "lastCursor");
	}
	Cursor getCursor() {
		return left || right ? SystemCursor.horizontalResize
		 : top || bottom ? SystemCursor.verticalResize
		 : SystemCursor.arrow;
	}
}
