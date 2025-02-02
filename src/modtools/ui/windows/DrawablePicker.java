package modtools.ui.windows;

import arc.func.Cons;
import arc.graphics.*;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import modtools.ui.*;
import modtools.ui.HopeIcons;
import modtools.ui.components.Window;

import static modtools.ui.HopeStyles.hope_defaultSlider;
import static modtools.ui.windows.ColorPicker.*;

public class DrawablePicker extends Window {
	private Drawable drawable;

	private Cons<Color> cons = c -> {};
	Color current = new Color();
	float h, s, v, a;
	TextField hexField;
	Slider    hSlider, aSlider;

	public DrawablePicker() {
		super("@pickcolor", 0, 0, false, false);

		cont.background(IntUI.whiteui.tint(bgColor));
	}

	public void show(Color color, Cons<Color> consumer) {
		show(color, true, consumer);
	}

	public void show(Color color, boolean alpha, Cons<Color> consumer) {
		this.current.set(color);
		this.cons = consumer;
		show();

		if (hueTex == null) {
			hueTex = Pixmaps.hueTexture(128, 1);
			hueTex.setFilter(TextureFilter.linear);
		}

		float[] values = color.toHsv(new float[3]);
		h = values[0];
		s = values[1];
		v = values[2];
		a = color.a;

		cont.clear();
		cont.pane(newTable(t -> {
			t.add(new Element() {
				public void draw() {
					if (drawable == null) return;
					drawable.draw(x, y, width, height);
				}
			});
			t.add(new Element() {
				 public void draw() {
					 float first  = Tmp.c1.set(current).value(1).saturation(0f).a(parentAlpha).toFloatBits();
					 float second = Tmp.c2.set(current).value(1).saturation(1f).a(parentAlpha).toFloatBits();

					 Fill.quad(
						x, y, Tmp.c1.value(0).toFloatBits(),/* 左下角 */
						x + width, y, Tmp.c2.value(0).toFloatBits(),/* 有下角 */
						x + width, y + height, second,/* 有上角 */
						x, y + height, first/* 左上角 */
					 );

					 Draw.color(Tmp.c1.fromHsv(h, s, v).inv());
					 Icon.cancelSmall.draw(x + s * width, y + v * height,
						5 * Scl.scl(), 5 * Scl.scl());
				 }
			 }).growX().height(100).padBottom(6f).colspan(2)
			 .with(l -> l.addListener(new InputListener() {
				 public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
					 apply(x, y);
					 return true;
				 }
				 public void touchDragged(InputEvent event, float x, float y, int pointer) {
					 apply(x, y);
				 }
				 private void apply(float x, float y) {
					 s = x / l.getWidth();
					 v = y / l.getHeight();
					 updateColor();
				 }
			 }))
			 .row();

			t.defaults().width(140f).height(24f);


			t.add(new Element() {
				public void draw() {
					Draw.color();
					HopeIcons.alphaBgCircle.draw(x, y, width, height);
					float x      = getX(Align.center);
					float y      = getY(Align.center);
					float radius = width / 2;
					float alpha  = a * parentAlpha;

					Draw.color(Tmp.c1.fromHsv(h, s, v).inv(), alpha);
					Fill.circle(x, y, radius);
					Draw.color(Tmp.c1.fromHsv(h, s, v), alpha);
					Fill.circle(x, y, radius - 1);
				}
			}).size(42);

			t.stack(new Image(new TextureRegion(hueTex)), hSlider = new Slider(0f, 360f, 0.3f, false, hope_defaultSlider) {{
				setValue(h);
				moved(value -> {
					h = value;
					updateColor();
				});
			}}).row();

			/* t.stack(new Element() {
				@Override
				public void draw() {
					float first  = Tmp.c1.set(current).saturation(0f).a(parentAlpha).toFloatBits();
					float second = Tmp.c1.set(current).saturation(1f).a(parentAlpha).toFloatBits();

					Fill.quad(
					 x, y, first,
					 x + width, y, second,
					 x + width, y + height, second,
					 x, y + height, first
					);
				}
			}, sSlider = new Slider(0f, 1f, 0.001f, false) {{
				setValue(s);
				moved(value -> {
					s = value;
					updateColor();
				});
			}}).row();

			t.stack(new Element() {
				@Override
				public void draw() {
					float first  = Tmp.c1.set(current).value(0f).a(parentAlpha).toFloatBits();
					float second = Tmp.c1.fromHsv(h, s, 1f).a(parentAlpha).toFloatBits();

					Fill.quad(
					 x, y, first,
					 x + width, y, second,
					 x + width, y + height, second,
					 x, y + height, first
					);
				}
			}, vSlider = new Slider(0f, 1f, 0.001f, false) {{
				setValue(v);
				moved(value -> {
					v = value;
					updateColor();
				});
			}}).row(); */

			hexField = t.field(current.toString().toUpperCase(), value -> {
				try {
					current.set(Color.valueOf(value).a(a));
					current.toHsv(values);
					h = values[0];
					s = values[1];
					v = values[2];
					a = current.a;

					hSlider.setValue(h);
					if (aSlider != null) {
						aSlider.setValue(a);
					}

					updateColor(false);
				} catch (Exception ignored) {
				}
			}).size(130f, 40f).valid(text -> {
				//garbage performance but who cares this runs only every key type anyway
				try {
					Color.valueOf(text);
					return true;
				} catch (Exception e) {
					return false;
				}
			}).get();

			if (alpha) {
				t.stack(new Image(Tex.alphaBgLine), new Element() {
					@Override
					public void draw() {
						float first  = Tmp.c1.set(current).a(0f).toFloatBits();
						float second = Tmp.c1.set(current).a(parentAlpha).toFloatBits();

						Fill.quad(
						 x, y, first,
						 x + width, y, second,
						 x + width, y + height, second,
						 x, y + height, first
						);
					}
				}, aSlider = new Slider(0f, 1f, 0.001f, false, hope_defaultSlider) {{
					setValue(a);
					moved(value -> {
						a = value;
						updateColor();
					});
				}}).row();
			}

		})).grow();

		buttons.clear();
		buttons.margin(6, 8, 6, 8).defaults().growX().height(32);
		buttons.button("@cancel", Icon.cancel, Styles.flatt, this::hide);
		buttons.button("@ok", Icon.ok, Styles.flatt, () -> {
			cons.get(current);
			hide();
		});
	}

	void updateColor() {
		updateColor(true);
	}

	void updateColor(boolean updateField) {
		h = Mathf.clamp(h, 0, 360);
		s = Mathf.clamp(s);
		v = Mathf.clamp(v);
		current.fromHsv(h, s, v);
		current.a = a;

		if (hexField != null && updateField) {
			String val = current.toString().toUpperCase();
			if (current.a >= 0.9999f) {
				val = val.substring(0, 6);
			}
			hexField.setText(val);
		}
	}
	/** a new table that stack() with clip */
	Table newTable(Cons<Table> cons) {
		return new Table(cons) {
			public Cell<Stack> stack(Element... elements) {
				Stack stack = new Stack() {
					protected void drawChildren() {
						clipBegin();
						super.drawChildren();
						clipEnd();
					}
				};
				if (elements != null) {
					for (Element element : elements) stack.addChild(element);
				}
				return add(stack);
			}
		};
	}
}
