package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

@Named
@Singleton
public class DateTimeParser {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public String getCurrentDateTime() {
        return new DateTime().toString(DATE_TIME_PATTERN);
    }

    public DateTime convertFromStringToDate(final String date) {
        return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC());
    }

    public long convertFromStringToMillis(final String date) {
        return convertFromStringToDate(date).toDate().getTime();
    }
}
