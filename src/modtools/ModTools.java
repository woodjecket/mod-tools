package modtools;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import mindustry.mod.ModClassLoader;
import mindustry.ui.dialogs.BaseDialog;
import modtools.ui.Background;
import modtools_lib.MyReflect;
import rhino.Context;

import java.util.Objects;

import static mindustry.Vars.ui;
import static modtools.IntVars.modName;
import static modtools.utils.MySettings.settings;

public class ModTools extends Mod {
	public static ModClassLoader mainLoader;
	public static boolean init = false;

	public ModTools() {
		Log.info("Loaded ModTools constructor.");
		Time.runTask(0, () -> {
			Log.info("Loaded Reflect.");
			loadReflect();
		});

		Events.on(ClientLoadEvent.class, e -> {
			if (throwable != null) {
				ui.showException(throwable);
				return;
			}

			if (!Vars.mobile) try {
				Context context = Context.getCurrentContext();
				MyReflect.setValue(context != null ? context : Context.enter(), Context.class, "applicationClassLoader", Vars.mods.mainLoader());
			} catch (Throwable err) {
				throw new RuntimeException(err);
			}
			// Unit135G.main();
			Time.runTask(10f, () -> {
				BaseDialog dialog = new BaseDialog("frog");
				dialog.addCloseListener();

				Table cont = dialog.cont;
				cont.image(Core.atlas.find(modName + "-frog")).pad(20f).row();
				cont.add("behold").row();
				Objects.requireNonNull(dialog);
				cont.button("I see", dialog::hide).size(100f, 50f);
				dialog.show();
			});
			IntVars.load();

			if (settings.getBool(modName + "-ShowMainMenuBackground")) Background.main();
		});
	}

	public static Throwable throwable = null;

	public static void loadReflect() {
		if (init) return;
		init = true;
		// 加载反射
		try {
			mainLoader = (ModClassLoader) Vars.mods.mainLoader();
			Fi sourceFi = Vars.mods.getMod("mod-tools").root
					.child("libs").child("lib.jar");
			Log.info("load source fi: " + sourceFi);
			Fi toFi = Vars.dataDirectory.child("tmp/mod-tools-lib.jar");
			if (toFi.exists()) {
				if (toFi.isDirectory()) {
					toFi.deleteDirectory();
				} else {
					toFi.delete();
				}
			}
			sourceFi.copyTo(toFi);
			ClassLoader loader = Vars.platform.loadJar(toFi, mainLoader);
			mainLoader.addChild(loader);
			Class.forName("modtools_lib.MyReflect", true, loader);
			toFi.delete();
			MyReflect.load();

			/*var urlloader = new URLClassLoader(new URL[]{Vars.tmpDirectory.child("mindustry.jar").file().toURI().toURL()}, null);
			for (Field f : MyReflect.lookupGetFields(ClassLoader.class)) {
				if (f.getName().equals("parent")) {
					MyReflect.setOverride(f);
					f.set(mdtcl, urlloader);
					break;
				}
			}*/
			//			urlloader.loadClass("mindustry.ctype.Content", true);
			//		mdtcl.addChild(loader);
			//			mdtcl.loadClass("mindustry.ctype.Content");
			//			Class.forName("mindustry.ctype.Content", true, mainLoader);
		} catch (Throwable e) {
			throwable = e;
			Log.err(e);
		}

	}

	/*public static void test() {
		new Thread(() -> Core.app.exit());
		unsafe.park(false, (long) 1E9);
		try {
			Method m = Application.class.getDeclaredMethod("getListeners");
			m.setAccessible(true);
			Seq<ApplicationListener> listeners = (Seq<ApplicationListener>) m.invoke(Core.app);
			listeners.remove((ApplicationListener) Vars.platform);
			Field field = SdlApplication.class.getDeclaredField("running");
			field.setAccessible(true);
			m = SdlApplication.class.getDeclaredMethod("loop");
			m.setAccessible(true);
			while (field.getBoolean(Core.app)) {
				m.invoke(Core.app);
			}
			Log.info("finish");
		} catch (Throwable e) {
			Log.err(e);
		}
	}*/
}
