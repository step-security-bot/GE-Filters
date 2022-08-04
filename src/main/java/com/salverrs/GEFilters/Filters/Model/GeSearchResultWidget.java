package com.salverrs.GEFilters.Filters.Model;

import net.runelite.api.widgets.Widget;

public class GeSearchResultWidget {
    private short itemId;
    private Widget container;
    private Widget title;
    private Widget icon;

    public GeSearchResultWidget(Widget container, Widget title, Widget icon, short itemId)
    {
        this.container = container;
        this.title = title;
        this.icon = icon;
        this.itemId = itemId;
    }

    public void setTooltipText(String text)
    {
        container.setName("<col=ff9040>" + text + "</col>");
    }

    public void setTitleText(String text)
    {
        title.setText(text);
    }

    public void setIcon(short itemId)
    {
        icon.setItemId(itemId);
        icon.setSpriteId(itemId);
        icon.revalidate();
    }

    public void setOnOpListener(Object... args)
    {
        container.setOnOpListener(args);
    }
}
