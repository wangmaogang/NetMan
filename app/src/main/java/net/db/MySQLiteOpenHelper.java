package net.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

	public MySQLiteOpenHelper(Context context) {
		super(context, DbConstants.DB_NAME, null, DbConstants.DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			db.execSQL(DbConstants.CREATE_TRAFFIC_TABLE_SQL.toString());
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.beginTransaction();
		try {
			db.execSQL(DbConstants.DELETE_TRAFFIC_TABLE_SQL.toString());
			db.execSQL(DbConstants.CREATE_TRAFFIC_TABLE_SQL.toString());
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

}
