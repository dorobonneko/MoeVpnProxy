package org.vpns.proxy.core;
import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import android.net.VpnService;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.vpns.proxy.util.Arrays;
import javax.net.ssl.SSLSocketFactory;
import java.net.SocketImplFactory;
import org.vpns.proxy.nethook.KingCard;
import org.vpns.proxy.util.SslUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.net.SocketException;
import org.vpns.proxy.tunnel.Tunnel;
import org.vpns.proxy.tunnel.HttpTunnel;
import java.io.ByteArrayOutputStream;

public class ProxyExecute implements Runnable
{
	private static final byte[] VER = { 0x5, 0x0 };
	private static final byte[] CONNECT_OK = { 0x5, 0x0, 0x0, 0x1, 0, 0, 0, 0, 0, 0 };
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private boolean ssl;
	public ProxyExecute(Socket socket,boolean ssl)
	{
		this.socket = socket;
		this.ssl=ssl;
	}
	@Override
	public void run()
	{
		try
		{
			socket.setKeepAlive(true);
			input = socket.getInputStream();
			output = socket.getOutputStream();
			/**
			 * 获取认证方法并通过认证 默认为无身份认证
			 * 接受的三个参数分别是
			 * ver:socket版本(5)--1字节
			 * nmethods:在下一个参数的方法数 --1字节
			 * methods:方法 --1至255字节
			 *  X’00’ NO AUTHENTICATION REQUIRED				无身份验证
			 X’01’ GSSAPI									未知
			 X’02’ USERNAME/PASSWORD							用户名/密码
			 X’03’ to X’7F’ IANA ASSIGNED					保留位
			 X’80’ to X’FE’ RESERVED FOR PRIVATE METHODS		私有位
			 X’FF’ NO ACCEPTABLE METHODS                 	没有可用方法
			 */
			if(input.read()==5){
				input.read(new byte[2]);
			}else{
				throw new IOException();
			}//0x05 0x01 0x00
			output.write(VER);
			output.flush();
			//认证
			if(input.read()!=5)
				throw new IOException("no socket5");
			int cmd=input.read();
			input.skip(1);//ipheader
			/*0 ver:socket版本(5) 
			 *1 cmd:sock命令码(1 tcp,2 bind,3 udp) 
			 *2 rsv:保留字段
			 *3 atyp:地址类型(ipv4 1,域名 3,ipv6 4)
			 *4 如果是域名，长度
			 */
			 String host=findHost(input);
			int port=ByteBuffer.wrap(new byte[]{(byte)(input.read()&0xff),(byte)(input.read()&0xff)}).asShortBuffer().get() & 0xffff;
			output.write(CONNECT_OK);
			output.flush();
			if(!ssl&&cmd==1){
				int type=input.read();
				ByteArrayOutputStream cache=new ByteArrayOutputStream();
				cache.write(type);
			switch(type){
				case 'G':read(input,cache,2);break;
				case 'P':read(input,cache,2);break;
				case 'D':read(input,cache,5);break;
				case 'H':read(input,cache,3);break;
				case 'O':read(input,cache,6);break;
				case 'T':read(input,cache,4);break;
				//case 'C':
				case 0x16:
				case 'C':
					default:
					Tunnel t=new Tunnel(cache.toByteArray(),host,port,socket);
					t.run();
					return;
			}
			switch(cache.toString()){
				case "GET":
				case "PUT":
				case "POS":
				case "DELETE":
				case "OPTIONS":
				case "TRACK":
					HttpTunnel ht=new HttpTunnel(cache.toByteArray(),socket);
					ht.run();
					break;
				default:
					Tunnel t=new Tunnel(cache.toByteArray(),host,port,socket);
					t.run();
				break;
			}
			cache.close();
			}else{
			Tunnel t=new Tunnel(new byte[0],host,port,socket);
			t.run();
			}
			
		}
		catch (Exception e)
		{
			
			try
			{
				socket.close();
			}
			catch (IOException ee)
			{}
		}
		finally
		{
			
		}

	}
	private void read(InputStream i,OutputStream o,int size) throws IOException{
		byte[] buff=new byte[size];
		int len=-1,read=0;
		while((len=i.read(buff,read,size))!=-1){
			o.write(buff);
			read+=len;
			if(read>=size)
				break;
		}
	}
	public static String findHost(InputStream input) throws IOException
	{  
		switch (input.read())
		{
			case 0x01://ipv4
				{
					byte[] ip=new byte[4];
					input.read(ip);
					return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x04://ipv6
				{
					byte[] ip=new byte[16];
					input.read(ip);
					return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x03://host
			byte[] host=new byte[input.read()];
			input.read(host);
				return new String(host);
		}
		return null;
	}
	
}
