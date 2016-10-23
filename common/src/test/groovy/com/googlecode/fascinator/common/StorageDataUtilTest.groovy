/*
 *
 *  Copyright (C) 2016 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License along
 *    with this program; if not, write to the Free Software Foundation, Inc.,
 *    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */
package com.googlecode.fascinator.common

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author <a href="matt@redboxresearchdata.com.au">Matt Mulholland</a>
 * created on 21/10/16.
 */
@Slf4j
public class StorageDataUtilTest extends Specification {

    def storageDataUtil

    def setup() {
        storageDataUtil = new StorageDataUtil()
    }

    @Unroll
    def "W3C dates method return full ISO8601 datetimes"() throws Exception {
        given:
        DateTimeZone.setDefault(DateTimeZone.forID("Australia/Brisbane"))
        def currentZone = DateTimeZone.getDefault()
        log.info("current zone is: " + currentZone)
        when:
        def result = storageDataUtil.getW3CDateTime(dateText)
        then:
        assert expected == result
        noExceptionThrown()
        where:
        dateText                    | expected
        "2004"                      | "2004-01-01T00:00:00.000+10:00"
        "20040102"                  | "20040102-01-01T00:00:00.000+10:00"
        "2004-01-02"                | "2004-01-02T00:00:00.000+10:00"
        "2004-04-02T18:06"          | "2004-04-02T18:06:00.000+10:00"
        "2004-04-02T18:06:02"       | "2004-04-02T18:06:02.000+10:00"
        "2004-04-02T18:06:02.012"   | "2004-04-02T18:06:02.012+10:00"
        "2004-04-02T18:06:02.01234" | "2004-04-02T18:06:02.012+10:00"
        "2004-04-02T18:06:02z"      | "2004-04-03T04:06:02.000+10:00"
        "2004-04-02T18:06:02.000Z"  | "2004-04-03T04:06:02.000+10:00"
        "2004-04-02T18:06:02+10:00" | "2004-04-02T18:06:02.000+10:00"
    }

    @Unroll
    def "W3C dates method but blank/null argument returns blank"() throws Exception {
        when:
        def result = storageDataUtil.getW3CDateTime(dateText)
        then:
        result == expected
        noExceptionThrown()
        where:
        dateText | expected
        null     | ""
        ""       | ""
        "  "     | ""
    }

    @Unroll
    def "W3C dates method with non-date-time format throws an exception"() throws Exception {
        when:
        def result = storageDataUtil.getW3CDateTime(dateText)
        then:
        Exception e = thrown()
        e.getClass() == expected
        log.debug("Method threw an expected exception: " + e.getMessage())
        where:
        dateText | expected
        "test"   | IllegalArgumentException
    }

    @Unroll
    def "W3C dates method return datetimes in specified format"() throws Exception {
        given:
        DateTimeZone.setDefault(DateTimeZone.forID("Australia/Brisbane"))
        def currentZone = DateTimeZone.getDefault()
        log.info("current zone is: " + currentZone)
        when:
        def result = storageDataUtil.getDateTime(dateText, format)
        then:
        assert expected == result
        noExceptionThrown()
        where:
        dateText           | format                     | expected
        "2004"             | "yyyy"                     | "2004"
        "2004"             | "yyyy-MM"                  | "2004-01"
        "2004"             | "yyyy-MM-DD"               | "2004-01-01"
        "2004-04-02T18:06" | "YYYY-MM-dd'T'hh:mm:ss.ss" | "2004-04-02T06:06:00.00"
        "2004-01-01"       | ""                         | "2004-01-01T00:00:00.000+10:00"
        "2004-01"          | " "                        | "2004-01-01T00:00:00.000+10:00"
        "2004-01"          | null                       | "2004-01-01T00:00:00.000+10:00"
    }

    @Unroll
    def "W3C dates method with specified format but blank/null argument returns blank"() throws Exception {
        when:
        def result = storageDataUtil.getDateTime(dateText, format)
        then:
        result == expected
        noExceptionThrown()
        where:
        dateText | format                     | expected
        ""       | "YYYY-MM-dd'T'hh:mm:ss.ss" | ""
        " "      | "YYYY-MM-dd"               | ""
        null     | "YYYY"                     | ""
        ""       | ""                         | ""
        ""       | null                       | ""
        ""       | " "                        | ""
        " "      | null                       | ""
        " "      | " "                        | ""
        " "      | ""                         | ""

    }

    @Unroll
    def "W3C dates method with specified format throws an exception"() throws Exception {
        when:
        def result = storageDataUtil.getDateTime(dateText, format)
        then:
        Exception e = thrown()
        e.getClass() == expected
        log.debug("Method threw an expected exception: " + e.getMessage())
        where:
        dateText          | format                     | expected
        "2004-04-0218:06" | "YYYY-MM-dd'T'hh:mm:ss.ss" | IllegalArgumentException
    }
}
