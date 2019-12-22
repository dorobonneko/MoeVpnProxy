package org.vpns.proxy.core;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.nio.channels.ServerSocketChannel;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.nio.channels.SocketChannel;
import org.vpns.proxy.tunnel.Tunnel;
import org.vpns.proxy.tunnel.TunnelFactory;
import java.nio.ByteBuffer;
import org.vpns.proxy.tcpip.TcpIp;
import org.vpns.proxy.tcpip.IpHeader;

public class Socket5Proxy extends Thread
{
	private ServerSocket ss;
	private ExecutorService service;
	private boolean global_ssl;
	public Socket5Proxy(boolean ssl){
		global_ssl=ssl;
		service=Executors.newFixedThreadPool(255);
	}

	public void globalSsl(boolean p0)
	{
		global_ssl=p0;
		LocalVpnService.write("Global_Ssl:".concat(String.valueOf(p0)));
	}
	@Override
	public void run()
	{
		try
		{
			ss = new ServerSocket(1080);
			while(true){
				service.submit(new ProxyExecute(ss.accept(),global_ssl));
				if(isInterrupted())
					throw new IOException();
			}
		}
		catch (IOException e)
		{
			try
			{
				ss.close();
			}
			catch (Exception ee)
			{}
		}
	}
	
}
