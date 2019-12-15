package org.vpns.proxy.nethook;
import org.vpns.proxy.core.HeaderHook;
import android.net.VpnService;

public class KingCardHeaderHook extends HeaderHook
{
	public KingCardHeaderHook(VpnService vs){
		super(vs);
	}
	@Override
	public void onHeader(StringBuilder header)
	{
		// TODO: Implement this method
	}
	
}
