package org.vpns.proxy.core;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import java.io.IOException;
import org.vpns.proxy.nethook.KingCardHeaderHook;

public class LocalVpnService extends VpnService implements Runnable
{
	private ParcelFileDescriptor pfd;
	private static boolean running;
	public static LocalVpnService Instance;
	public static boolean isRunning()
	{
		// TODO: Implement this method
		return running;
	}

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		Instance=this;
		new Thread(this).start();
	}
	
	private void startVpn(){
		try
		{
			pfd = new Builder().addAddress("10.0.0.1", 24).addDisallowedApplication(getPackageName()).setSession(getPackageName()).setMtu(1500).addRoute("0.0.0.0", 0).establish();
			new Thread(){
				public void run(){
					Socket5Proxy proxy=new Socket5Proxy(new KingCardHeaderHook(LocalVpnService.this));
					proxy.start();
					Tun2Socks.runTun2Socks(pfd.getFd(),1500,"10.0.0.2","255.255.255.0","127.0.0.1:"+(proxy.getPort()&0xffff));
					}
					}.start();
		}
		catch (PackageManager.NameNotFoundException e)
		{}
	}
	private void stopVpn(){
		try
		{
			if (pfd != null)
				pfd.close();
		}
		catch (IOException e)
		{}
		Tun2Socks.terminateTun2Socks();
		running=false;
		stopSelf();
	}
	@Override
	public void onDestroy()
	{
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
