/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.portal.tapestry;

import org.apache.tapestry5.services.Request;

/**
 * Utility methods to aid working with the Tapestry Request interface.
 * 
 * @author andrewqcif
 * 
 */
public class TapestryRequestUtil {

    /**
     * Get QueryString is hidden from the Tapestry request so this method
     * attempts to recreate it.
     * 
     * @param request Tapestry request object
     * @return the query string from the underlying HTTP Request
     */
    public static String getQueryString(Request request) {
        if ("GET".equals(request.getMethod())) {
            String queryString = "";
            if (request.getParameterNames().size() > 0) {

                for (String parameterName : request.getParameterNames()) {
                    String[] parameterValues = request
                            .getParameters(parameterName);
                    for (String parameterValue : parameterValues) {
                        queryString += parameterName + "=" + parameterValue
                                + "&";
                    }
                }
                queryString = queryString
                        .substring(0, queryString.length() - 1);
            }
            return queryString;
        }
        return null;
    }
}
