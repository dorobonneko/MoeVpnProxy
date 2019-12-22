package org.vpns.proxy.tunnel;
import java.nio.ByteBuffer;
import org.vpns.proxy.tcpip.IpHeader;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.io.IOException;

public class RawTunnel extends Tunnel
{
	
	private SocketChannel local;
	private boolean readed;
	@Override
	public void onWriteAble(SelectionKey key) throws IOException
	{
		//隧道已建立
		/*SocketChannel remote=(SocketChannel) key.channel();
		ByteBuffer buff=getByteBuffer();
		buff.clear();
		remote.read(buff);
		buff.flip();
		String s=new String(buff.array(),0,buff.limit());*/
	}

	@Override
	public void onReadable(SelectionKey key) throws IOException
	{
		ByteBuffer buff=getByteBuffer();
		buff.clear();
		SocketChannel socket=(SocketChannel) key.channel();
		int len=-1;
		if((len=socket.read(buff))>0){
			readed=true;
			buff.flip();
			if(buff.array()[0]=='H'){
			String s=new String(buff.array(),0,buff.limit());
			}
			while(buff.hasRemaining())
			this.local.write(buff);
			buff.clear();
			key.interestOps(SelectionKey.OP_READ);
		}else if(readed){
		socket.shutdownOutput();
		socket.shutdownInput();
		socket.close();
		local.shutdownInput();
		local.shutdownOutput();
		this.local.close();
		key.cancel();
		}else{
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	@Override
	public void onConnectable(SelectionKey key) throws IOException
	{
		// TODO: Implement this method
	}
	

	

	public RawTunnel(SocketChannel socket,Selector selector,IpHeader ipHeader){
		super(selector,ipHeader);
		this.local=socket;
	}
}
