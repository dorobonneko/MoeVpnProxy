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

public class KingCard extends TimerTask
{
	private static KingCard kc;
	private String uid="",token="";
	private boolean finish;
	private Timer mTimer=new Timer();
	public static String Api="https://gitee.com/r0x/WK/raw/master/wk.htm";
	private KingCard()
	{
		mTimer.schedule(this, 0, 90 * 60 * 1000);
		while (!finish)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{}
		}
	}
	public static KingCard getInstance()
	{
		if (kc == null)
		{
			synchronized (KingCard.class)
			{
				if (kc == null)kc = new KingCard();
			}
		}
		return kc;
	}
	public static void stop(){
		if(kc!=null){
			kc.mTimer.cancel();
			kc=null;
		}
	}
	public InetSocketAddress getHttpProxy()
	{
		return new InetSocketAddress("210.22.247.196", 8090);
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
			LocalVpnService.write("Q_GUID:".concat(data[0]));
			LocalVpnService.write("Q_Token:".concat(data[1]));
		}
		catch (IOException e)
		{
			LocalVpnService.write("Token: null");
			if (!TextUtils.isEmpty(getQUID()))
			{
				uid = "";
				run();}
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
			finish = true;
		}
	}


}
