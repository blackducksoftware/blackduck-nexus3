package com.synopsys.integration.blackduck.nexus3.database;

import java.util.Optional;

public class PagedResult<TYPE> {
    private final Iterable<TYPE> typeList;
    private final Optional<String> lastName;
    private final int limit;

    public PagedResult(final Iterable<TYPE> typeList, final Optional<String> lastName, final int limit) {
        this.typeList = typeList;
        this.lastName = lastName;
        this.limit = limit;
    }

    public Iterable<TYPE> getTypeList() {
        return typeList;
    }

    public Optional<String> getLastName() {
        return lastName;
    }

    public int getLimit() {
        return limit;
    }

    public boolean hasResults() {
        return (typeList != null) && typeList.iterator().hasNext();
    }
}
