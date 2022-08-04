package com.salverrs.GEFilters;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GEFiltersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GEFiltersPlugin.class);
		RuneLite.main(args);
	}
}