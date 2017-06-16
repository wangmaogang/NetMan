package net.db;

import android.os.Environment;

public class DbConstants {
	public static final String DB_PATH;
	public static final String DB_NAME = "traffic.db";
	public static final int DB_VERSION = 1;

	public static final String NETWORK_TYPE_MOBILE = "移动网络";
	public static final String NETWORK_TYPE_WIFI = "WIFI";

	// 已用流量表（包名，应用名，开始时间，结束时间，网络类型，接收流量，发送流量）
	public static final StringBuffer CREATE_TRAFFIC_TABLE_SQL = new StringBuffer();
	public static final StringBuffer DELETE_TRAFFIC_TABLE_SQL = new StringBuffer();
	public static final String TABLE_NAME_TRAFFIC = "traffic_record";
	public static final String COLUMN_ID = android.provider.BaseColumns._ID;
	public static final String COLUMN_PACKAGE_NAME = "package_name";//包名字段
	public static final String COLUMN_APP_NAME = "app_name";//应用名字段
	public static final String COLUMN_START_TIME = "start_time";//开始时间字段
	public static final String COLUMN_END_TIME = "end_time";//结束时间字段
	public static final String COLUMN_NETWORK_TYPE = "network_type";//网络类型字段
	public static final String COLUMN_RX = "rx";//接收流量字段
	public static final String COLUMN_TX = "tx";//发送流量字段

	static {
		DB_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
		// 消费记录表的建表sql语句
		CREATE_TRAFFIC_TABLE_SQL.append("create table if not exists ")
				.append(TABLE_NAME_TRAFFIC).append(" (");
		CREATE_TRAFFIC_TABLE_SQL.append(COLUMN_ID).append(
				" integer primary key autoincrement");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_PACKAGE_NAME)
				.append(" text not null");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_APP_NAME)
				.append(" text not null");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_START_TIME)
				.append(" long not null");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_END_TIME)
				.append(" long ");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_NETWORK_TYPE)
				.append(" text not null");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_RX)
				.append(" long not null");
		CREATE_TRAFFIC_TABLE_SQL.append(",").append(COLUMN_TX)
				.append(" long not null");
		CREATE_TRAFFIC_TABLE_SQL.append(");");
		// 消费记录表的删表sql语句
		DELETE_TRAFFIC_TABLE_SQL.append("drop table if exists ").append(
				TABLE_NAME_TRAFFIC);
	}
}
