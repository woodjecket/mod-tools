package modtools.ui.content.debug;

import arc.Core;
import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import modtools.IntVars;
import modtools.ui.components.*;
import modtools.ui.components.input.MyLabel;
import modtools.ui.components.limit.LimitTable;
import modtools.ui.content.Content;
import modtools.utils.Tools;

public class ErrorDisplay extends Content {
	public ErrorDisplay() {
		super("errordisplay");
	}
	{
		defLoadable = false;
	}

	Window ui;
	Table  crashes;

	public void load() {
		crashes = new LimitTable(p -> {
			p.defaults().grow();
			IntVars.async(() -> {
				Seq<Fi> list = new Seq<>(Vars.dataDirectory.child("crashes").list()).reverse();
				p.left().defaults().left();
				for (var fi : list) {
					var label = new MyLabel("");
					TextButton button = p.button(fi.nameWithoutExtension(), new TextButtonStyle(Styles.logicTogglet), () -> {
						if (label.getText().length() == 0) {
							label.setText(fi.readString());
						}
					}).size(Core.graphics.isPortrait() ? 450 : 650, 45).get();
					button.getStyle().up = Tex.underline;
					p.row();
					p.collapser(new Table(Tex.pane, cont -> cont.left().add(label).growX().wrap().left()), true, button::isChecked).growX().row();
				}
			}, () -> {});
		});
	}

	public void rebuild() {
		float w = getW();
		ui = new Window(localizedName(), w, 90, true);
		ui.update(() -> ui.minWidth = getW());

		Color[]      colors       = {Color.sky, Color.gold};
		Fi           last_log     = Vars.dataDirectory.child("last_log.txt");
		long[]       lastModified = {last_log.lastModified()};
		Prov<String> getString    = () -> last_log.exists() ? last_log.readString() : "";
		String[]     text         = {getString.get()};
		Table[] tables = {new LimitTable(t -> t.pane(p -> p.label(() -> {
			if (last_log.exists() && Tools.EQSET(lastModified, last_log.lastModified())) {
				text[0] = getString.get();
			}
			return text[0];
		}))), crashes};
		String[] names = {"last_log", "crashes"};
		IntTab   tab   = new IntTab(-1, names, colors, tables);
		tab.setPrefSize(w, -1);
		ui.cont.add(tab.build()).grow();

		// ui.addCloseButton();
	}

	private static int getW() {
		return Core.graphics.isPortrait() ? 400 : 600;
	}

	public void build() {
		if (ui == null) rebuild();
		else ui.toFront();
		ui.show();
	}
}
