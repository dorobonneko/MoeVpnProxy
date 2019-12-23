package com.moe.vpnproxy.util;
import android.content.Context;
import android.content.SharedPreferences;
import com.moe.vpnproxy.MainActivity;

public class Preference
{
	private static final String TAG="moe";

	public static String get(Context p0, String p1, String api)
	{
		// TODO: Implement this method
		return p0.getSharedPreferences(TAG,0).getString(p1,api);
	}
	public static void put(Context context,String key,String value){
		context.getSharedPreferences(TAG,0).edit().putString(key,value).commit();
	}
	public static boolean is(Context context,String key,boolean default_){
		return context.getSharedPreferences(TAG,0).getBoolean(key,default_);
	}
	public static void put(Context context,String key,boolean value){
		context.getSharedPreferences(TAG,0).edit().putBoolean(key,value).commit();
	}
	public static void register(Context context,SharedPreferences.OnSharedPreferenceChangeListener c){
		context.getSharedPreferences(TAG,0).registerOnSharedPreferenceChangeListener(c);
	}
	public static void unregister(Context context,SharedPreferences.OnSharedPreferenceChangeListener c){
		context.getSharedPreferences(TAG,0).unregisterOnSharedPreferenceChangeListener(c);
	}
}
