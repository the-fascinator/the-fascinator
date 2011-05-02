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
package au.edu.usq.fascinator.api.transformer;

import au.edu.usq.fascinator.api.Plugin;
import au.edu.usq.fascinator.api.storage.DigitalObject;

/**
 * Provides transformation or enrichment services for digital objects
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public interface Transformer extends Plugin {

    /**
     * Transforms a digital object. This method should return the original if no
     * transformation occurred.
     * 
     * @param object the object to transformed
     * @param config transformation configuration
     * @return the transformed object
     * @throws TransformerException if an error occurred during transformation
     */
    public DigitalObject transform(DigitalObject object, String config)
            throws TransformerException;

}
