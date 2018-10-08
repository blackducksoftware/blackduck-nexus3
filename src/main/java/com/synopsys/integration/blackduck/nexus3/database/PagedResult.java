package com.synopsys.integration.blackduck.nexus3.database;

import java.util.Optional;

public class PagedResult<TYPE> {
    private final Iterable<TYPE> typeList;
    private final Optional<String> lastName;

    public PagedResult(final Iterable<TYPE> typeList, final Optional<String> lastName) {
        this.typeList = typeList;
        this.lastName = lastName;
    }

    public Iterable<TYPE> getTypeList() {
        return typeList;
    }

    public Optional<String> getLastName() {
        return lastName;
    }

    public boolean hasResults() {
        return (typeList != null) && typeList.iterator().hasNext();
    }
}
