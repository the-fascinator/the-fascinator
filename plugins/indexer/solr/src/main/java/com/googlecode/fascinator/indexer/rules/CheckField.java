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
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

public class CheckField extends Rule {

    private String fieldName;

    private String regex;

    private boolean matchAll;

    public CheckField(String fieldName) {
        this(fieldName, ".+", false);
    }

    public CheckField(String fieldName, String regex) {
        this(fieldName, regex, false);
    }

    public CheckField(String fieldName, boolean matchAll) {
        this(fieldName, ".+", matchAll);
    }

    public CheckField(String fieldName, String regex, boolean matchAll) {
        super("Check field", true);
        this.fieldName = fieldName;
        this.regex = regex;
        this.matchAll = matchAll;
    }

    @Override
    public void run(Reader in, Writer out) throws RuleException {
        log("Checking " + (matchAll ? "ALL '" : "AT LEAST ONE '") + fieldName
                + "' match '" + regex + "'");
        try {
            AddDoc addDoc = AddDoc.read(in);
            List<Field> fields = addDoc.getFields(fieldName);
            int valid = 0;
            for (Field field : fields) {
                String value = field.getValue();
                boolean match = Pattern.matches(regex, field.getValue());
                if (match) {
                    valid++;
                    log("'" + value + "' matches");
                } else {
                    log("'" + value + "' does not match");
                }
            }
            int diff = fields.size() - valid;
            if (matchAll && diff > 0) {
                throw new RuleException("All " + fieldName
                        + " values must match " + regex);
            } else if (!matchAll && valid == 0) {
                throw new RuleException("At least one " + fieldName
                        + " value must match " + regex);
            } else {
                log("OK");
                addDoc.write(out);
            }
        } catch (JAXBException jaxbe) {
            throw new RuleException(jaxbe.getLinkedException());
        }
    }
}
