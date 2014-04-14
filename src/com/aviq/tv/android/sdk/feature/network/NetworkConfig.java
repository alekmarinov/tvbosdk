/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    NetworkConfig.java
 * Author:      alek
 * Date:        14 Apr 2014
 * Description: Encapsulates network configuration properties
 */

package com.aviq.tv.android.sdk.feature.network;

import java.math.BigInteger;

import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Encapsulates network configuration properties
 */
public class NetworkConfig
{
	static String IntToIP(int intIp)
	{
		if (intIp == 0)
			return null;
		byte[] ipBytes = BigInteger.valueOf(intIp).toByteArray();

		if (ipBytes.length != 4)
		{
			ipBytes = new byte[4];
			ipBytes[0] = ipBytes[1] = ipBytes[2] = ipBytes[3] = 0;
		}

		// reverse bytes order
		byte temp;
		temp = ipBytes[0];
		ipBytes[0] = ipBytes[3];
		ipBytes[3] = temp;
		temp = ipBytes[1];
		ipBytes[1] = ipBytes[2];
		ipBytes[2] = temp;

		return TextUtils.implodeBytesArray(ipBytes, "%d", ".");
	}

	public String Iface;
	public boolean IsDHCP;
	public boolean IsUp;
	public String Addr;
	public String Mask;
	public String Gateway;
	public String Dns1;
	public String Dns2;

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(Iface);
		if (IsDHCP)
			sb.append(" dhcp");
		else
			sb.append(" manual");
		if (IsUp)
			sb.append(" up");
		else
			sb.append(" down");
		sb.append(" ip:").append(Addr);
		sb.append(" mask:").append(Mask);
		sb.append(" gw:").append(Gateway);
		sb.append(" dns1:").append(Dns1);
		sb.append(" dns2:").append(Dns2);
		return sb.toString();
	}
}
