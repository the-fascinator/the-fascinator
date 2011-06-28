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
import java.util.List;

import javax.xml.bind.JAXBException;

public class CopyField extends Rule {

    private String sourceFieldName;

    private String newFieldName;

    public CopyField(String newFieldName, String sourceFieldName) {
        super("Copy field");
        this.newFieldName = newFieldName;
        this.sourceFieldName = sourceFieldName;
    }

    @Override
    public void run(Reader in, Writer out) throws RuleException {
        log("Copying field " + sourceFieldName + " as " + newFieldName);
        try {
            AddDoc addDoc = AddDoc.read(in);
            List<Field> sourceFields = addDoc.getFields(sourceFieldName);
            for (Field sourceField : sourceFields) {
                Field newField = new Field(newFieldName, sourceField.getValue());
                addDoc.getFields().add(newField);
            }
            addDoc.write(out);
        } catch (JAXBException jaxbe) {
            throw new RuleException(jaxbe.getLinkedException());
        }
    }
}
