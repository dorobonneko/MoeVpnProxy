package org.vpns.proxy.nethook;
import java.net.InetSocketAddress;
import java.util.TimerTask;
import java.util.Timer;
import android.net.Uri;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import javax.net.ssl.HttpsURLConnection;
import org.vpns.proxy.util.SslUtil;
import java.io.InputStream;
import android.text.TextUtils;
import org.vpns.proxy.core.LocalVpnService;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

public class KingCard extends TimerTask
{
	private static KingCard kc;
	private String uid="",token="";
	private boolean finish;
	public static String Api="https://gitee.com/r0x/WK/raw/master/wk.htm";
	private KingCard(Context context)
	{
	}

	public void refresh()
	{
		new Thread(this).start();
	}
	public static void init(Context context){
		kc=new KingCard(context);
	}
	public static KingCard getInstance()
	{
		return kc;
	}
	public static void stop(){
		if(kc!=null)kc.finish=true;
		kc=null;
	}
	public InetSocketAddress getHttpProxy()
	{
		return new InetSocketAddress("157.255.173.185", 8090);
	}
	public InetSocketAddress getHttpsProxy()
	{
		return new InetSocketAddress("210.22.247.196", 8091);
	}
	public String getQUID()
	{
		return uid;
	}
	public String getQTOKEN()
	{
		return token;
	}
	public Uri getApi()
	{
		return Uri.parse(KingCard.Api);
	}

	@Override
	public void run()
	{
		HttpURLConnection huc=null;
				InputStream input=null;
				try
				{
					huc = (HttpURLConnection) new URL(getApi().toString()).openConnection();
					if (huc instanceof HttpsURLConnection)
					{
						((HttpsURLConnection)huc).setSSLSocketFactory(SslUtil.getSslFactory());
					}
					if (!TextUtils.isEmpty(getQUID()))
					{
						huc.setRequestProperty("Q_GUID", uid);
						huc.setRequestProperty("Q_Token", token);
					}
					input = huc.getInputStream();
					byte[] buff=new byte[129];
					input.read(buff);
					String[] data=new String(buff).split(",");
					uid = data[0];
					token = data[1];
					LocalVpnService.write(new SimpleDateFormat("hh:mm:ss").format(new Date()));
					LocalVpnService.write("Q_GUID:".concat(data[0]));
					LocalVpnService.write("Q_Token:".concat(data[1]));
				}
				catch (IOException e)
				{
					LocalVpnService.write("Token: null");
					if (!TextUtils.isEmpty(getQUID()))
					{
						uid = "";
						
						}
				}
				finally
				{
					try
					{
						input.close();
					}
					catch (Exception e)
					{}
					huc.disconnect();
					}
	}
		
	

}
