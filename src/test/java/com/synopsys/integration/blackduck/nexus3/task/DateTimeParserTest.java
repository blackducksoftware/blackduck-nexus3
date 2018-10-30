package com.synopsys.integration.blackduck.nexus3.task;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;

public class DateTimeParserTest {

    private DateTime generateSpecificDate() {
        final DateTime dateTime = new DateTime()
                                      .withMillisOfDay(777)
                                      .withSecondOfMinute(8)
                                      .withMinuteOfHour(9)
                                      .withHourOfDay(3)
                                      .withDayOfMonth(2)
                                      .withMonthOfYear(1)
                                      .withYear(2001);
        return dateTime;
    }

    @Test
    public void getCurrentDateTimeTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();

        final long firstMark = System.currentTimeMillis() - 1;
        final String testedMark = dateTimeParser.getCurrentDateTime();
        final long secondMark = System.currentTimeMillis() + 1;

        final DateTime testTime = new DateTime(testedMark);

        Assert.assertTrue(testTime.isAfter(firstMark));
        Assert.assertTrue(testTime.isBefore(secondMark));
    }

    @Test
    public void formatDateTimeTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();

        final DateTime generatedDate = generateSpecificDate();
        final DateTime formattedNow = dateTimeParser.formatDateTime(generatedDate);

        Assert.assertEquals("2001-01-02T03:09:08.777Z", formattedNow.toString());
    }

    @Test
    public void convertFromDateToStringTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();
        final DateTime testValue = generateSpecificDate();

        final String convertedString = dateTimeParser.convertFromDateToString(testValue);

        Assert.assertEquals("2001-01-02T03:09:08.777", convertedString);
    }

    //    @Test
    // TODO clean up date parsing to make it more consistent through out.
    public void convertFromStringToDateTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();

        final DateTime convertedDateTime = dateTimeParser.convertFromStringToDate("2001-01-02T03:09:08.777");

        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DateTimeParser.DATE_TIME_PATTERN);
        dateTimeFormatter.withZoneUTC();
        final String dateTimeString = dateTimeFormatter.print(generateSpecificDate());
        final DateTime enforcedTime = DateTime.parse(dateTimeString);

        Assert.assertEquals(enforcedTime, convertedDateTime);
    }

}
