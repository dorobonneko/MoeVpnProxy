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

public class ProxyExecute implements Runnable
{
	private static final byte[] VER = { 0x5, 0x0 };
	private static final byte[] CONNECT_OK = { 0x5, 0x0, 0x0, 0x1, 0, 0, 0, 0, 0, 0 };
	private Socket socket,remote;
	private InputStream input,remoteInput;
	private OutputStream output,remoteOutput;
	private HeaderHook hh;
	public ProxyExecute(Socket socket,HeaderHook hh)
	{
		this.socket = socket;
		this.hh=hh;
		
	}
	@Override
	public void run()
	{
		try
		{
			byte[] buff=new byte[1024];
			input=socket.getInputStream();
			output=socket.getOutputStream();
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
			input.read(buff);//0x05 0x01 0x00
			output.write(VER);
			output.flush();
			//认证
			
			int end=input.read(buff);//ipheader
			/*0 ver:socket版本(5) 
				*1 cmd:sock命令码(1 tcp,2 bind,3 udp) 
				*2 rsv:保留字段
				*3 atyp:地址类型(ipv4 1,域名 3,ipv6 4)
				*4 如果是域名，长度
				*/
			boolean tcp=buff[1]==1;
			if(!tcp)return;
			String host=findHost(buff[3],buff,4,end);
			int port=ByteBuffer.wrap(buff,8,2).asShortBuffer().get()&0xffff;
			for (int i = 4;i <= 9;i++) {
				CONNECT_OK[i] = buff[i];
				}
			output.write(CONNECT_OK);
			output.flush();
			
			/*if(true)
			return;
			sb = new StringBuilder();
			StringBuilder line=new StringBuilder();
			char value=0;
			char old=0;
			while ((value = (char)input.read()) != -1)
			{
				if (value == '\r' || value == '\n')
				{
					if (old == '\r' && value == '\n')
						continue;
					old = value;
					if (line.length() == 0)
					{
						break;
					}
					//makeLine(line);
					String hex=Arrays.bytes2Hex(line.toString().getBytes()).toString();
					line.setLength(0);
					sb.append("\r\n");
					continue;
				}
				old = value;
				sb.append(value);
				line.append(value);
				}
			hh.onHeader(sb);//处理请求头*/
			//连接外部网络
			remote=new Socket();
			//hh.protect(remote);
			remote.connect(hh.getSocketAddress(new InetSocketAddress(host,port)));
			remoteInput=remote.getInputStream();
			remoteOutput=remote.getOutputStream();
			Local2Remote l2r=new Local2Remote(input,remoteOutput);
			l2r.start();
			Remote2Local r2l=new Remote2Local(remoteInput,output);
			r2l.start();
			l2r.join();
			r2l.join();
			remote.close();
			socket.close();
		}
		catch (Exception e)
		{}
		
	}
	
	public static String findHost(byte type,byte[] bArray, int begin,int end) throws UnknownHostException
	{  
		switch(type){
			case 0x01://ipv4
			{
				byte[] ip=new byte[4];
				System.arraycopy(bArray,begin,ip,0,ip.length);
				return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x04://ipv6
			{
			byte[] ip=new byte[16];
				System.arraycopy(bArray,begin,ip,0,ip.length);
			return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x03://host
			return new String(bArray,begin+1,bArray[begin]);
		}
		return null;
	}
	public static int findPort(byte[] bArray, int begin, int end)
	{
		int port = 0;
		for (int i = begin;i <= end;i++)
		{
			port<<=8;
			port|=bArray[begin]&0xff;
			}
		return port;
	}
	class Local2Remote extends Thread
	{
		private InputStream input;
		private OutputStream output;
		public Local2Remote(InputStream input,OutputStream output){
			this.input=input;
			this.output=output;
		}
		@Override
		public void run()
		{
			byte[] buff=new byte[1024];
			try
			{
				int len=input.read(buff);
				if(socket.getLocalPort()==1080){
					String s=new String(buff);
				}
				
				output.write(buff,0,len);
				while((len=input.read(buff))!=-1){
					output.write(buff,0,len);
				}
				output.flush();
				/*if ((buff[0] != 71 || buff[1] != 69 || buff[3] != 32) && (buff[0] != 80 || buff[1] != 79 || buff[4] != 32))
				{
					
				}
				else if (buff[0] != 67 || buff[1] != 79 || buff[7] != 32)
				{
					toString();
				}
				else
				{
					toString();
				}
				String s=new String(buff);*/
			}
			catch (IOException e)
			{}
		}
		
	}
	class Remote2Local extends Thread{
		private InputStream input;
		private OutputStream output;
		public Remote2Local(InputStream input,OutputStream output){
			this.input=input;
			this.output=output;
		}

		@Override
		public void run()
		{
			int len=-1;
			byte[] buff=new byte[10240];
			try
			{
				while ((len = input.read(buff)) != -1)
				{
					output.write(buff, 0, len);
				}
				output.flush();
			}
			catch (IOException e)
			{}
		}
		
	}
}
