package org.vpns.proxy.core;
import java.util.concurrent.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Socket5Proxy extends Thread
{
	private ServerSocket ss;
	private ExecutorService service;
	private boolean global_ssl;
	public Socket5Proxy(boolean ssl){
		global_ssl=ssl;
		service=Executors.newCachedThreadPool();
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
				Socket socket=ss.accept();
				if(socket!=null)
				service.submit(new ProxyExecute(socket,global_ssl));
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
			service.shutdown();
		}
	}
	
}
