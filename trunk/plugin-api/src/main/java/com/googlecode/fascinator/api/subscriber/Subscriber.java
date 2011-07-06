/*
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package com.googlecode.fascinator.api.subscriber;

import java.util.Map;

import com.googlecode.fascinator.api.Plugin;

/**
 * An Subscriber plugin describe and implement Subscriber schema
 * 
 * @author Linda Octalina
 */
public interface Subscriber extends Plugin {

    /**
     * OnEvent method that received number of parameters to be submitted to
     * subscriber solr schema
     * 
     * @param param List of parameters
     */
    public void onEvent(Map<String, String> param) throws SubscriberException;
}
