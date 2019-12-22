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
	private ByteBuffer bb=ByteBuffer.allocate(1024);

	@Override
	public void onWriteAble(SelectionKey key) throws IOException
	{
		bb.clear();
		if (remote.read(bb) > 0)
		{
			if(bb.array()[0]!=72){
				String s=new String(bb.array(),0,bb.position());
			}
			SocketChannel local=(SocketChannel) key.channel();
			ByteBuffer buff=getByteBuffer();
			buff.position(0);
			remote.write(buff);
			buff.clear();
			while (local.read(buff) > 0)
			{
				buff.flip();
				remote.write(buff);
				buff.clear();
			}
			remote.register(getSelector(), SelectionKey.OP_READ, raw);
			key.cancel();
		}
		else
		{
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	@Override
	public void onReadable(SelectionKey key) throws IOException
	{
		local = (SocketChannel) key.channel();
		ByteBuffer buff=getByteBuffer();
		buff.clear();
		int size=local.read(buff);
		if (size <= 0)return;
		buff.flip();
		buff.mark();
		//判断http还是https
		switch (HttpHostHeaderParser.parse(buff.get()))
		{
			case HttpHostHeaderParser.HTTP:
				remote = SocketChannel.open();
				raw = new RawTunnel(local, getSelector(), null);
				remote.configureBlocking(false);
				String ip=getIpHeader().getIp();
				int port=getIpHeader().getPort() & 0xffff;
				LocalVpnService.Instance.protect(remote.socket());
				remote.connect(new InetSocketAddress(ip, port));
				remote.register(getSelector(), SelectionKey.OP_CONNECT, this);
				break;
			case HttpHostHeaderParser.SSL:
			case HttpHostHeaderParser.HTTPS:
				remote = SocketChannel.open();
				raw = new RawTunnel(local, getSelector(), null);

				remote.configureBlocking(false);
				LocalVpnService.Instance.protect(remote.socket());
				remote.connect(new InetSocketAddress("101.71.140.5", 8128));
				remote.register(getSelector(), SelectionKey.OP_CONNECT, this);
				break;
			default:
				key.cancel();
				local.close();
				return;
		}
		buff.reset();
	}

	@Override
	public void onConnectable(SelectionKey key) throws IOException
	{
		SocketChannel remote=(SocketChannel) key.channel();
		while (!remote.finishConnect());
		ByteBuffer buff=getByteBuffer();
		buff.position(0);
		switch (HttpHostHeaderParser.parse(buff.get()))
		{
			case HttpHostHeaderParser.HTTP:
				StringBuilder sb=new StringBuilder();
				buff.position(0);
				while (buff.hasRemaining())
					remote.write(buff);
				sb.append(new String(buff.array(), 0, buff.limit()));
				buff.clear();
				while (local.read(buff) > 0)
				{
					buff.flip();
					while (buff.hasRemaining())
						remote.write(buff);
					buff.clear();
				}
				remote.register(getSelector(), SelectionKey.OP_READ, raw);
				break;
			case HttpHostHeaderParser.HTTPS:
				break;
			case HttpHostHeaderParser.SSL:
				String ip=HttpHostHeaderParser.parseHost(buff.array(), 0, buff.limit());
				if (ip == null)ip = getIpHeader().getIp();
				int port=getIpHeader().getPort() & 0xffff;
				byte[] request = String.format("CONNECT %s:%d HTTP/1.1\r\nHost: %s\r\nProxy-Connection: keep-alive\r\nProxy-Authorization: Basic dWMxMC44NC4xNi4xOTM6MWY0N2QzZWY1M2IwMzU0NDM0NTFjN2VlNzg3M2ZmMzg=\r\n\r\n", ip, port, ip).getBytes();
				ByteBuffer request_buff=ByteBuffer.wrap(request);
				remote.write(request_buff);
				request_buff.clear();
				local.register(getSelector(), SelectionKey.OP_WRITE, this);
				break;
			default:
				remote.close();
				local.close();
				key.cancel();
				break;
		}

	}




	public HttpTunnel(Selector selector, IpHeader ipHeader)
	{
		super(selector, ipHeader);
	}


}
