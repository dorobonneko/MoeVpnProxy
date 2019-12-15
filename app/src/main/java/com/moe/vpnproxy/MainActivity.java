package com.moe.vpnproxy;

import android.app.*;
import android.os.*;
import android.view.Menu;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.net.VpnService;
import android.content.Intent;
import org.vpns.proxy.core.LocalVpnService;

public class MainActivity extends Activity implements Switch.OnCheckedChangeListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu,menu);
		Switch switch_=(Switch) menu.findItem(R.id.switch_).getActionView();
		switch_.setChecked(LocalVpnService.isRunning());
		switch_.setOnCheckedChangeListener(this);
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		if(p2){
			if(VpnService.prepare(this)!=null)
				startActivityForResult(VpnService.prepare(this),5844);
				else
				onActivityResult(5844,RESULT_OK,null);
		}else{
			stopService(new Intent(this,LocalVpnService.class));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO: Implement this method
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==5844&&resultCode==RESULT_OK){
			startService(new Intent(this,LocalVpnService.class));
		}
	}

	
}
