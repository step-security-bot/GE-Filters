package com.salverrs.GEFilters.Filters.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeSearch {
    private String name;
    private short iconItemId;

    public GeSearch(String name, short iconItemId)
    {
        this.name = name;
        this.iconItemId = iconItemId;
    }
}
