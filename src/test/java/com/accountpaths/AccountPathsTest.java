package com.accountpaths;

import com.accountpaths.AccountPathsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AccountPathsTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AccountPathsPlugin.class);
		RuneLite.main(args);
	}
}