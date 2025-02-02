package modtools.ui.content.world;

import arc.Core;
import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.Touchable;
import arc.scene.style.*;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.struct.ObjectMap.Entry;
import arc.util.*;
import arc.util.Timer.Task;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.*;
import mindustry.world.Tile;
import modtools.events.*;
import modtools.ui.*;
import modtools.ui.components.Window.DisWindow;
import modtools.ui.components.input.JSRequest;
import modtools.ui.components.limit.*;
import modtools.ui.components.utils.TemplateTable;
import modtools.ui.content.ui.PositionProv;
import modtools.ui.effect.MyDraw;
import modtools.utils.*;
import modtools.utils.ui.LerpFun;
import modtools.utils.world.WorldDraw;

import java.util.*;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.function.*;

import static mindustry.Vars.tilesize;
import static modtools.ui.Contents.tester;
import static modtools.ui.IntUI.*;
import static modtools.ui.content.world.Selection.tmpList;
import static modtools.utils.world.WorldDraw.CAMERA_RECT;

public abstract class WFunction<T> {
	private static Selection SC;
	static void init(Selection selection) {
		SC = selection;
	}
	public final Table   wrap    = new Table();
	public final Table   main    = new Table();
	public final Table   buttons = new Table();
	public       List<T> list    = new MyVector();


	// for select
	public        Seq<Seq<T>> select      = new Seq<>();
	private final Runnable    changeEvent = () -> MyEvents.fire(this);
	public final  String      name;
	E_Selection data;
	public WorldDraw WD;

	public TemplateTable<Seq<T>> template;

	public ObjectMap<Float, TextureRegion> iconMap = new ObjectMap<>();

	private final ExecutorService                  executor;
	private final ObjectMap<TextureRegion, Seq<T>> selectMap = new ObjectMap<>();
	private       boolean                          drawAll   = true;

	public WFunction(String name, WorldDraw WD) {
		this.name = name;
		data = E_Selection.valueOf(name);
		this.WD = WD;
		Tools.TASKS.add(() -> WD.alpha = SC.selectFunc == this ? 0.7f : 0.1f);
		executor = Threads.boundedExecutor(name + "-each", 1);

		main.button("show all", HopeStyles.blackt, this::showAll).growX().height(Selection.buttonHeight).row();
		main.add(buttons).growX().row();
		buildButtons();

		MyEvents.on(this, () -> {
			template.clear();
			select.clear();
			selectMap.clear();
			Tools.each(list, t -> {
				selectMap.get(getIcon(t), Seq::new).add(t);
			});
			int i = 0;
			for (Entry<TextureRegion, Seq<T>> entry : selectMap) {
				var value = new SeqBind(entry.value);
				selectMap.put(entry.key, value);
				template.bind(value);
				class NewBtn extends Button {
					public NewBtn() {
						super(new ButtonStyle(Styles.flatTogglet));
					}
					static final NinePatchDrawable tinted = ((NinePatchDrawable) Styles.flatDown).tint(Color.pink);

					static {
						tinted.setLeftWidth(0);
						tinted.setRightWidth(0);
						tinted.setTopHeight(0);
						tinted.setBottomHeight(0);
					}

					boolean uiShowing = false;
					void toggleShowing() {
						if (uiShowing) {
							uiShowing = false;
							getStyle().checked = Styles.flatDown;
						} else {
							uiShowing = true;
							getStyle().checked = tinted;
						}
					}
				}
				;
				var btn = new NewBtn();
				btn.update(() -> {
					btn.setChecked(btn.uiShowing || select.contains(value, true));
				});
				IntUI.doubleClick(btn, () -> {
					if (select.contains(value, true)) select.remove(value);
					else select.add(value);
				}, () -> {
					btn.toggleShowing();
					IntUI.showSelectTable(btn, (p, hide, str) -> {
						int c = 0;
						for (T item : value) {
							p.add(new SelectHover(item, t -> {
								t.image(getIcon(item)).size(45);
							}));
							if (++c % 6 == 0) p.row();
						}
					}, false, Align.center).hidden(btn::toggleShowing);
				});
				btn.add(new ItemImage(entry.key, value.size)).grow().pad(6f);
				template.add(btn);
				template.unbind();
				if (++i % 4 == 0) template.newLine();
			}
		});

		template = new TemplateTable<>(null, list -> list.size != 0);
		template.top().defaults().top();
		main.add(template).grow().row();
		template.addAllCheckbox(main);
		wrap.update(() -> {
			if (data.enabled()) {
				setup();
			} else {
				remove();
			}
		});

		Selection.allFunctions.put(name, this);
		main.update(() -> SC.selectFunc = this);

		FunctionBuild("copy", list -> {
			tester.put(Core.input.mouse(), list.toArray());
		});
	}
	private void buildButtons() {
		buttons.defaults().height(Selection.buttonHeight).growX();
		buttons.button("Refresh", Icon.refreshSmall, Styles.flatt, () -> {
			MyEvents.fire(this);
		});
		buttons.button("All", Icon.menuSmall, Styles.flatTogglet, () -> {}).with(b -> b.clicked(() -> {
			 boolean all = select.size != selectMap.size;
			 select.clear();
			 if (all) for (var entry : selectMap) select.add(entry.value);
		 })).update(b -> b.setChecked(select.size == selectMap.size))
		 .row();
		buttons.button("Run", Icon.okSmall, Styles.flatt, () -> {}).with(b -> b.clicked(() -> {
			showMenuList(getMenuLists(this, mergeList()));
		})).disabled(__ -> select.isEmpty());
		buttons.button("Filter", Icon.filtersSmall, Styles.flatt, () -> {
			JSRequest.requestForSelection(mergeList(), null, boolf -> {
				int size = select.sum(seq -> seq.size);
				select.each(seq -> seq.filter((Boolf) boolf));
				showInfoFade("Filtered [accent]" + (size - select.sum(seq -> seq.size)) + "[] elements")
				 .sticky = true;
			});
		}).disabled(__ -> select.size == 0).row();
		buttons.button("DrawAll", Icon.menuSmall, Styles.flatTogglet, () -> {
			drawAll = !drawAll;
		}).update(t -> t.setChecked(drawAll));
		buttons.button("ClearAll", Icon.trash, Styles.flatt, () -> {
			clearList();
			changeEvent.run();
		}).update(t -> t.setChecked(drawAll)).row();
	}

	private List<T> mergeList() {
		Seq<T> seq = new Seq<>();
		select.each(seq::addAll);
		return seq.list();
	}

	public abstract TextureRegion getIcon(T key);
	public abstract TextureRegion getRegion(T t);

	public void setting(Table t) {
		t.check(name, 28, data.enabled(), checked -> {
			if (checked) setup();
			else remove();

			SC.hide();
			data.set(checked);
		}).with(cb -> {
			cb.left();
			cb.setStyle(HopeStyles.hope_defaultCheck);
		});
	}

	public void remove() {
		wrap.clearChildren();
	}

	public float sumf(List<T> list, Floatf<T> summer) {
		float sum = 0;
		for (T t : list) {
			sum += summer.get(t);
		}
		return sum;
	}
	public int sum(List<T> list, Intf<T> summer) {
		int sum = 0;
		for (T t : list) {
			sum += summer.get(t);
		}
		return sum;
	}


	public void each(Consumer<? super T> action) {
		each(list, action);
	}

	public void each(List<T> list, Consumer<? super T> action) {
		if (((ThreadPoolExecutor) executor).getActiveCount() >= 2) {
			IntUI.showException(new RejectedExecutionException("There's already 2 tasks running."));
			return;
		}
		executor.submit(() -> {
			Tools.each(list, t -> {
				new LerpFun(Interp.smooth).onWorld().rev()
				 .registerDispose(1 / 24f, fin -> {
					 Draw.color(Pal.accent);
					 Vec2 pos = getPos(t);
					 Lines.stroke(3f - fin * 2f);
					 TextureRegion region = getRegion(t);
					 Lines.square(pos.x, pos.y,
						fin * Mathf.dst(region.width, region.height) / tilesize);
				 });
				Core.app.post(() -> action.accept(t));
				Threads.sleep(1);
			});
		});
	}
	public void removeIf(List<T> list, Predicate<? super T> action) {
		list.removeIf(action);
	}
	public final void clearList() {
		if (!WD.drawSeq.isEmpty()) WD.drawSeq.clear();
		if (!list.isEmpty()) list.clear();
	}
	public void setup() {
		if (main.parent == wrap) return;
		wrap.add(main).grow();
	}
	public final void showAll() {
		new ShowAllWindow().show();
	}

	public abstract void buildTable(T item, Table table);

	public final void add(T item) {
		Core.app.post(() -> {
			TaskManager.acquireTask(15, changeEvent);
		});
		list.add(item);
		if (SC.drawSelect) {
			// 异步无法创建FrameBuffer
			Core.app.post(() -> Core.app.post(() -> afterAdd(item)));
		}
	}


	public final void afterAdd(T item) {
		TextureRegion region = getRegion(item);
		new BindBoolp(item, () -> {
			if (checkRemove(item)) {
				return false;
			}
			Vec2 pos = getPos(item);
			/* 判断是否在相机内 */
			if (!CAMERA_RECT.overlaps(pos.x, pos.y, region.width, region.height)) return true;
			if (drawAll || select.contains(t -> t.contains(item, true))) {
				Draw.rect(region, pos.x, pos.y, rotation(item));
			}
			return true;
		});
	}
	/** 返回{@code true}如果需要删除 */
	public abstract boolean checkRemove(T item);
	public Vec2 getPos(T item) {
		if (item instanceof Posc) return Tmp.v3.set(((Posc) item).x(), ((Posc) item).y());
		throw new UnsupportedOperationException("You don't overwrite it.");
	}
	public float rotation(T item) {
		return 0;
	}


	protected final ObjectMap<String, Cons<List<T>>> FUNCTIONS = new OrderedMap<>();

	public <R extends UnlockableContent> void ListFunction(
	 String name, Prov<Seq<R>> list,
	 Cons<Table> builder, Cons2<List<T>, R> cons) {
		FunctionBuild(name, from -> {
			var table = IntUI.showSelectImageTableWithFunc(
			 Core.input.mouse().cpy(), list.get(), () -> null,
			 n -> cons.get(from, n), 42f, 32, 6, t -> new TextureRegionDrawable(t.uiIcon), true);
			if (builder != null) builder.get(table);
		});
	}
	/** 这个exec的list是用来枚举的 */
	public void FunctionBuild(String name, Cons<List<T>> exec) {
		// TextButton button = new TextButton(name);
		// cont.add(button).height(buttonHeight).growX().row();

		FUNCTIONS.put(name, exec);
		// button.clicked(() -> {
		// 	clickedBtn = button;
		// 	exec.get(list);
		// });
	}
	public void TeamFunctionBuild(String name, Cons2<List<T>, Team> cons) {
		FunctionBuild(name, from -> {
			Team[]        arr   = Team.baseTeams;
			Seq<Drawable> icons = new Seq<>();

			for (Team team : arr) {
				icons.add(IntUI.whiteui.tint(team.color));
			}

			IntUI.showSelectImageTableWithIcons(Core.input.mouse().cpy(), new Seq<>(arr), icons, () -> null,
			 n -> cons.get(from, n), 42f, 32f, 3, false);
		});
	}


	public boolean onRemoved = false;
	private void onRemoved() {
		if (!onRemoved) Core.app.post(() -> onRemoved = false);
		onRemoved = true;
	}

	public class BindBoolp implements Boolp {
		public T     hashObj;
		public Boolp boolp;
		public BindBoolp(T hashObj, Boolp boolp) {
			this.hashObj = hashObj;
			this.boolp = boolp;
			WD.drawSeq.add(this);
		}
		public boolean get() {
			return (!onRemoved || list.contains(hashObj)) && boolp.get();
		}
		public int hashCode() {
			return hashObj.hashCode();
		}
		public boolean equals(Object obj) {
			return obj == hashObj;
		}
	}
	private class MyVector extends Vector<T> {
		protected void removeRange(int fromIndex, int toIndex) {
			super.removeRange(fromIndex, toIndex);
			onRemoved();
		}
		public boolean removeIf(Predicate<? super T> filter) {
			onRemoved();
			return super.removeIf(filter);
		}
		public boolean remove(Object o) {
			onRemoved();
			return super.remove(o);
		}
		public synchronized T remove(int index) {
			onRemoved();
			return super.remove(index);
		}
	}
	private class SelectHover extends LimitButton {
		public final Task clearFocusWorld = new Task() {
			public void run() {
				if (item instanceof Tile) SC.focusTile = null;
				else if (item instanceof Building) SC.focusBuild = null;
				else if (item instanceof Unit) SC.focusUnits.remove((Unit) item);
				else if (item instanceof Bullet) SC.focusBullets.remove((Bullet) item);
				SC.focusDisabled = false;
			}
		};

		private final T item;

		public SelectHover(T item) {
			super(Styles.flati);
			margin(2, 4, 2, 4);
			this.item = item;

			touchable = Touchable.enabled;

			hovered(() -> {
				if (SC.focusDisabled) return;
				SC.focusElem = this;
				SC.focusElemType = WFunction.this;
				if (item instanceof Tile) SC.focusTile = (Tile) item;
				else if (item instanceof Building) SC.focusBuild = (Building) item;
				else if (item instanceof Unit) SC.focusUnits.add((Unit) item);
				else if (item instanceof Bullet) SC.focusBullets.add((Bullet) item);
				SC.focusDisabled = true;
			});
			exited(() -> {
				SC.focusElem = null;
				SC.focusElemType = null;
				clearFocusWorld.run();
			});

			clicked(() -> {
				WorldInfo.showInfo(this, item);
			});
		}

		public void updateVisibility() {
			super.updateVisibility();
			if (SC.focusDisabled || SC.focusElem == this ||
					(SC.focusTile != item && SC.focusBuild != item
					 && !(item instanceof Unit && SC.focusUnits.contains((Unit) item))
					 && !(item instanceof Bullet && SC.focusBullets.contains((Bullet) item))
					)
			) return;

			SC.focusElem = this;
			SC.focusElemType = WFunction.this;
		}

		public SelectHover(T item, Cons<Table> cons) {
			this(item);
			cons.get(this);
		}
		/* public Element hit(float x, float y, boolean touchable) {
			Element tmp = super.hit(x, y, touchable);
			if (tmp == null) return null;

			focusElem = this;
			return tmp;
		} */

		public void draw() {
			super.draw();
			if (SC.focusElem == this) {
				Draw.color(Pal.accent, Draw.getColor().a);
				Lines.stroke(4f);
				float w = width - 2;
				float h = height - 2;
				MyDraw.dashRect(x + width / 2f, y + height / 2f, w, h,
				 Interp.smooth.apply(0, (w + h) / 2, Time.globalTime / ((w + h) / 2) % 1));
				// Fill.crect(x, y, width, height);
			}
		}

	}
	private class ShowAllWindow extends DisWindow {
		int c, cols = Vars.mobile ? 4 : 6;
		public ShowAllWindow() {
			super(WFunction.this.name, 0, 200, true);
			cont.pane(new LimitTable(table -> {
				for (T item : list) {
					var cont = new SelectHover(item);
					table.add(cont).minWidth(150);
					buildTable(item, cont);
					cont.row();
					cont.button("@details", HopeStyles.blackt, () -> {
						 JSFunc.showInfo(item);
					 }).growX().height(Selection.buttonHeight)
					 .colspan(10);
					if (++c % cols == 0) {
						table.row();
					}
				}
			})).grow();
		}
	}
	private class SeqBind extends Seq<T> {
		final Seq<T> from;
		public SeqBind(Seq<T> from) {
			this.from = from;
			addAll(from);
		}
		public boolean equals(Object object) {
			return this == object && ((SeqBind) object).from == from;
		}
	}


	static <R> Seq<MenuList> getMenuLists(WFunction<R> function, List<R> list) {
		Seq<MenuList> seq = new Seq<>(function.FUNCTIONS.size);
		function.FUNCTIONS.each((k, r) -> {
			seq.add(MenuList.with(null, k, () -> r.get(list)));
		});
		return seq;
	}

	@SuppressWarnings("unchecked")
	static Seq<MenuList> getMenuLists0(ObjectSet<Bullet> bulletSet) {
		tmpList.clear();
		bulletSet.each(tmpList::add);
		return getMenuLists(SC.bullets, tmpList);
	}
	@SuppressWarnings("unchecked")
	static Seq<MenuList> getMenuLists(ObjectSet<Unit> unitSet) {
		tmpList.clear();
		unitSet.each(tmpList::add);
		return getMenuLists(SC.units, tmpList);
	}
	@SuppressWarnings("unchecked")
	static Seq<MenuList> getMenuLists(Building build) {
		tmpList.clear();
		tmpList.add(build);
		return getMenuLists(SC.buildings, tmpList);
	}
	@SuppressWarnings("unchecked")
	static Seq<MenuList> getMenuLists(Tile tile) {
		tmpList.clear();
		tmpList.add(tile);
		return getMenuLists(SC.tiles, tmpList);
	}


	static void buildPos(Table table, Position u) {
		table.label(new PositionProv(() -> Tmp.v1.set(u),
			u instanceof Building || u instanceof Vec2 ? "," : "\n"))
		 .fontScale(0.7f).color(Color.lightGray)
		 .get().act(0.1f);
	}
	<U extends UnlockableContent, E> void sumItems(Seq<U> items, Func<U, E> func, Cons2<U, String> setter) {
		var watcher = JSFunc.watch();
		watcher.addAllCheckbox();
		items.each(i -> {
			if (i.id % 6 == 0) watcher.newLine();
			watcher.watchWithSetter(new TextureRegionDrawable(i.uiIcon),
			 () -> func.get(i),
			 setter == null ? null : str -> setter.get(i, str));
		});
		watcher.show();
	}

}
