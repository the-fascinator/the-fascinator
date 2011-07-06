/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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
package com.googlecode.fascinator.common.sax;

import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;

/**
 * A custom SAX parser to preserve escaped entities
 * 
 * Credit due :
 * http://asfak.wordpress.com/2009/08/29/escaping-or-unescaping-special-
 * characters-while-writing-xml-files-using-dom4j/
 * 
 * @author Greg Pendlebury
 */

public class SafeSAXParser extends SAXParser {

    /** Name of the entity */
    private String entityName;

    /**
     * Character content
     * 
     * @param text The content
     * @param augs Additional information that may include infoset augmentations
     * @throws XNIException Thrown by handler to signal an error
     */
    @Override
    public void characters(XMLString text, Augmentations augs)
            throws XNIException {
        if (entityName != null) {
            char[] charArray = entityName.toCharArray();
            text.setValues(charArray, 0, charArray.length);
            entityName = null;
        }
        super.characters(text, augs);
    }

    /**
     * This method notifies of the start of an entity.
     * 
     * @param name - The name of the entity.
     * @param identifier - The resource identifier.
     * @param encoding - The auto-detected IANA encoding name of the entity
     *            stream.
     * @param augs - Additional information that may include infoset
     *            augmentations
     * @throws XNIException Thrown by handler to signal an error
     */
    @Override
    public void startGeneralEntity(String name,
            XMLResourceIdentifier identifier, String encoding,
            Augmentations augs) throws XNIException {
        super.startGeneralEntity(name, identifier, encoding, augs);
        entityName = "&" + name + ";";
    }
}