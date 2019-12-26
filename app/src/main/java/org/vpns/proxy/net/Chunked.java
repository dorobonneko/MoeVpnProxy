package org.vpns.proxy.net;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class Chunked
{
	private static final byte[] crlf="\r\n".getBytes();
	private InputStream input;
	private OutputStream output;
	public Chunked(InputStream input,OutputStream output){
		this.input=input;
		this.output=output;
	}
	public void process() throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		int v=0;
		int len=0;
		byte[] buff=new byte[1024];
		out:
		while((v=input.read())!=-1){
			switch(v){
				case '\r':
					int lf=input.read();
					if(lf=='\n'){
						//size读取结束
						long length=Long.parseUnsignedLong(baos.toString(),16);
						
						output.write(baos.toByteArray());
						baos.reset();
						//读取该长度数据
						output.write(crlf);
						if(length==0){
							output.write(crlf);
							output.flush();
							
							break out;
						}
						do{
							if(length>buff.length){
							len=input.read(buff);
						}else{
							len=input.read(buff,0,(int)length);
						}
						if(len==-1)break;
						length-=len;
						output.write(buff,0,len);
						//output.flush();
						if(length==0){
							output.write(crlf);
							output.flush();
							input.skip(2);
							break;
						}
						}while(true);
					}else{
						baos.write(v);
					}
					break;
				default:
				baos.write(v);
				break;
			}
			//判断已读数据长度
			if(baos.size()>=10){
				//完整数据体！直接读写
				output.write(baos.toByteArray());
				while((len=input.read(buff))!=-1){
					output.write(buff,0,(int)len);
					output.flush();
					}
				break;
			}
		}
		baos.close();
		input.close();
		output.close();
	}
}
