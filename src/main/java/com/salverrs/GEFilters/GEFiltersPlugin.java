package com.salverrs.GEFilters;

import com.google.inject.Provides;
import javax.inject.Inject;

import com.salverrs.GEFilters.Filters.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Slf4j
@PluginDescriptor(
	name = "GE Filters",
	description = "Search filters for the Grand Exchange.",
	tags = {"ge","filter","grand","exchange","search","bank","tag","inventory","setups"},
	enabledByDefault = true
)
public class GEFiltersPlugin extends Plugin
{
	public static final String CONFIG_GROUP = "GE_FILTERS_CONFIG";
	public static final String CONFIG_GROUP_DATA = "GE_FILTERS_CONFIG_DATA";
	public static final String BANK_TAGS_COMP_NAME = "Bank Tags";
	private static final String SEARCH_BUY_PREFIX = "What would you like to buy?";
	public static final String INVENTORY_SETUPS_COMP_NAME = "Inventory Setups";
	private static final int SEARCH_BOX_LOADED_ID = 750;
	private static final int SEARCH_STRING_APPEND_ID = 222;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private GEFiltersConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private BankTabSearchFilter bankTabSearchFilter;
	@Inject
	private InventorySetupsSearchFilter inventorySetupsSearchFilter;
	@Inject
	private RecentItemsSearchFilter recentItemsSearchFilter;
	@Inject
	private InventorySearchFilter inventorySearchFilter;
	@Inject
	private PluginManager pluginManager;

	private List<SearchFilter> filters;

	@Override
	protected void startUp() throws Exception
	{
		log.info("GE Filters started!");
		clientThread.invoke(() ->
		{
			loadFilters();
			tryStartFilters();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("GE Filters stopped!");
		clientThread.invoke(() ->
		{
			stopFilters();
		});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == SEARCH_BOX_LOADED_ID)
		{
			clientThread.invoke(() ->
			{
				tryStartFilters();
			});
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (config.hideSearchPrefix() && event.getScriptId() == SEARCH_STRING_APPEND_ID)
		{
			final String[] stringStack = client.getStringStack();
			if (stringStack[0].equals(SEARCH_BUY_PREFIX))
			{
				stringStack[0] = "";
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(GEFiltersPlugin.CONFIG_GROUP))
			return;

		clientThread.invoke(() ->
		{
			stopFilters();
			loadFilters();
			tryStartFilters();
		});
	}

	private void loadFilters()
	{
		filters = new ArrayList<>();

		if (config.enableBankTagFilter() && isPluginEnabled(BANK_TAGS_COMP_NAME))
		{
			filters.add(bankTabSearchFilter);
		}

		if (config.enableInventorySetupsFilter() && isPluginEnabled(INVENTORY_SETUPS_COMP_NAME))
		{
			filters.add(inventorySetupsSearchFilter);
		}

		if (config.enableInventoryFilter())
		{
			filters.add(inventorySearchFilter);
		}

		if (config.enableRecentItemsFilter())
		{
			filters.add(recentItemsSearchFilter);
		}

		registerFilterEvents();
	}

	private void tryStartFilters()
	{
		if (isSearchVisible())
		{
			startFilters();
		}
	}

	private void startFilters()
	{
		final int horizontalSpacing = SearchFilter.ICON_SIZE + config.filterHorizontalSpacing();
		int xOffset = 0;

		for (SearchFilter filter : filters)
		{
			filter.start(xOffset, 0);
			xOffset += horizontalSpacing ;
		}
	}

	private void stopFilters()
	{
		for (SearchFilter filter : filters)
		{
			filter.stop();
		}

		unregisterFilterEvents();
	}

	private void registerFilterEvents()
	{
		for (SearchFilter filter : filters)
		{
			eventBus.register(filter);
		}
	}

	private void unregisterFilterEvents()
	{
		for (SearchFilter filter : filters)
		{
			eventBus.unregister(filter);
		}
	}

	private boolean isPluginEnabled(String pluginName)
	{
		final Collection<Plugin> plugins = pluginManager.getPlugins();
		for (Plugin plugin : plugins)
		{
			final String name = plugin.getName();
			if (name.equals(pluginName))
			{
				return pluginManager.isPluginEnabled(plugin);
			}
		}

		return false;
	}

	private boolean isSearchVisible()
	{
		final Widget widget = client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS);
		return widget != null && !widget.isHidden();
	}

	@Provides
	GEFiltersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GEFiltersConfig.class);
	}
}
