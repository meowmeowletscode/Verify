package com.globalsion.verify.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExportFunc {

    private DBHelper mydb;

    public void exportCSV(final Context ct) throws IOException {
        mydb = new DBHelper(ct);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        final String currentDateandTime = sdf.format(new Date());

        final File folder = new File(ct.getExternalFilesDir(null), "Verify");

        boolean var = false;
        if (!folder.exists())
            var = folder.mkdir();

        System.out.println("Directory created: " + var);

        final String filename = folder + "/" + "Verify_" + currentDateandTime + ".csv";

        // Show waiting screen
        final ProgressDialog progDailog = ProgressDialog.show(
                ct, "Verify", "Export Data in Excel......",
                true); // please wait

        final Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                UIHelper.ToastMessage(ct, "File was Exported in device/Android/data/com.globalsion.verify/files/Verify/ Folder");
            }
        };

        new Thread() {
            public void run() {
                try {
                    Cursor cursor = mydb.getAllRow();
                    if (cursor != null && cursor.getCount() > 0) {
                        FileWriter fw = new FileWriter(filename);

                        fw.append("ID");
                        fw.append(',');

                        fw.append("Company");
                        fw.append(',');

                        fw.append("Product");
                        fw.append(',');

                        fw.append("Code");
                        fw.append(',');

                        fw.append("Status");

                        fw.append('\n');

                        if (cursor.moveToFirst()) {
                            do {
                                fw.append(cursor.getString(0));
                                fw.append(',');

                                fw.append(cursor.getString(1));
                                fw.append(',');

                                fw.append(cursor.getString(2));
                                fw.append(',');

                                fw.append(cursor.getString(3));
                                fw.append(',');

                                fw.append(cursor.getString(4));
                                fw.append('\n');

                            } while (cursor.moveToNext());
                        }

                        if (cursor != null && !cursor.isClosed()) {
                            cursor.close();
                        }

                        fw.close();
                        mydb.delete();
                    }
                } catch (final Exception e) {
                    // Show error message on the main thread
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            UIHelper.ToastMessage(ct, e.getMessage());
                        }
                    });
                } finally {
                    // Notify on success and dismiss the ProgressDialog on the main thread
                    handler.sendEmptyMessage(0);
                    progDailog.dismiss();
                }
            }
        }.start();
    }
}
