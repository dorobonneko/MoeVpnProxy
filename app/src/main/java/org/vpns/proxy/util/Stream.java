package org.vpns.proxy.util;

import java.io.*;

public class Stream extends Thread
{
	private InputStream input;
	private OutputStream output;
	private long size;
	public Stream(InputStream input,OutputStream output,long size){
		this.input=input;
		this.output=output;
		this.size=size;
	}
	
	@Override
	public void run()
	{
		byte[] buff=new byte[10240];
		int len=-1;
		long length=0;
		try
		{
			while ((len = input.read(buff)) != -1)
			{
				output.write(buff,0,len);
				output.flush();
				length+=len;
				if(length>=size)break;
			}
			}
		catch (IOException e)
		{try
			{
				output.flush();
			}
			catch (Exception ee)
			{}
			}finally{
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
			}
	}

}
