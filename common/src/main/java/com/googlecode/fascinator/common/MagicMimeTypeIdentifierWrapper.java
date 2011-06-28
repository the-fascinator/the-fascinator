/* 
 * The Fascinator - Common Library - MIME Type Indentifier
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator.common;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.ontoware.rdf2go.model.node.URI;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the Aperture MIME type identifier with provision to add custom types
 * based on extension.
 * 
 * @author Oliver Lucido
 */
public class MagicMimeTypeIdentifierWrapper implements MimeTypeIdentifier {
    private Logger log = LoggerFactory.getLogger(
            MagicMimeTypeIdentifierWrapper.class);
    private JsonSimple mimeTypes;
    private MagicMimeTypeIdentifier identifier;

    public MagicMimeTypeIdentifierWrapper() {
        super();
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            mimeTypes = new JsonSimple(config.getObject("mime-types"));
            identifier = new MagicMimeTypeIdentifier();
        } catch (IOException e) {
            log.warn("Failed to load custom MIME types");
        }
    }

    @Override
    public int getMinArrayLength() {
        return identifier.getMinArrayLength();
    }

    @Override
    public String identify(byte[] firstBytes, String fileName, URI uri) {
        String ext = FilenameUtils.getExtension(fileName);
        String mimeType = mimeTypes.getString(null, ext);
        if (mimeType != null) {
            return mimeType;
        }
        return identifier.identify(firstBytes, fileName, uri);
    }
}
