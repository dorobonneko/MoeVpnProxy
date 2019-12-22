package com.moe.vpnproxy.util;
import android.content.Context;
import android.content.SharedPreferences;

public class Preference
{
	private static final String TAG="moe";
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
