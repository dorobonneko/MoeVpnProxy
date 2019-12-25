package org.vpns.proxy.tunnel;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.vpns.proxy.nethook.KingCard;
import java.nio.channels.SocketChannel;
import org.vpns.proxy.core.LocalVpnService;
import android.text.TextUtils;
import org.vpns.proxy.util.Stream;

public class Tunnel extends Thread
{
	private byte[] cache;
	private Socket socket;
	private String ip;
	private int port;
	public Tunnel(byte[] cache, String ip, int port, Socket socket)
	{
		this.cache = cache;
		this.socket = socket;
		this.ip = ip;
		this.port = port;
	}
	public void start()
	{
		super.start();
		try
		{
			super.join();
		}
		catch (InterruptedException e)
		{}
	}

	@Override
	public void run()
	{
		Socket remote=null;
		try
		{
			remote = new Socket();
			LocalVpnService.Instance.protect(remote);
			remote.connect(KingCard.getInstance().getHttpsProxy());
			StringBuilder header=new StringBuilder();
			header.append(String.format("CONNECT %s:%d HTTP/1.1\r\nHost: %s:%d\r\nProxy-Connection: Keep-Alive\r\n", ip, port, ip, port));
			header.append("Q-GUID: ".concat(KingCard.getInstance().getQUID()));
			header.append("\r\n");
			header.append("Q-Token: ".concat(KingCard.getInstance().getQTOKEN()));
			header.append("\r\n");
			header.append("\r\n");
			remote.getOutputStream().write(header.toString().getBytes());
			remote.getOutputStream().flush();
			StringBuilder sb=readLine(remote.getInputStream());
			if (TextUtils.isEmpty(sb.toString()))throw new IOException();
			String[] first=sb.toString().split(" ");
			if (first.length < 3)throw new IOException();
			if (!first[1].equals("200"))throw new IOException(sb.toString());
			while (!TextUtils.isEmpty(readLine(remote.getInputStream())));
			remote.getOutputStream().write(cache);
			Stream l2r=new Stream(socket.getInputStream(), remote.getOutputStream(),Long.MAX_VALUE);
			l2r.start();
			Stream r2l=new Stream(remote.getInputStream(),socket.getOutputStream(),Long.MAX_VALUE);
			r2l.start();
			l2r.join();
			r2l.join();
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				if (remote != null)
					remote.close();
			}
			catch (IOException e)
			{}
			try
			{
				if (socket != null)socket.close();
			}
			catch (IOException e)
			{}
		}
	}
	private StringBuilder readLine(InputStream input) throws IOException
	{
		StringBuilder sb=new StringBuilder();
		while (true)
		{
			int v=input.read();
			switch (v)
			{
				case -1:
					return sb;
				case '\r':
					int n=input.read();
					if (n == '\n')
					{
						return sb;
					}
					else if (n == -1)
					{
						return sb;
					}
					else
					{
						sb.append((char)n);
						continue;
					}
				default:sb.append((char)v); 
			}
		}
	}

	
}
