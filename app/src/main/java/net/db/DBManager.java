package net.db;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.trafficstats.TrafficInfo;

public class DBManager {

	private MySQLiteOpenHelper dbHelper;
	private SQLiteDatabase wDb;
	private SQLiteDatabase rDb;

	public DBManager(Context context) {
		//ContextWrapper用于支持对存储在SD卡上的数据库的访问
		dbHelper = new MySQLiteOpenHelper(new MyContextWrapper(context));
		//使用SQLiteOpenHelper类获取读和写的SQLiteDatabase类，可以参考斗狗项目FighterDog游戏。
		wDb = dbHelper.getWritableDatabase();//此步执行时会调MyContextWrapper的openOrCreateDatabase方法和getDatabasePath方法
		rDb = dbHelper.getReadableDatabase();
	}

	public void close() {
		wDb.close();
		rDb.close();
		dbHelper.close();
	}

	public void insertStart(String pacakgeName, String appName, long startTime,
			String networkType, long rx, long tx) {
		//写入的SQLiteDatabase类具体需要ContentValues，同斗狗项目FighterDog
		ContentValues value = new ContentValues();
		value.put(DbConstants.COLUMN_PACKAGE_NAME, pacakgeName);
		value.put(DbConstants.COLUMN_APP_NAME, appName);
		value.put(DbConstants.COLUMN_START_TIME, startTime);
		value.put(DbConstants.COLUMN_NETWORK_TYPE, networkType);
		value.put(DbConstants.COLUMN_RX, rx);
		value.put(DbConstants.COLUMN_TX, tx);
		wDb.insert(DbConstants.TABLE_NAME_TRAFFIC, null, value);
	}

	public void updateEnd(String pacakgeName, long endTime, long rx, long tx) {
		
		/*
		SQLiteDatabase.query(String table, String[] columns, String selection, String[] selectionArgs,
		 									String groupBy, String having, String orderBy, String limit)
		table:表名，不能为null。columns:要查询的列名，可以是多个，可以为null，表示查询所有列。
		selection:查询条件，比如id=? and name=? 可以为null。selectionArgs:对查询条件赋值，一个问号对应一个值，按顺序可以为null。
		having:语法have，可以为null。orderBy：语法，按xx排序，可以为null。
		 */
		// 查询最新的一条记录
		/*SELECT * FROM traffic_record WHERE package_name = 'com.adups.fota' and end_time is null 
		                                                     ORDER BY start_time desc LIMIT 1 */  
		Cursor start = rDb.query(DbConstants.TABLE_NAME_TRAFFIC, null,
				DbConstants.COLUMN_PACKAGE_NAME + " = '" + pacakgeName
						+ "' and " + DbConstants.COLUMN_END_TIME + " is null",
				null, null, null, DbConstants.COLUMN_START_TIME + " desc", "1");
		//start.moveToFirst()==false则查询结果为空。
		if (!start.moveToFirst()) {
			return;
		}
		ContentValues value = new ContentValues();
		value.put(DbConstants.COLUMN_END_TIME, endTime);
		value.put(
				DbConstants.COLUMN_RX,rx - start.getLong(start.getColumnIndexOrThrow(DbConstants.COLUMN_RX)));
		value.put(
				DbConstants.COLUMN_TX,tx - start.getLong(start.getColumnIndexOrThrow(DbConstants.COLUMN_TX)));
		wDb.update(
				DbConstants.TABLE_NAME_TRAFFIC,value,DbConstants.COLUMN_ID
														+ "="
														+ start.getInt(start
																.getColumnIndexOrThrow(DbConstants.COLUMN_ID)),
				null);
		start.close();
	}

	public Map<String, TrafficInfo> queryTotal() {
		Cursor c = rDb.rawQuery("select " + DbConstants.COLUMN_PACKAGE_NAME
				+ "," + DbConstants.COLUMN_APP_NAME + ","
				+ DbConstants.COLUMN_NETWORK_TYPE + ",sum("
				+ DbConstants.COLUMN_RX + "),sum(" + DbConstants.COLUMN_TX
				+ ") from " + DbConstants.TABLE_NAME_TRAFFIC + " where "
				+ DbConstants.COLUMN_END_TIME + " is not null group by "
				+ DbConstants.COLUMN_PACKAGE_NAME + ","
				+ DbConstants.COLUMN_APP_NAME + ","
				+ DbConstants.COLUMN_NETWORK_TYPE, null);
		Map<String, TrafficInfo> map = new HashMap<String, TrafficInfo>();
		while (c.moveToNext()) {
			String packageName = c.getString(0);
			TrafficInfo item = null;
			if (!map.containsKey(packageName)) {
				item = new TrafficInfo();
				item.packageName = packageName;
				item.appName = c.getString(1);
				map.put(packageName, item);
			} else {
				item = map.get(packageName);
			}
			String networkType = c.getString(2);
			if (networkType.equals(DbConstants.NETWORK_TYPE_MOBILE)) {
				item.mobileRx = c.getLong(3);
				item.mobileTx = c.getLong(4);
			} else if (networkType.equals(DbConstants.NETWORK_TYPE_WIFI)) {
				item.wifiRx = c.getLong(3);
				item.wifiTx = c.getLong(4);
			}
		}
		return map;
	}
}
