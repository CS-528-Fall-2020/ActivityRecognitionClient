package edu.wpi.activityrecognition.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Table Name
    public static final String TABLE_NAME1 = "ACTIVITY_TRACKER";
    public static final String TABLE_NAME2 = "GEO_FENCE";

    // Table columns
    public static final String CREATED_AT1 = "cretedAt";
    public static final String DESC1 = "activityDescription";
    public static final String FENCE_NAME = "name";
    public static final String FENCE_CNT = "visitCount";

    // Database Information
    static final String DB_NAME = "ACTIVITIES_TRACKER.DB";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_TABLE1 = "create table " + TABLE_NAME1 + "(" + CREATED_AT1
            + " TEXT PRIMARY KEY, " + DESC1 + " TEXT NOT NULL);";


    private static final String CREATE_TABLE2 = "create table " + TABLE_NAME2 + "(" + FENCE_NAME
            + " TEXT PRIMARY KEY, " + FENCE_CNT + " INTEGER );";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE1);
        db.execSQL(CREATE_TABLE2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME2);
        onCreate(db);
    }
}
