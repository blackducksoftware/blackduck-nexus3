package com.synopsys.integration.blackduck.nexus3.database;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class PagedResultTest {

    @Test
    public void typeListTest() {
        final Iterable<String> iterables = Arrays.asList("first", "second");
        final PagedResult<String> pagedResult = new PagedResult<>(iterables, Optional.empty());

        final Iterable<String> passedItems = pagedResult.getTypeList();
        final Iterator<String> itemsIterator = passedItems.iterator();
        int counter = 0;
        while (itemsIterator.hasNext()) {
            itemsIterator.next();
            counter++;
        }

        Assert.assertEquals(2, counter);
    }

    @Test
    public void lastNameTest() {
        final PagedResult<String> pagedResultWithoutName = new PagedResult<>(null, Optional.empty());
        final Optional<String> noFoundName = pagedResultWithoutName.getLastName();
        Assert.assertFalse(noFoundName.isPresent());

        final PagedResult<String> pagedResultWithName = new PagedResult<>(null, Optional.of("name"));
        final Optional<String> foundName = pagedResultWithName.getLastName();
        Assert.assertTrue(foundName.isPresent());
    }

    @Test
    public void hasResultsTest() {
        final Iterable<String> listedItems = Arrays.asList("first", "second");
        final PagedResult<String> pagedResultWithResults = new PagedResult<>(listedItems, Optional.empty());
        Assert.assertTrue(pagedResultWithResults.hasResults());

        final Iterable<String> noItems = Arrays.asList();
        final PagedResult<String> pagedResultWithoutResults = new PagedResult<>(noItems, Optional.empty());
        Assert.assertFalse(pagedResultWithoutResults.hasResults());

        final PagedResult<String> nullPagedResults = new PagedResult<>(null, Optional.empty());
        Assert.assertFalse(nullPagedResults.hasResults());
    }
}
