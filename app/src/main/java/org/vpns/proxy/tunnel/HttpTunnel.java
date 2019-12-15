package org.vpns.proxy.tunnel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import org.vpns.proxy.tcpip.IpHeader;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import org.vpns.proxy.core.HttpHostHeaderParser;
import java.net.InetSocketAddress;
import org.vpns.proxy.core.LocalVpnService;

public class HttpTunnel extends Tunnel
{
	private SocketChannel remote,local;
	private Tunnel raw;
	@Override
	public void onWriteAble(SelectionKey key) throws IOException
	{
		SocketChannel local=(SocketChannel) key.channel();
		ByteBuffer buff=getByteBuffer();
		buff.position(0);
			remote.write(buff);
			buff.clear();
			while(local.read(buff)>0){
				buff.flip();
				remote.write(buff);
				buff.clear();
			}
		remote.register(getSelector(),SelectionKey.OP_READ,raw);
			
	}

	@Override
	public void onReadable(SelectionKey key) throws IOException
	{
		local=(SocketChannel) key.channel();
		ByteBuffer buff=getByteBuffer();
		buff.clear();
		int size=local.read(buff);
		if(size<=0)return;
		buff.flip();
		remote=SocketChannel.open();
		raw=new RawTunnel(local,getSelector(),null);
		
		remote.configureBlocking(false);
		String ip=getIpHeader().getIp();
		int port=getIpHeader().getPort()&0xffff;
		if(port==80||port==443){
		LocalVpnService.Instance.protect(remote.socket());
		boolean conn=remote.connect(new InetSocketAddress(ip,port));
		remote.register(getSelector(),SelectionKey.OP_CONNECT,this);
		}
	}

	@Override
	public void onConnectable(SelectionKey key) throws IOException
	{
		SocketChannel remote=(SocketChannel) key.channel();
		while(!remote.finishConnect());
		ByteBuffer buff=getByteBuffer();
		buff.position(0);
		StringBuilder sb=new StringBuilder();
		if((getIpHeader().getPort()&0xffff)==80){
			toString();
		}
		//if(HttpHostHeaderParser.canParse(buff.get())){
			buff.position(0);
			while(buff.hasRemaining())
			remote.write(buff);
			sb.append(new String(buff.array(),0,buff.limit()));
			buff.clear();
			while(local.read(buff)>0){
				buff.flip();
				while(buff.hasRemaining())
					remote.write(buff);
				buff.clear();
			}
			remote.register(getSelector(),SelectionKey.OP_READ,raw);
		/*}else{
			String ip=getIpHeader().getIp();
			int port=getIpHeader().getPort()&0xffff;
			byte[] request = String.format("CONNECT %s:%d HTTP/1.0\r\nHost: %s\r\nProxy-Connection: keep-alive\r\n\r\n",ip,port,ip).getBytes();
			ByteBuffer request_buff=ByteBuffer.wrap(request);
			remote.write(request_buff);
			request_buff.clear();
			remote.register(getSelector(),SelectionKey.OP_WRITE,raw);
			
		}*/
		
	}
	

	

	public HttpTunnel(Selector selector,IpHeader ipHeader){
		super(selector,ipHeader);
	}

	
}
