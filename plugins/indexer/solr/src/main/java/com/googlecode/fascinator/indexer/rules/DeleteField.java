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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

public class DeleteField extends Rule {

    private String fieldName;

    private String regex;

    public DeleteField(String fieldName) {
        this(fieldName, "^\\s*$");
    }

    public DeleteField(String fieldName, String regex) {
        super("Delete field");
        this.fieldName = fieldName;
        this.regex = regex;
    }

    @Override
    public void run(Reader in, Writer out) throws RuleException {
        log("Deleting '" + fieldName + "' fields that match '" + regex + "'");
        try {
            AddDoc addDoc = AddDoc.read(in);
            List<Field> fields = addDoc.getFields(fieldName);
            List<Field> deletedFields = new ArrayList<Field>();
            for (Field field : fields) {
                String value = field.getValue();
                boolean match = Pattern.matches(regex, field.getValue());
                if (match) {
                    deletedFields.add(field);
                    log("Deleted matching value '" + value + "'");
                } else {
                    log("Keep unmatched value '" + value + "'");
                }
            }
            for (Field field : deletedFields) {
                addDoc.getFields().remove(field);
            }
            addDoc.write(out);
        } catch (JAXBException jaxbe) {
            throw new RuleException(jaxbe.getLinkedException());
        }
    }
}
