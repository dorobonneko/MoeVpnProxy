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
	private HeaderHook hh;
	private short port;
	private Selector mSelector;
	private ServerSocketChannel mServerSocketChannel;
	public Socket5Proxy(HeaderHook hh)
	{
		this.hh = hh;
		try
		{
			mServerSocketChannel = ServerSocketChannel.open();
			mServerSocketChannel.configureBlocking(false);
			mServerSocketChannel.bind(new InetSocketAddress(0));
			mServerSocketChannel.register(mSelector = Selector.open(), SelectionKey.OP_ACCEPT);
			port = (short)mServerSocketChannel.socket().getLocalPort();
		}
		catch (IOException e)
		{}
	}
	public short getPort()
	{
		return port;
	}
	@Override
	public void run()
	{
		
			while (true)
			{
				try{
				int keys=mSelector.select();
				if (keys <= 0)
					continue;
				Iterator<SelectionKey> iterator=mSelector.selectedKeys().iterator();
				while (iterator.hasNext())
				{
					SelectionKey key=iterator.next();
					iterator.remove();
					if (key.isValid())
					{
						if (key.isAcceptable())
						{
							onAcceptable(key);
						}
						else if (key.isConnectable())
						{

							((Tunnel)key.attachment()).onConnectable(key);
						}
						else if (key.isReadable())
						{
							Object attach=key.attachment();
							if (attach instanceof IpHeader)
							{
								TunnelFactory.wrap(mSelector, (IpHeader)attach).onReadable(key);
							}
							else
							{
								((Tunnel)key.attachment()).onReadable(key);
							}
						}
						else if (key.isWritable())
						{

							((Tunnel)key.attachment()).onWriteAble(key);
						}
					}
				}
		}
		catch (IOException e)
		{}
		}
	}
	void onAcceptable(SelectionKey key) throws IOException
	{
		SocketChannel localChannel=mServerSocketChannel.accept();
		ByteBuffer bb=ByteBuffer.allocateDirect(3);
		localChannel.read(bb);
		bb.flip();
		bb.put(new byte[]{0x05,0x00});
		bb.flip();
		bb.limit(2);
		localChannel.write(bb);
		bb = ByteBuffer.allocate(20);
		localChannel.read(bb);
		//ip
		IpHeader ih=TcpIp.parser(bb.array());
		bb.flip();
		bb.position(0);
		bb.put(new byte[]{0x05,0x00,0x00,0x01});
		bb.position(0);
		localChannel.write(bb);
		localChannel.configureBlocking(false);
		localChannel.register(mSelector, SelectionKey.OP_READ, ih);
		/*Tunnel tunnel=TunnelFactory.wrap(localChannel,key.selector());
		 tunnel.connect(new InetSocketAddress("210.22.247.196",8091));*/
	}
}
