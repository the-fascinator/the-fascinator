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

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.googlecode.fascinator.api.indexer.rule.Rule;
import com.googlecode.fascinator.api.indexer.rule.RuleException;

public class XslTransform extends Rule {

    private Transformer transformer;

    public XslTransform(InputStream xsl) throws RuleException {
        this(xsl, null);
    }

    public XslTransform(InputStream xsl, Map<String, String> params)
            throws RuleException {
        super("XSLT transform", true);
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Templates t = tf.newTemplates(new StreamSource(xsl));
            transformer = t.newTransformer();
            if (params != null) {
                for (Entry<String, String> entry : params.entrySet()) {
                    transformer.setParameter(entry.getKey(), entry.getValue());
                }
            }
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
            throw new RuleException("Failed to load stylesheet", tce);
        }
    }

    @Override
    public void run(Reader in, Writer out) throws RuleException {
        Source source = new StreamSource(in);
        Result result = new StreamResult(out);
        try {
            transformer.transform(source, result);
        } catch (TransformerException te) {
            throw new RuleException("Failed to transform", te);
        }
    }
}
