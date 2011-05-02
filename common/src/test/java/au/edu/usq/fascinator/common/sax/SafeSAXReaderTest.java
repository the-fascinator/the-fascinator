/*
 * The Fascinator - Common Library
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
package au.edu.usq.fascinator.common.sax;

import java.io.ByteArrayOutputStream;
import junit.framework.Assert;
import org.dom4j.Document;
import org.junit.Test;

/**
 * Unit tests for SafeSAXReader
 *
 * @author Greg Pendlebury
 */
public class SafeSAXReaderTest {

    @Test
    public void basicXML() throws Exception {
        String xml = getClass().getResource("/basic.xml").toString();
        SafeSAXReader reader = new SafeSAXReader();
        Document doc = reader.loadDocument(xml);

        String xmlStr = "<someNode><sub>test</sub></someNode>";
        Assert.assertEquals(reader.docToString(doc), xmlStr);
    }

    @Test
    public void escapedXML() throws Exception {
        String xml = getClass().getResource("/escaped.xml").toString();
        SafeSAXReader reader = new SafeSAXReader();
        Document doc = reader.loadDocument(xml);

        String xmlStr = "<someNode><sub>'single quotation' \"double quotation\" Apostrophe' Apostrophe&apos; &quot;Quotation&quot; A&amp;B</sub></someNode>";
        Assert.assertEquals(reader.docToString(doc), xmlStr);
    }

    @Test
    public void streamOutput() throws Exception {
        String xml = getClass().getResource("/basic.xml").toString();
        SafeSAXReader reader = new SafeSAXReader();
        Document doc = reader.loadDocument(xml);

        String xmlStr = "<someNode><sub>test</sub></someNode>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reader.docToStream(doc, out);

        Assert.assertEquals(out.toString(), xmlStr);
    }
}
