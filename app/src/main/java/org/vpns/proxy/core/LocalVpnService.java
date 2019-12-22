package org.vpns.proxy.core;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import java.io.IOException;
import android.content.Intent;
import com.moe.vpnproxy.util.Preference;
import android.content.SharedPreferences;
import org.vpns.proxy.nethook.KingCard;

public class LocalVpnService extends VpnService implements Runnable,SharedPreferences.OnSharedPreferenceChangeListener
{
	private ParcelFileDescriptor pfd;
	private static boolean running;
	public static LocalVpnService Instance;
	private Thread tun2socks;
	private Socket5Proxy proxy;
	private boolean globalSsl;
	public static boolean isRunning()
	{
		// TODO: Implement this method
		return running;
	}
	public static void write(String msg){
		if(Instance==null)return;
		Intent intent=new Intent(Instance.getPackageName().concat(".Write"));
		intent.putExtra(intent.getAction(),msg);
		Instance.sendBroadcast(intent);
	}
	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		Instance=this;
		globalSsl=Preference.is(this,"ssl",true);
		Preference.register(this,this);
		new Thread(this).start();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String p2)
	{
		switch(p2){
			case "ssl":
				if(proxy!=null)
					proxy.globalSsl(p1.getBoolean(p2,true));
				break;
		}
	}

	
	private void startVpn(){
		try
		{
			pfd = new Builder().addAddress("10.0.0.1", 24).addDisallowedApplication(getPackageName()).setSession(getPackageName()).setMtu(1500).addRoute("0.0.0.0", 0).establish();
			(tun2socks= new Thread(){
				 public void run(){
					proxy=new Socket5Proxy(globalSsl);
					proxy.start();
					Tun2Socks.runTun2Socks(pfd.getFd(),1500,"26.26.26.2","255.255.255.0","127.0.0.1:1080");
					}
					}).start();
		write("VPN:Start");
		}
		catch (PackageManager.NameNotFoundException e)
		{}
	}
	public void stopVpn(){
		if(!running)return;
		proxy.interrupt();
		try{
			Tun2Socks.terminateTun2Socks();
			tun2socks.join();
			}catch(Exception e){}
		try
		{
			if (pfd != null)
				pfd.close();
		}
		catch (Exception e)
		{}
		running=false;
		write("VPN:Stop");
		Instance=null;
		Preference.unregister(this,this);
		KingCard.stop();
		stopSelf();
	}
	@Override
	public void onDestroy()
	{
		if(running)
		stopVpn();
		super.onDestroy();
	}
	
	private void waitPrepared(){
		while(VpnService.prepare(this)!=null){
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{}
		}
	}

	@Override
	public void run()
	{
		synchronized(this){
			waitPrepared();
			startVpn();
			running=true;
		}
	}

	
}
