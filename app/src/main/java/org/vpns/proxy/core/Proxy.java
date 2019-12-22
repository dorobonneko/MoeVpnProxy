package org.vpns.proxy.core;
import java.net.Socket;

public class Proxy implements Runnable
{
	private Socket socket;
	public Proxy(Socket socket){
		this.socket=socket;
	}
	@Override
	public void run()
	{
		// TODO: Implement this method
	}
	
}
