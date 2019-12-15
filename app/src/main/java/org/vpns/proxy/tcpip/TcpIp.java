package org.vpns.proxy.tcpip;

public class TcpIp
{
	public static IpHeader parser(byte[] data){
		return new IpHeader(data);
	}
}
