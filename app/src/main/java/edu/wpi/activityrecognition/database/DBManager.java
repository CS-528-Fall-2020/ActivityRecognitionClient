package edu.wpi.activityrecognition.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.widget.TextView;

import java.text.SimpleDateFormat;

public class DBManager {
    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insertActivity(String desc) {
        ContentValues contentValue = new ContentValues();
        String date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new java.util.Date());
        contentValue.put(DatabaseHelper.CREATED_AT1, date);
        contentValue.put(DatabaseHelper.DESC1, desc);
        database.insert(DatabaseHelper.TABLE_NAME1, null, contentValue);
    }

    public Cursor fetchActivities() {
        String[] columns = new String[]{DatabaseHelper.CREATED_AT1, DatabaseHelper.DESC1};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME1, columns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchAllFenceCounts() {
        String[] columns = new String[]{DatabaseHelper.FENCE_NAME, DatabaseHelper.FENCE_CNT};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME2, columns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void insertGeoFence(String fenceName, TextView view) {
        ContentValues contentValue = new ContentValues();
        int fenceCount = fetchGeoFenceCount(fenceName);
        if (fenceCount == 0) {
            contentValue.put(DatabaseHelper.FENCE_NAME, fenceName);
            contentValue.put(DatabaseHelper.FENCE_CNT, 1);
            database.insert(DatabaseHelper.TABLE_NAME2, null, contentValue);
        } else {
            contentValue.put(DatabaseHelper.FENCE_CNT, fenceCount++);
            database.update(DatabaseHelper.TABLE_NAME2, contentValue, DatabaseHelper.FENCE_NAME + "=" + fenceName, null);
        }
        view.setText(fenceCount);
    }

    public int fetchGeoFenceCount(String args) {
        String[] columns = new String[]{DatabaseHelper.FENCE_CNT};
        String[] selectionArgs = new String[]{args};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME2, columns, DatabaseHelper.FENCE_NAME, selectionArgs, null, null, null, null);
        int count = 0;
        if (cursor != null) {
            cursor.moveToFirst();
            count = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.FENCE_CNT));
            cursor.close();
        }

        return count;
    }
}
