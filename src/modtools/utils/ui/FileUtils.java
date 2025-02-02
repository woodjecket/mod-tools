package modtools.utils.ui;

// import android.content.*;
// import android.net.Uri;
// import android.os.Build.*;
// import android.provider.MediaStore.Files;
// import android.provider.MediaStore.Files.FileColumns;
import arc.Core;
import arc.backend.android.AndroidApplication;
import arc.files.Fi;
import arc.func.Cons;
import modtools.ui.IntUI;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class FileUtils {
	public static void openFiSelector(Cons<List<Fi>> fiCons) {
		new JFrame() {{
			new DropTarget(getContentPane(), DnDConstants.ACTION_COPY_OR_MOVE,
			 new DropTargetAdapter() {
				 public void drop(DropTargetDropEvent event) {
					 try {
						 // 如果拖入的文件格式受支持
						 if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							 // 接收拖拽来的数据
							 event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
							 List<File> list = (List<File>) (event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
							 // 指示拖拽操作已完成
							 event.dropComplete(true);
							 setVisible(false);

							 fiCons.get(list.stream().map(Fi::new).toList());
						 } else {
							 // 拒绝拖拽来的数据
							 event.rejectDrop();
						 }
					 } catch (Exception e) {
						 IntUI.showException(e);
					 }
				 }
			 });

			setSize(300, 300);
			setLocationRelativeTo(null);
			// setDefaultCloseOperation(EXIT_ON_CLOSE);
			setVisible(true);
		}};
	}

	/* public static void shareFile(Fi file) {
		Context app         = ((AndroidApplication) Core.app);
		Intent  shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		Uri uri;
		if (VERSION.SDK_INT >= VERSION_CODES.N) {
			ContentValues values = new ContentValues();
			values.put(FileColumns.DATA, file.readBytes());
			values.put(FileColumns.MIME_TYPE, "text/plain");
			uri = app.getContentResolver().insert(Files.getContentUri("external"), values);
		} else {
			uri = Uri.fromFile(file.file());
		}
		shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
		shareIntent.setType("text/plain");
		shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		app.startActivity(Intent.createChooser(shareIntent, "分享文件"));
	} */
}
