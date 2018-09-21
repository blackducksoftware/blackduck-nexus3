package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

@Named
@Singleton
public class DateTimeParser {

    public DateTime convertFromStringToDate(final String date) {
        final String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        return DateTime.parse(date, DateTimeFormat.forPattern(dateTimePattern).withZoneUTC());
    }

    public long convertFromStringToMillis(String date) {
        return convertFromStringToDate(date).toDate().getTime();
    }
}
