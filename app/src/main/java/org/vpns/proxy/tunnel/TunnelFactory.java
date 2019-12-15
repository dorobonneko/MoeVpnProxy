package org.vpns.proxy.tunnel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import org.vpns.proxy.tcpip.IpHeader;

public class TunnelFactory
{
	public static Tunnel wrap(Selector selector,IpHeader ipHeader){
		return new HttpTunnel(selector,ipHeader);
	}
}
