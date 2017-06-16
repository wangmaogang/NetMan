package net.trafficstats;

import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.db.DBManager;
import net.db.DbConstants;

/*
 * 问题点：
 * 1.如果服务被强制关闭，则不能统计
 * 2.开机自动启动服务
 * 3.如果要做某天的流量统计，则需要定时在每晚十二点前进行一次已用流量统计
 * 4.数据库相关操作比较耗时，要考虑异步
 * 
 */
public class TrafficService extends Service {
	
	private static final String TAG = TrafficService.class.getSimpleName();
	List<PackageInfo> packinfos;
	private TrafficBroadcastReceiver trafficBroadcastReceiver;
	private ConnectivityManager connManager;
	private DBManager dbManager;

	private MyBinder binder = new MyBinder();
	//http://www.linuxidc.com/Linux/2012-07/66195.htm
	//http://blog.csdn.net/coding_glacier/article/details/7520199
	//http://blog.csdn.net/luoshengyang/article/details/6618363
	//Binder:包扎物,不知道作用
	public class MyBinder extends Binder {
		TrafficService getService() {
			return TrafficService.this;
		}
	}
	//当前服务执行完onCreate()方法的dbManager = new DBManager(this);代码，就调此步。
	//return binder后报异常，进到TrafficBroadcastReceiver的onreceive方法，但此时还没注册广播，不知道为什么。
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		return binder;
	}

	public void onCreate() {
		Log.d(TAG, "onCreate");
		// 获得数据库连接服务
		dbManager = new DBManager(this);
		// 获得网络连接服务,简历项目NewResume_android_client的判断WIFI网络是否可用用过此代码
		connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		//获取手机上全部应用
		//getPackageManager()获得已安装的应用程序信息 。
		packinfos = getPackageManager().getInstalledPackages(
				PackageManager.GET_UNINSTALLED_PACKAGES
						| PackageManager.GET_PERMISSIONS);
		//注册BroadcastReceiver，监听打开或者关闭移动网络。CONNECTIVITY：连通性。
		trafficBroadcastReceiver = new TrafficBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		// filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(trafficBroadcastReceiver, filter);
		super.onCreate();
	}

	//onUnbind();这种情况下只是解除了Activity与Service之间的绑定,Service仍然执行
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		unregisterReceiver(trafficBroadcastReceiver);
		logRecord();
		dbManager.close();
		super.onDestroy();
	}

	
	private int iii = 0;
	public void logRecord() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
				String networkType = null;
				if (networkInfo == null) {
					networkType = null;
				} else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					networkType = DbConstants.NETWORK_TYPE_WIFI;
				} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					networkType = DbConstants.NETWORK_TYPE_MOBILE;
				}

				for (PackageInfo info : packinfos) {
					iii++;
					if(packinfos.size() == iii){
						System.out.println("iii");
					}
					//获取具体某个应用的全部权限
					String[] premissions = info.requestedPermissions;
					if (premissions != null && premissions.length > 0) {
						for (String premission : premissions) {
							//判断某个应用的权限中是否有网络权限
							if ("android.permission.INTERNET".equals(premission)) {
								//获取有网络权限的应用的参数  uid:10003  rx:2482  tx:2342
								int uid = info.applicationInfo.uid;
								//Android在2.2版中新加入了TrafficStats用于统计流量
								long rx = TrafficStats.getUidRxBytes(uid);//某一个进程的总接收量
								long tx = TrafficStats.getUidTxBytes(uid);//某一个进程的总发送量

								//操作SQLiteOpenHelper数据库
								dbManager.updateEnd(info.packageName,
										System.currentTimeMillis(), rx, tx);
								dbManager
										.insertStart(
												info.packageName,
												//getPackageManager():获得已安装的应用程序信息 。
												//info.applicationInfo.loadLabel(getPackageManager()).toString():获取已安装应用名
												info.applicationInfo.loadLabel(
														getPackageManager()).toString(),
												System.currentTimeMillis(),
												networkType, rx, tx);
							}
						}
					}
				}
			}
		});
	}

	private class TrafficBroadcastReceiver extends BroadcastReceiver {
		private final String TAG = TrafficBroadcastReceiver.class.getSimpleName();

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "网络状态改变");
			logRecord();
		}

	}

}
