/* 
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.api.indexer.rule;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Solr field used with {@link AddDoc}
 * 
 * @see AddDoc
 * @author Oliver Lucido
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Field {

    /** Maximum value length for display purposes */
    public static final int MAX_LENGTH = 60;

    /** Field name */
    @XmlAttribute
    private String name;

    /** Field value */
    @XmlValue
    private String value;

    /**
     * Default constructor
     */
    public Field() {
    }

    /**
     * Creates a field with the specified name and value
     * 
     * @param name Field name
     * @param value Field value
     */
    public Field(String name, String value) {
        this.name = name;
        this.value = value == null ? "" : value;
    }

    /**
     * Gets the field name
     * 
     * @return The field name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the field value
     * 
     * @return The field value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the field value
     * 
     * @param value The field vaue
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * String representation of this field as &quot;name: value&quot;. If the
     * value exceeds MAX_LENGTH, it is truncated.
     */
    @Override
    public String toString() {
        String shortValue = value;
        if (value.length() > MAX_LENGTH) {
            shortValue = value.substring(0, MAX_LENGTH) + "...";
        }
        return name + ": " + shortValue;
    }
}
