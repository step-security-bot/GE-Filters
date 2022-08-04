package com.salverrs.GEFilters.Filters.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterOption
{
    private String title;
    private String searchValue;

    public FilterOption(String title, String searchValue)
    {
        this.title = title;
        this.searchValue = searchValue;
    }
}
