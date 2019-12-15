package org.vpns.proxy.util;

public class Arrays
{
	private static final char[] HEX=new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	public static StringBuilder bytes2Hex(byte[] data)
	{
		return bytes2Hex(data, 0, data.length);
	}
	public static StringBuilder bytes2Hex(byte[] data, int offset, int count)
	{
		StringBuilder result = new StringBuilder(count * 2);
		int end=offset + count;
		for (;offset < end;offset++)
		{
			result.append(HEX[(data[offset] & 0xf0) >>> 4]).append(HEX[data[offset] & 0x0f]);
		}
		return result;
	}
}
