package org.vpns.proxy.net;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.text.TextUtils;
import org.vpns.proxy.util.Stream;
import java.io.ByteArrayOutputStream;
import org.vpns.proxy.core.LocalVpnService;

public class HttpResponse extends Thread
{
	private static final byte[] crlf="\r\n".getBytes();
	private InputStream input;
	private OutputStream output;
	public HttpResponse(InputStream input, OutputStream output)
	{
		this.input = input;
		this.output = output;
	}

	

	@Override
	public void run()
	{
		String line=null;
		long content_length=-1;
		boolean isChunkded=false;
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try
		{
			while (!TextUtils.isEmpty(line = readLine(input).toString()))
			{
				if (line.startsWith("Content-Length:"))
					content_length = Long.parseLong(line.substring(15).trim());
				else if(line.startsWith("Transfer-Encoding:"))
					isChunkded=line.contains("chunked");
				baos.write(line.getBytes());
				baos.write(crlf);
			}
			baos.write(crlf);
			baos.flush();
			output.write(baos.toByteArray());
			output.flush();
			baos.close();
			if(isChunkded){
				Chunked chunked=new Chunked(input,output);
				chunked.process();
			}else
			if (content_length > 0)
			{
				Stream s=new Stream(input, output, content_length);
				s.run();
			}
			else if(content_length==-1)
			{
				Stream s=new Stream(input, output, Long.MAX_VALUE);
				s.run();
			}
		}
		catch (IOException e)
		{}
	}
	private StringBuilder readLine(InputStream input) throws IOException, IOException
	{
		StringBuilder header=new StringBuilder();
		while (true)
		{
			int v=input.read();
			switch (v)
			{
				case -1:
					return header;
				case '\r':
					int n=input.read();
					if (n == '\n')
					{
						return header;
					}
					else if (n == -1)
					{
						return header;
					}
					else
					{
						header.append((char)n);
						continue;
					}
				default:header.append((char)v); 
			}
		}
	}
}
