package org.vpns.proxy.core;
import android.net.VpnService;
import java.net.Socket;
import java.net.SocketAddress;

public abstract class HeaderHook
{
	private VpnService vs;
	public HeaderHook(VpnService vs){
		this.vs=vs;
	}
	public void protect(Socket socket){
		vs.protect(socket);
	}
	public SocketAddress getSocketAddress(SocketAddress address){
		//返回一个新的地址用于代理
		return address;
	}
	public abstract void onHeader(StringBuilder header);
}
