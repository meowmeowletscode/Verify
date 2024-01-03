package com.globalsion.verify.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class DBHelper {

    public static final String Tablename = "verify";
    public static final String id = "_id";
    public static final String Company = "Company";
    public static final String Product = "Product";
    public static final String Code = "Code";
    public static final String Status = "Status";

    private SQLiteDatabase db;
    private Database dbHelper;

    private Context context;

    private static final int VERSION = 1;
    private static final String DB_NAME = "Verify.db";

    public DBHelper(Context context) {
        this.context = context;
        dbHelper = new Database(context);
    }

    public SQLiteDatabase getReadableDatabaseInstance() {
        return dbHelper.getReadableDatabase();
    }



    public void open() {
        if (null == db || !db.isOpen()) {
            try {
                db = dbHelper.getWritableDatabase();
            } catch (SQLiteException sqLiteException) {
                Log.e("Error", sqLiteException.getMessage());
            }
        }
    }

    public void close() {
        if (db != null) {
            db.close();
        }
    }

    public int getStatus(String code) {
        try {
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            String selectQuery = "SELECT " + Status + " FROM " + Tablename + " WHERE " + Code + " = ? LIMIT 1";
            Cursor cursor = database.rawQuery(selectQuery, new String[]{code});

            int status = 0; // Default status is 0 (unchecked)

            if (cursor.moveToFirst()) {
                status = cursor.getInt(cursor.getColumnIndex(Status));
            }

            cursor.close();
            return status;
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            return 0;
        }
    }

    public boolean setCheck(String code) {
        try {
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            String selection = Code + "=?";
            String[] selectionArgs = {code};

            // Perform the query to check if a record with the given code exists
            Cursor cursor = database.query(Tablename, new String[]{id}, selection, selectionArgs, null, null, null);

            boolean recordExists = cursor.moveToFirst();

            cursor.close();

            return recordExists;
        } catch (Exception e) {
            Log.e("Error", e.getMessage(), e);
            return false; // Return false in case of an exception
        }
    }

    public int insert(String table, ContentValues values) {
        try {
            db = dbHelper.getWritableDatabase();
            int y = (int) db.insert(table, null, values);
            db.close();
            Log.e("Data Inserted", "Data Inserted");
            Log.e("y", y + "");
            return y;
        } catch (SQLiteConstraintException e) {
            Log.e("Error: Duplicate entry. The code already exists.", e.getMessage());

            return 0;
        } catch (Exception ex) {
            Log.e("Error Insert", ex.getMessage());

            return 0;
        }
    }

    public void delete() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("Verify", "1", null);
    }


    public Cursor getAllRow() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+ Tablename, null);
        return res;
    }

    public int updateStatus(String code, int newStatus) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(Status, newStatus);

            int rowsAffected = db.update(Tablename, values, Code + " = ?", new String[]{code});

            db.close();
            return rowsAffected;
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            return 0;
        }
    }

    public ArrayList<HashMap<String, String>> getProducts() {
        ArrayList<HashMap<String, String>> prolist;
        prolist = new ArrayList<>();
        String selectQuery = "SELECT Company, Product, Code, Status FROM " + Tablename;
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(Company, cursor.getString(0));
                map.put(Product, cursor.getString(1));
                map.put(Code, cursor.getString(2));

                boolean isChecked = cursor.getInt(cursor.getColumnIndex(Status)) == 1;
                map.put(Status, String.valueOf(isChecked));

                prolist.add(map);
            } while (cursor.moveToNext());
        }
        return prolist;
    }

    private class Database extends SQLiteOpenHelper {

        public Database(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            String create_sql = "CREATE TABLE IF NOT EXISTS " + Tablename + "("
                    + id + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Company + " TEXT ,"
                    + Product + " TEXT ,"
                    + Code + " TEXT ,"
                    + Status + " INTEGER DEFAULT 0, "
                    + "UNIQUE(" + Code + ")" + ")";
            db.execSQL(create_sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + Tablename);
        }
    }
}