package com.salverrs.GEFilters;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup(GEFiltersPlugin.CONFIG_GROUP)
public interface GEFiltersConfig extends Config
{
	@ConfigSection(
			name = "Filters",
			description = "Grand Exchange search filters.",
			position = 0
	)
	String filtersSection = "filters";

	@ConfigSection(
			name = "Preferences",
			description = "Grand Exchange search filter preferences.",
			position = 1
	)
	String preferencesSection = "preferences";

	@ConfigItem(
		keyName = "enableBankTagFilter",
		name = "Enable Bank Tag Filter",
		description = "Filters GE items by bank tag. Requires the Bank Tag plugin to be enabled.",
		section = filtersSection,
		position = 0
	)
	default boolean enableBankTagFilter()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableInventoryFilter",
			name = "Enable Inventory Filter",
			description = "Filters GE items by inventory/equipped items.",
			section = filtersSection,
			position = 1
	)
	default boolean enableInventoryFilter()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableRecentItemsFilter",
			name = "Enable Recent Items Filter",
			description = "Filters GE items by recently viewed or recent buy/sell offers.",
			section = filtersSection,
			position = 2
	)
	default boolean enableRecentItemsFilter()
	{
		return true;
	}

	@ConfigItem(
			keyName = "filterTitleColour",
			name = "Filter Title Colour",
			description = "The text colour for filter titles.",
			section = preferencesSection,
			position = 3
	)
	default Color filterTitleColour()
	{
		return new Color(178, 0, 0);
	}


	@ConfigItem(
			keyName = "keyPressOverridesFilter",
			name = "Typing Overrides Active Filter",
			description = "When enabled typing will override the currently active filter and perform a regular search.",
			section = preferencesSection,
			position = 4
	)
	default boolean keyPressOverridesFilter()
	{
		return true;
	}


}
