package com.salverrs.GEFilters.Filters.Events;

import com.salverrs.GEFilters.Filters.SearchFilter;
import com.salverrs.GEFilters.Filters.Model.FilterOption;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtherFilterOptionActivated {
    private SearchFilter searchFilter;
    private FilterOption filterOption;

    public OtherFilterOptionActivated(SearchFilter filter, FilterOption option)
    {
        this.searchFilter = filter;
        this.filterOption = option;
    }
}
