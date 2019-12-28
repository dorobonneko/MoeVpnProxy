package com.moe.vpnproxy;

import android.app.*;
import android.os.*;
import android.view.Menu;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.net.VpnService;
import android.content.Intent;
import org.vpns.proxy.core.LocalVpnService;
import android.view.View;
import java.net.URL;
import java.io.IOException;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import org.vpns.proxy.util.SslUtil;
import java.io.InputStream;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.view.ViewGroup;
import android.content.IntentFilter;
import com.moe.vpnproxy.util.Preference;
import android.view.MenuItem;
import android.widget.EditText;
import org.vpns.proxy.nethook.KingCard;
import android.content.DialogInterface;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.ScrollingMovementMethod;

public class MainActivity extends Activity implements Switch.OnCheckedChangeListener
{
	private TextView msg;
	private Message message;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		msg=findViewById(R.id.msg);
		msg.setMovementMethod(new ScrollingMovementMethod());
		IntentFilter ifilter=new IntentFilter(getPackageName().concat(".Write"));
		ifilter.addAction(getPackageName().concat(".Clear"));
		registerReceiver(message=new Message(),ifilter);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu,menu);
		Switch switch_=(Switch) menu.findItem(R.id.switch_).getActionView();
		switch_.setChecked(LocalVpnService.isRunning());
		switch_.setOnCheckedChangeListener(this);
		menu.findItem(R.id.global_ssl).setChecked(Preference.is(this,"ssl",true));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case R.id.global_ssl:
				item.setChecked(!item.isChecked());
				Preference.put(this,"ssl",item.isChecked());
				break;
			case R.id.api:
				final EditText et=new EditText(this);
				et.setText(Preference.get(this,"api",KingCard.Api));
				new AlertDialog.Builder(this).setTitle("接口").setView(et).setNegativeButton("保存", new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface p1, int p2)
						{
							Preference.put(getApplicationContext(),"api",et.getText().toString().trim());
						}
					}).show();
				break;
			case R.id.reloadToken:
				if(LocalVpnService.isRunning())
					KingCard.getInstance().refresh();
				break;
		}
		return super.onOptionsItemSelected(item);
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
			LocalVpnService.Instance.stopVpn();
			//stopService(new Intent(this,LocalVpnService.class));
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

	@Override
	protected void onDestroy()
	{
		unregisterReceiver(message);
		super.onDestroy();
	}
	
	class Message extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context p1, Intent p2)
		{
			switch(p2.getAction()){
				case "com.moe.vpnproxy.Clear":
					msg.setText(null);
					break;
				default:
			msg.setText(msg.getText()+"\n"+p2.getStringExtra(p2.getAction()));
			break;
			}
		}

	
}
	
}
