package com.salverrs.GEFilters.Filters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javax.inject.Inject;
import com.salverrs.GEFilters.Filters.Model.FilterOption;
import com.salverrs.GEFilters.Filters.Model.GeSearch;
import com.salverrs.GEFilters.Filters.Model.GeSearchResultWidget;
import com.salverrs.GEFilters.GEFiltersConfig;
import com.salverrs.GEFilters.GEFiltersPlugin;
import joptsimple.internal.Strings;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.salverrs.GEFilters.Filters.Model.InventorySetups.*;
import com.salverrs.GEFilters.Filters.Model.InventorySetups.Serialization.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

public class InventorySetupsSearchFilter extends SearchFilter {

    public static final String INVENTORY_SETUPS_CONFIG_GROUP = "inventorysetups";
    public static final String INVENTORY_SETUPS_CONFIG_KEY = "setupsV2"; // previously 'setups'
    private static final int SPRITE_ID_MAIN = SpriteID.TAB_EQUIPMENT;
    private static final String TITLE_MAIN = "Inventory Setups";
    private static final String SEARCH_BASE_MAIN = "inventory-setups";
    private static final String INV_SETUPS_MENU_IDENTIFIER = "Open setup";
    private static final String SETUPS_EXCEPTION_JSON_KEY = "inventory-setups-exceptions";
    private FilterOption inventorySetupsFilter;
    private boolean bankOpen = false;
    private List<String> setupExceptions = new ArrayList<>();

    @Inject
    public Gson gson;
    @Inject
    private GEFiltersConfig config;

    @Override
    protected void onFilterInitialising()
    {
        inventorySetupsFilter = new FilterOption(TITLE_MAIN, SEARCH_BASE_MAIN);

        setFilterOptions(inventorySetupsFilter);
        setIconSprite(SPRITE_ID_MAIN, 0);

        gson = gson.newBuilder().registerTypeAdapter(long.class, new LongTypeAdapter()).create();
        gson = gson.newBuilder().registerTypeAdapter(InventorySetupItemSerializable.class, new InventorySetupItemSerializableTypeAdapter()).create();
    }

    @Override
    protected void onFilterStarted()
    {
        loadSetupExceptions();
    }

    @Override
    protected void onFilterEnabled(FilterOption option)
    {
        if (option == inventorySetupsFilter)
        {
            if (option.getData() != null)
            {
                generateSetupResults((String)(option.getData()));
            }
            else
            {
                addInvSetupsFilterOptionResults();
            }
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == WidgetID.BANK_GROUP_ID)
        {
            bankOpen = true;
        }
    }

    @Override
    public void onWidgetClosed(WidgetClosed event)
    {
        super.onWidgetClosed(event);

        if (event.getGroupId() == WidgetID.BANK_GROUP_ID)
        {
            bankOpen = false;
        }
    }

    @Subscribe
    protected void onClientTick(ClientTick clientTick)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
            return;

        if (!bankOpen)
            return;

        final List<MenuEntry> entries = new ArrayList<>(Arrays.asList(client.getMenuEntries()));
        boolean isSetupsMenu = false;

        for (MenuEntry entry : entries)
        {
            final String option = entry.getOption();
            if (option.contains(INV_SETUPS_MENU_IDENTIFIER))
            {
                isSetupsMenu = true;
                break;
            }
        }

        if (!isSetupsMenu)
            return;

        final List<String> setupNames = getInventorySetupNames();

        final MenuEntry parent = client.createMenuEntry(-1)
                .setOption("GE Filters Setups")
                .setTarget("")
                .setType(MenuAction.RUNELITE_SUBMENU);


        for (String setup : setupNames)
        {
            if (setupExceptions.contains(setup))
            {
                client.createMenuEntry(-1)
                        .setOption("Include")
                        .setTarget(setup)
                        .setType(MenuAction.RUNELITE)
                        .setParent(parent)
                        .onClick(removeSetupFromExceptions(setup));
            }
            else
            {
                client.createMenuEntry(-1)
                        .setOption("Exclude")
                        .setTarget(setup)
                        .setType(MenuAction.RUNELITE)
                        .setParent(parent)
                        .onClick(addSetupToExceptions(setup));
            }
        }

    }

    private void addInvSetupsFilterOptionResults()
    {
        final ArrayList<GeSearch> setupFilters = new ArrayList<>();
        final List<String> setupNames = getInventorySetupNames();

        if (setupNames == null || setupNames.size() == 0)
            return;

        for (String setup : setupNames)
        {
            if (setupExceptions.contains(setup))
                continue;

            setupFilters.add(new GeSearch(setup, (short)SpriteID.TAB_EQUIPMENT));
        }

        setGESearchResults(getEmptySearchResults(setupFilters.size()));
        setSearchResultsHidden(true);

        clientThread.invokeLater(() -> {
            final List<GeSearchResultWidget> searchResultWidgets = getGeSearchResults();
            generateInvSetupsResults(setupFilters, searchResultWidgets);
            setSearchResultsHidden(false);
        });
    }

    private void generateInvSetupsResults(List<GeSearch> filters, List<GeSearchResultWidget> searchResults)
    {
        if (searchResults.size() == 0)
            return;

        int resultIndex = 0;
        final int resultSize = searchResults.size();

        for (GeSearch filter : filters)
        {
            if (resultIndex == resultSize)
                break;

            final String setupName = filter.getName();
            final GeSearchResultWidget searchResult = searchResults.get(resultIndex);

            searchResult.setTitleText(setupName);
            searchResult.setTooltipText(setupName);
            searchResult.setSpriteId(filter.getIconItemId());
            searchResult.setSpriteSize(22, 24);
            searchResult.setSpriteOffset(5, 2);

            searchResult.setOnOpListener((JavaScriptCallback)(e) ->
            {
                final String title = TITLE_MAIN + " - " + setupName;
                inventorySetupsFilter.setData(setupName);
                searchGE(inventorySetupsFilter.getSearchValue());
                setTitle(title);
            });

            resultIndex++;
        }
    }

    private void generateSetupResults(String setupName)
    {
        final String title = TITLE_MAIN + " - " + setupName;
        final InventorySetup setup = getInventorySetup(setupName);

        if (setup == null)
            return;

        final List<InventorySetupsItem> invItems = setup.getInventory();
        final List<InventorySetupsItem> equipmentItems = setup.getEquipment();
        final List<InventorySetupsItem> runePouchItems = setup.getRune_pouch();
        final List<InventorySetupsItem> boltPouchItems = setup.getBoltPouch();
        final List<InventorySetupsItem> additionalFilteredItems = new ArrayList<>(setup.getAdditionalFilteredItems().values());

        List<Short> itemIds = new ArrayList<Short>();

        if (config.enableInvSetupsEquipment() && equipmentItems != null)
            itemIds.addAll(getSetupItemIds(equipmentItems));

        if (config.enableInvSetupsInventory() && invItems != null)
            itemIds.addAll(getSetupItemIds(invItems));

        if (config.enableInvSetupsRunePouch() && runePouchItems != null)
            itemIds.addAll(getSetupItemIds(runePouchItems));

        if (config.enableInvSetupsBoltPouch() && boltPouchItems != null)
            itemIds.addAll(getSetupItemIds(boltPouchItems));

        if (config.enableInvSetupsAdditionalItems() && additionalFilteredItems != null)
            itemIds.addAll(getSetupItemIds(additionalFilteredItems));

        setTitle(title);
        addInventorySetupItemResults(itemIds);
        saveSearchState(SEARCH_BASE_MAIN);
    }

    private List<GeSearchResultWidget> getGeSearchResults()
    {
        final List<GeSearchResultWidget> results = new ArrayList<>();
        final Widget[] geSearchResultWidgets = client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS).getDynamicChildren();
        final Queue<Widget> widgetQueue = new LinkedList<Widget>();

        for (Widget w : geSearchResultWidgets)
        {
            widgetQueue.add(w);

            if (widgetQueue.size() == 3)
            {
                final Widget container = widgetQueue.remove();
                final Widget title = widgetQueue.remove();
                final Widget icon = widgetQueue.remove();
                final short itemId = (short)icon.getItemId();

                results.add(new GeSearchResultWidget(container, title, icon, itemId));
            }
        }

        return results;
    }

    private Consumer<MenuEntry> addSetupToExceptions(String setup)
    {
        return e ->
        {
            if (setupExceptions.contains(setup))
                return;

            setupExceptions.add(setup);
            saveSetupExceptions();
        };
    }

    private Consumer<MenuEntry> removeSetupFromExceptions(String setup)
    {
        return e ->
        {
            if (!setupExceptions.contains(setup))
                return;

            setupExceptions.remove(setup);
            saveSetupExceptions();
        };
    }

    private void saveSetupExceptions()
    {
        final String[] setupExc = new String[setupExceptions.size()];
        setupExceptions.toArray(setupExc);
        final String json = gson.toJson(setupExc);
        configManager.setConfiguration(GEFiltersPlugin.CONFIG_GROUP_DATA, SETUPS_EXCEPTION_JSON_KEY, json);
    }

    private void loadSetupExceptions()
    {
        final String setupExceptionsJson = configManager.getConfiguration(GEFiltersPlugin.CONFIG_GROUP_DATA, SETUPS_EXCEPTION_JSON_KEY);
        if (setupExceptionsJson == null || setupExceptionsJson.equals(""))
        {
            setupExceptions = new ArrayList<>();
        }
        else
        {
            final String[] setupExc = gson.fromJson(setupExceptionsJson, String[].class);
            setupExceptions = new ArrayList(Arrays.asList(setupExc));
        }
    }

    private short[] getEmptySearchResults(int size)
    {
        final short[] ids = new short[size];
        for (int i = 0; i < size; i++)
        {
            ids[i] = 0;
        }

        return ids;
    }

    private InventorySetup getInventorySetup(String name)
    {
        return getInventorySetups().stream().filter(s -> s.getName().equals(name)).findAny().orElse(null);
    }

    private List<String> getInventorySetupNames()
    {
        return getInventorySetups().stream().map(s -> s.getName()).collect(Collectors.toList());
    }

    private List<InventorySetup> getInventorySetups()
    {
        final String storedSetups = configManager.getConfiguration(INVENTORY_SETUPS_CONFIG_GROUP, INVENTORY_SETUPS_CONFIG_KEY);
        if (Strings.isNullOrEmpty(storedSetups))
        {
            return null;
        }
        try
        {
            final Type type = new TypeToken<ArrayList<InventorySetupSerializable>>(){}.getType();
            final List<InventorySetupSerializable> issList = new ArrayList<>(gson.fromJson(storedSetups, type));
            List<InventorySetup> setups = new ArrayList<>();

            for (final InventorySetupSerializable iss : issList)
            {
                setups.add(InventorySetupSerializable.convertToInventorySetup(iss));
            }

            return setups;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private List<Short> getSetupItemIds(List<InventorySetupsItem> items)
    {
        return items.stream().map(i -> (short)i.getId()).collect(Collectors.toList());
    }

    private void addInventorySetupItemResults(List<Short> itemIds)
    {
        final List<Short> finalItems = new ArrayList<>();

        for (Short id : itemIds)
        {
            if (id == -1)
                continue;

            ItemComposition composition = client.getItemDefinition(id);
            ItemComposition unnotedComposition = null;

            final int notedId = composition.getLinkedNoteId();
            if (notedId != -1)
            {
                unnotedComposition = client.getItemDefinition(notedId);
            }

            if (finalItems.contains(id) || finalItems.contains((short)notedId))
                    continue;

            if (composition.isTradeable())
            {
                finalItems.add(id);
            }
            else if (unnotedComposition != null && unnotedComposition.isTradeable())
            {
                finalItems.add((short)notedId);
            }
        }

        final short[] itemResultIds = FilterUtility.getPrimitiveShortArray(finalItems);
        setGESearchResults(itemResultIds);
    }

}
