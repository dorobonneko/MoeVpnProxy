package org.vpns.proxy.util;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

public class SslUtil
{
	private static SSLSocketFactory ssf;
	public static SSLSocketFactory getSslFactory(){
		if(ssf==null){
			synchronized(SSLSocketFactory.class){
				if(ssf==null){
					try
					{
						SSLContext sc= SSLContext.getInstance("TLS");
						sc.init(null, new X509TrustManager[]{new X509TrustManager(){

										@Override
										public void checkClientTrusted(X509Certificate[] p1, String p2) throws CertificateException
										{
											// TODO: Implement this method
										}

										@Override
										public void checkServerTrusted(X509Certificate[] p1, String p2) throws CertificateException
										{
											// TODO: Implement this method
										}

										@Override
										public X509Certificate[] getAcceptedIssuers()
										{
											// TODO: Implement this method
											return new X509Certificate[0];
										}
									}}, new SecureRandom());
									ssf=sc.getSocketFactory();
					}
					catch (Exception e)
					{}
				}
			}
		}
		return ssf;
	}
}
