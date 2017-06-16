package net.trafficstats;

import java.util.Map;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import net.db.DBManager;
import noah.mg.netman.R;

public class MainActivity extends Activity {
	private TextView txtView;
	Map<String, TrafficInfo> list = null;
	private TrafficService trafficService;
	private DBManager dbManager;
	boolean flag=true;
	String str_view="";

	Handler mHandler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what==0x012){
				str_view="";
				if(list!=null&&list.size()>0)
				for (TrafficInfo info : list.values()) {
					str_view+=info.appName + " - 流量信息:\r\n"+
							"移动网络接收的流量"
							+ Formatter.formatFileSize(MainActivity.this, info.mobileRx)+"\r\n"+
							"移动网络发送的流量"
							+ Formatter.formatFileSize(MainActivity.this, info.mobileTx)+"\r\n"+
							"WIFI接收的流量"
							+ Formatter.formatFileSize(MainActivity.this, info.wifiRx)+"\r\n"+
							"WIFI发送的流量"
							+ Formatter.formatFileSize(MainActivity.this, info.wifiTx)+"\r\n"+
							"--------------------\r\n";
					txtView.setText(str_view);
				}
			}
			super.handleMessage(msg);
		}
	};
	//ServiceConnection资料：
	//http://book.51cto.com/art/201211/363287.htm
	//http://blog.sina.com.cn/s/blog_92facc5e0101164a.html
	//http://www.iteye.com/topic/729291
	private ServiceConnection mConnection = new ServiceConnection() {

		//类ServiceConnection中的onServiceDisconnected()方法在正常情况下是不被调用的，
		//它的调用时机是当Service服务被意外销毁时，例如内存的资源不足时这个方法才被自动调用。
		//翻译：connect连接 ，Disconnected不连接
		@Override
		public void onServiceDisconnected(ComponentName name) {
			trafficService = null;
		}

		//Service中需要创建一个实现Binder的内部类(这个内部类不一定在Service中实现，但必须在Service中创建它)。
		//service的onBind()中必须返回的IBinder
		//系统调用onServiceConnected（）这个来传送在service的onBind()中返回的IBinder。
		//onServiceConnected作用：当Service服务连接时调此方法。
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			TrafficService.MyBinder trafficService_myBinder = (TrafficService.MyBinder) service;
			trafficService = trafficService_myBinder.getService();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Init();
	}

	/**
	 * 开启线程读取流量
	 */
	private void Init(){
		txtView = (TextView) findViewById(R.id.textView1);
		txtView.setMovementMethod(ScrollingMovementMethod.getInstance());
		/**
		 Service的几种启动方式
		 1、startService 启动的服务：主要用于启动一个服务执行后台任务，不进行通信。停止服务使用stopService；
		 2、bindService 启动的服务：该方法启动的服务可以进行通信。停止服务使用unbindService；
		 3、startService 同时也 bindService 启动的服务：停止服务应同时使用stepService与unbindService
		 */
		Intent intent = new Intent(MainActivity.this, TrafficService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);//bindService 启动的服务
		dbManager = new DBManager(this);

		//准备SQLiteOpenHelper
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(flag){
					if (trafficService == null) {
						txtView.setText("服务未绑定");
					} else {
						trafficService.logRecord();
						list = dbManager.queryTotal();
						Message message=Message.obtain();
						message.what=0x012;
						mHandler.sendMessage(message);
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}


}
