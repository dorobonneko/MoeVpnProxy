package org.vpns.proxy.tunnel;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import org.vpns.proxy.core.LocalVpnService;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import org.vpns.proxy.tcpip.IpHeader;

public abstract class Tunnel
{
	private Selector mSelector;
	private ByteBuffer buff=ByteBuffer.allocate(20000);
	private IpHeader mIpHeader;
	public Tunnel(Selector selector,IpHeader ipHeader){
		this.mSelector=selector;
		this.mIpHeader=ipHeader;

	}
	public IpHeader getIpHeader(){
		return mIpHeader;
	}
	public Selector getSelector(){
		return mSelector;
	}
	public ByteBuffer getByteBuffer(){
		return buff;
	}
	public abstract void onWriteAble(SelectionKey key) throws IOException;
	public abstract void onReadable(SelectionKey key) throws IOException;
	public abstract void onConnectable(SelectionKey key) throws IOException;
	
}
