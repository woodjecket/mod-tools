package modmake.ui;

import arc.struct.Seq;
import modmake.ui.Content.*;

public class Contents {
	public static final Seq<Content> all = new Seq<>();
	public static final Settings settings = new Settings();
	public static Tester tester = new Tester();
	public static Selection selection = new Selection();
	public static ShowUIList showuilist = new ShowUIList();
}