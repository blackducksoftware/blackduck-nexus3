package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Named
@Singleton
public class DateTimeParser {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public String getCurrentDateTime() {
        return new DateTime().toString(DATE_TIME_PATTERN);
    }

    public String convertFromDateToString(final DateTime dateTime) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
        return dateTimeFormatter.print(dateTime);
    }

    public DateTime convertFromStringToDate(final String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC());
    }

    public long convertFromStringToMillis(final String date) {
        return convertFromDateTimeToMillis(convertFromStringToDate(date));
    }

    public long convertFromDateTimeToMillis(final DateTime dateTime) {
        return dateTime.toDate().getTime();
    }
}
