package modtools;

import arc.Events;
import arc.files.Fi;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods.*;
import modtools.ui.IntUI;
import modtools.utils.array.MySet;

import java.util.concurrent.*;

import static mindustry.Vars.ui;

public class IntVars {
	public static final String  modName = "mod-tools";
	public static       ModMeta meta;
	public static       Fi      root;

	public static String QQ = "https://qm.qq.com/q/7rAZZaEMs&personal_qrcode_source=4";
	public static ModClassLoader mainLoader = (ModClassLoader) Vars.mods.mainLoader();

	public static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Threads.boundedExecutor("hope-async", 1);

	public static final String  NL = System.lineSeparator();
	public static       boolean hasDecompiler;


	public static void showException(Exception e, boolean b) {
		if (b) {
			IntUI.showException(e);
		} else {
			Log.err(e);
		}
	}
	public static void async(Runnable runnable) {
		async(runnable, null);
	}

	public static void async(Runnable runnable, Runnable callback) {
		async(null, runnable, callback, false);
	}
	public static void async(String text, Runnable runnable, Runnable callback) {
		async(text, runnable, callback, ui != null);
	}
	public static void async(String text, Runnable runnable, Runnable callback, boolean displayUI) {
		if (displayUI) ui.loadfrag.show(text);
		CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> {
			try {
				runnable.run();
			} catch (Exception err) {
				showException(err, displayUI);
			}
			if (displayUI) ui.loadfrag.hide();
			if (callback != null) callback.run();
			return 1;
		});
		try {
			completableFuture.get();
		} catch (Exception e) {
			showException(e, displayUI);
		}
	}

	public static final MySet<Runnable> resizeListeners = new MySet<>();
	public static void addResizeListener(Runnable runnable) {
		resizeListeners.add(runnable);
	}

	/** @return {@code true} if the file be deleted successfully */
	public static boolean delete(Fi fi) {
		return fi.exists() && (fi.isDirectory() ? fi.deleteDirectory() : fi.delete());
	}

	static {
		// new Not("aaa");
		Events.on(ResizeEvent.class, __ -> {
			for (var r : resizeListeners) r.run();
		});
	}
}
