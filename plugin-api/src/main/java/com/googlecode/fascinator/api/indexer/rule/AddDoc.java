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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Solr input document
 * 
 * @author Oliver Lucido
 */
@XmlRootElement(name = "add")
@XmlAccessorType(XmlAccessType.NONE)
public class AddDoc {

    /** Allow duplicates attribute */
    @XmlAttribute
    private boolean allowDups = false;
    /** Field element list */
    @XmlElementWrapper(name = "doc")
    @XmlElement(name = "field")
    private List<Field> fields;

    /**
     * Gets the value of the allowDups attribute
     * 
     * @return <code>true</code> if duplicates allowed, <code>false</code>
     *         otherwise
     */
    public boolean isAllowDups() {
        return allowDups;
    }

    /**
     * Gets the fields for this document
     * 
     * @return List of fields
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * Sets the fields for this document
     * 
     * @param fields A list of fields
     */
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Gets the fields with the specified name
     * 
     * @param name A field name
     * @return List of fields (possibly empty)
     */
    public List<Field> getFields(String name) {
        List<Field> list = new ArrayList<Field>();
        for (Field field : getFields()) {
            if (name.equals(field.getName())) {
                list.add(field);
            }
        }
        return list;
    }

    /**
     * Creates an AddDoc instance from a Solr document reader
     * 
     * @param in Solr document reader
     * @return AddDoc instance
     * @throws JAXBException If an error occurred reading the Solr document
     */
    public static AddDoc read(Reader in) throws JAXBException {
        return (AddDoc) getUnmarshaller().unmarshal(in);
    }

    /**
     * Creates an AddDoc instance from a Solr document input stream
     * 
     * @param in Solr document input stream
     * @return AddDoc instance
     * @throws JAXBException If an error occurred reading the Solr document
     */
    public static AddDoc read(InputStream in) throws JAXBException {
        return (AddDoc) getUnmarshaller().unmarshal(in);
    }

    /**
     * Serializes this Solr document to the specified writer
     * 
     * @param out The writer to serialize to
     * @throws JAXBException If an error occurred while writing this Solr
     *             document
     */
    public void write(Writer out) throws JAXBException {
        getMarshaller().marshal(this, out);
    }

    /**
     * Serializes this Solr document to the specified output stream
     * 
     * @param out The output stream to serialize to
     * @throws JAXBException If an error occurred while writing this Solr
     *             document
     */
    public void write(OutputStream out) throws JAXBException {
        getMarshaller().marshal(this, out);
    }

    /**
     * Get context
     * 
     * @return JAXB Context
     */
    private static JAXBContext getContext() throws JAXBException {
        return JAXBContext.newInstance(AddDoc.class);
    }

    /**
     * Get the marshaller
     * 
     * @return Marshaller
     * @throws JAXBException if error in retrieving marshaller
     */
    private static Marshaller getMarshaller() throws JAXBException {
        return getContext().createMarshaller();
    }

    /**
     * Get the Unmarshaller
     * 
     * @return UnMarshaller
     * @throws JAXBException if error in retrieving unmarshaller
     */
    private static Unmarshaller getUnmarshaller() throws JAXBException {
        return getContext().createUnmarshaller();

    }
}
