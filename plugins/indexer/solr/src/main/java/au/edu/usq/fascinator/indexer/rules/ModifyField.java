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
package au.edu.usq.fascinator.indexer.rules;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.xml.bind.JAXBException;

import au.edu.usq.fascinator.api.indexer.rule.AddDoc;
import au.edu.usq.fascinator.api.indexer.rule.Field;
import au.edu.usq.fascinator.api.indexer.rule.Rule;
import au.edu.usq.fascinator.api.indexer.rule.RuleException;

public class ModifyField extends Rule {

    private String fieldName;

    private String regex;

    private String replacement;

    public ModifyField(String fieldName, String replacement) {
        this(fieldName, null, replacement);
    }

    public ModifyField(String fieldName, String regex, String replacement) {
        super("Modify field", false);
        this.fieldName = fieldName;
        this.regex = regex;
        this.replacement = replacement;
    }

    @Override
    public void run(Reader in, Writer out) throws RuleException {
        if (regex == null) {
            log("Changing all '" + fieldName + "' to value '" + replacement
                    + "'");
        } else {
            log("Modifying '" + fieldName + "' matching '" + regex
                    + "' with replacement '" + replacement + "'");
        }
        try {
            AddDoc addDoc = AddDoc.read(in);
            List<Field> fields = addDoc.getFields(fieldName);
            for (Field field : fields) {
                if (regex == null) {
                    field.setValue(replacement);
                } else {
                    String value = field.getValue();
                    String newValue = value.replaceAll(regex, replacement);
                    if (value.equals(newValue)) {
                        log("Value '" + value + "' was unmodified");
                    } else {
                        field.setValue(newValue);
                        log("Modified value '" + value + "' to '" + newValue
                                + "'");
                    }
                }
            }
            addDoc.write(out);
        } catch (JAXBException jaxbe) {
            throw new RuleException(jaxbe.getLinkedException());
        }
    }
}
