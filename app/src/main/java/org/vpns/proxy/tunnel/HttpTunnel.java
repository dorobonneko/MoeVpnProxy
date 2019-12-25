package org.vpns.proxy.tunnel;
import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;
import android.text.TextUtils;
import org.vpns.proxy.nethook.KingCard;
import org.vpns.proxy.core.LocalVpnService;
import org.vpns.proxy.util.Stream;
import org.vpns.proxy.net.HttpResponse;

public class HttpTunnel extends Thread
{
	private StringBuilder header=null;
	private Socket socket;
	public HttpTunnel(int type,Socket socket){
		header=new StringBuilder();
		header.append((char)type);
		this.socket=socket;
	}

	@Override
	public void start()
	{
		// TODO: Implement this method
		super.start();
		
	}

	@Override
	public void run()
	{
		Socket remote=null;
		try
		{
			remote=new Socket();
			remote.setKeepAlive(false);
			LocalVpnService.Instance.protect(remote);
			remote.connect(KingCard.getInstance().getHttpProxy());
			//获取头长度再处理
			HttpResponse r2l=new HttpResponse(remote.getInputStream(),socket.getOutputStream());
			r2l.start();
			long content_length=0l;
			StringBuilder headers=new StringBuilder();
			while(!TextUtils.isEmpty(readLine(socket.getInputStream()))){
				String line=header.toString().trim();
				if (!line.startsWith("Q-GUID") && !line.startsWith("Q-Token") && !line.startsWith("Proxy-Authorization")){
				headers.append(header).append("\r\n");
				}
				if(line.startsWith("Content-Length:"))
				content_length=Long.parseLong(line.substring(15).trim());
				header.setLength(0);
			}
			headers.append("Q-GUID: ".concat(KingCard.getInstance().getQUID()));
			headers.append("\r\n");
			headers.append("Q-Token: ".concat(KingCard.getInstance().getQTOKEN()));
			headers.append("\r\n\r\n");
			//headers.append("Proxy-Connection: Keep-Alive\r\n\r\n");
			
			remote.getOutputStream().write(headers.toString().getBytes());
			remote.getOutputStream().flush();
			
			if(content_length>0){
			Stream l2r=new Stream(socket.getInputStream(),remote.getOutputStream(),content_length);
			l2r.start();
			
			l2r.join();
			}
			r2l.join();
		}
		catch (Exception e)
		{}finally{
			try
			{
				if (remote != null){
					remote.close();
					}
			}
			catch (IOException e)
			{}
			try
			{
				if (socket != null){
					socket.close();
					}
			}
			catch (IOException e)
			{}
		}
	}
	
	private StringBuilder readLine(InputStream input) throws IOException
	{
		while (true)
		{
			int v=input.read();
			switch (v)
			{
				case -1:
					return header;
				case '\r':
					int n=input.read();
					if (n == '\n')
					{
						return header;
					}
					else if (n == -1)
					{
						return header;
					}
					else
					{
						header.append((char)n);
						continue;
					}
				default:header.append((char)v); 
			}
		}
	}
	
}
