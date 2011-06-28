/* 
 * The Fascinator - Indexer
 * Copyright (C) 2009 University of Southern Queensland
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
package com.googlecode.fascinator.indexer.rules;

import com.googlecode.fascinator.api.indexer.rule.AddDoc;
import com.googlecode.fascinator.api.indexer.rule.Field;
import com.googlecode.fascinator.api.indexer.rule.Rule;
import com.googlecode.fascinator.api.indexer.rule.RuleException;

import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBException;

/**
 * Adds a single field to a Solr document
 * 
 * @author Oliver Lucido
 */
public class AddField extends Rule {

    /** The field to add */
    private Field field;

    /**
     * Creates a rule to add a field with no initial value
     * 
     * @param fieldName Field name
     */
    public AddField(String fieldName) {
        this(fieldName, "");
    }

    /**
     * Creates a rule to add a field with the specified name and initial value
     * 
     * @param fieldName Field name
     * @param value Field value
     */
    public AddField(String fieldName, String value) {
        super("Add field");
        field = new Field(fieldName, value);
    }

    /**
     * Sets the value of the field
     * 
     * @param value Field value
     */
    public void setValue(String value) {
        field.setValue(value);
    }

    /**
     * Adds a single field to a Solr document
     * 
     * @param in Solr document
     * @param out Solr document with added field
     */
    @Override
    public void run(Reader in, Writer out) throws RuleException {
        //log("Adding field [" + field + "]");
        try {
            AddDoc addDoc = AddDoc.read(in);
            addDoc.getFields().add(field);
            addDoc.write(out);
        } catch (JAXBException jaxbe) {
            throw new RuleException(jaxbe.getLinkedException());
        }
    }
}
