package org.vpns.proxy.tcpip;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IpHeader
{
	public static final int VER=0;//socket版本
	public static final int CMD=1;//命令码(1 tcp,2 bind,3 udp)
	public static final int ATYP=3;//地址类型(1 ipv4,3 host,4 ipv6)
	public static final int HOST_LEN=4;//host长度
	private byte[] data;
	public IpHeader(byte[] data){
		this.data=data;
	}
	public byte get(int offset){
		return data[offset];
	}
	public String getIp(){
		switch(get(ATYP)){
			case 0x01:
				return String.format("%s.%s.%s.%s",data[4]&0xff,data[5]&0xff,data[6]&0xff,data[7]&0xff);
			case 0x04:
				byte[] ip=new byte[16];
				System.arraycopy(data,4,ip,0,ip.length);
				try
				{
					return InetAddress.getByAddress(ip).getHostAddress();
				}
				catch (UnknownHostException e)
				{}
			case 0x03:
				return new String(data,HOST_LEN+1,get(HOST_LEN));
		}
		return null;
	}
	public short getPort(){
		int offset=0;
		switch(get(ATYP)){
			case 0x03:
				offset=get(HOST_LEN)+HOST_LEN;
				break;
			case 0x01:
				offset=ATYP+4;
				break;
			case 0x04:
				offset=ATYP+16;
				break;
				}
		return ByteBuffer.wrap(data,offset+1,offset+3).asShortBuffer().get();
	}
}
