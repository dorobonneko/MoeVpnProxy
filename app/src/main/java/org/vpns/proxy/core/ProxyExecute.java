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

public class ProxyExecute implements Runnable
{
	private static final byte[] VER = { 0x5, 0x0 };
	private static final byte[] CONNECT_OK = { 0x5, 0x0, 0x0, 0x1, 0, 0, 0, 0, 0, 0 };
	private Socket socket,remote;
	private InputStream input,remoteInput;
	private OutputStream output,remoteOutput;
	private boolean http,ssl;
	public ProxyExecute(Socket socket,boolean ssl)
	{
		this.socket = socket;
		this.ssl=ssl;
	}
	@Override
	public void run()
	{
		byte[] buff=new byte[20000];
		try
		{
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
			boolean tcp=buff[1] == 1;
			if (!tcp)throw new IOException();
			int port=ByteBuffer.wrap(buff, 8, 2).asShortBuffer().get() & 0xffff;
			output.write(CONNECT_OK);
			output.flush();
			for (int i = 4;i <= 9;i++)
			{
				CONNECT_OK[i] = buff[i];
			}
			end = input.read(buff);
			if (end < 0)throw new IOException();
			switch (buff[0])
			{
				case 'G':
				case 'P':
				case 'D':
				case 'H':
				case 'O':
				case 'T':
				case 'C':
					if(!ssl){
					http = true;
					remote = new Socket();
					LocalVpnService.Instance.protect(remote);
					remote.connect(KingCard.getInstance().getHttpProxy());
					break;}
				case 0x16:
				default:
					http = false;
					remote = new Socket();// SslUtil.getSslFactory().createSocket();
					LocalVpnService.Instance.protect(remote);
					remote.setKeepAlive(true);
					remote.connect(KingCard.getInstance().getHttpsProxy());
					break;
					//default:throw new IOException();
			}
			remoteInput = remote.getInputStream();
			remoteOutput = remote.getOutputStream();
			String host=HttpHostHeaderParser.parseHost(buff, 0, end);
			if (host == null)host = findHost(CONNECT_OK[3], CONNECT_OK, 4, CONNECT_OK.length);
			Local2Remote l2r=new Local2Remote(input, remoteInput, remoteOutput, buff, end, host, port);
			l2r.start();
			l2r.join();

		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				output.close();
			}
			catch (IOException e)
			{}
			try
			{
				input.close();
			}
			catch (IOException e)
			{}
			try
			{
				socket.close();
			}
			catch (IOException e)
			{}
			try
			{
				remote.close();
			}
			catch (Exception e)
			{}
			input = null;output = null;remoteInput = null;remoteOutput = null;
			remote = null;
			socket = null;
		}

	}

	public static String findHost(byte type, byte[] bArray, int begin, int end) throws UnknownHostException
	{  
		switch (type)
		{
			case 0x01://ipv4
				{
					byte[] ip=new byte[4];
					System.arraycopy(bArray, begin, ip, 0, ip.length);
					return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x04://ipv6
				{
					byte[] ip=new byte[16];
					System.arraycopy(bArray, begin, ip, 0, ip.length);
					return InetAddress.getByAddress(ip).getHostAddress();
				}
			case 0x03://host
				return new String(bArray, begin + 1, bArray[begin]);
		}
		return null;
	}
	/*public static int findPort(byte[] bArray, int begin, int end)
	 {
	 int port = 0;
	 for (int i = begin;i <= end;i++)
	 {
	 port <<= 8;
	 port |= bArray[begin] & 0xff;
	 }
	 return port;
	 }*/
	class Local2Remote extends Thread
	{
		private InputStream input,remote;
		private OutputStream output;
		private byte[] buff=null;
		private int count,port;
		private String host;
		public Local2Remote(InputStream input, InputStream remote, OutputStream output, byte[] buff, int len, String host, int port)
		{
			this.input = input;
			this.remote = remote;
			this.output = output;
			this.buff = buff;
			this.count = len;
			this.host = host;
			this.port = port;
		}
		@Override
		public void run()
		{

			try
			{
				StringBuilder header=new StringBuilder();
				ByteArrayInputStream bi=new ByteArrayInputStream(buff, 0, count);
				if (!http)
				{
					header.append(String.format("CONNECT %s:%d HTTP/1.1\r\nHost: %s:%d\r\nProxy-Connection: Keep-Alive\r\n", host, port, host, port));
				}
				else
				{
					//first
					String line=null;
					while ((line = readLine(bi).toString()) != null && line.length() > 0)
					{
						if (!line.startsWith("Q-GUID") && !line.startsWith("Q-Token") && !line.startsWith("Proxy-Authorization"))
							header.append(line).append("\r\n");
					}
				}
				header.append("Q-GUID: ".concat(KingCard.getInstance().getQUID()));
				header.append("\r\n");
				header.append("Q-Token: ".concat(KingCard.getInstance().getQTOKEN()));
				header.append("\r\n");
				header.append("\r\n");
				String headers=header.toString();
				output.write(headers.getBytes());
				output.flush();
				if (headers.startsWith("CONNECT"))
				{
					String line=null;
					while ((line = readLine(remote).toString()) != null && line.length() != 0)
					{

					}
				}
				Remote2Local r2l=new Remote2Local(remoteInput, ProxyExecute.this.output);
				r2l.start();
				int len=-1;
				while ((len = bi.read(buff)) != -1)
				{
					output.write(buff, 0, len);
					output.flush();
				}
				//if(count>=buff.length||!http)
				while ((len = input.read(buff)) != -1)
					{
						output.write(buff, 0, len);
						output.flush();
					}
				r2l.join();

			}
			catch (Exception e)
			{}

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
	class Remote2Local extends Thread
	{
		private InputStream input;
		private OutputStream output;
		private byte[] buff;
		public Remote2Local(InputStream input, OutputStream output)
		{
			this.input = input;
			this.output = output;
			this.buff = new byte[10240];
		}

		@Override
		public void run()
		{
			int len=-1;
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
