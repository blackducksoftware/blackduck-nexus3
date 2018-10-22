/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.nexus3.task;

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
