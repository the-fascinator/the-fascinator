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
package com.googlecode.fascinator.api.storage;

/**
 * Types of payloads
 * 
 * @author Oliver Lucido
 */
public enum PayloadType {

    /** Descriptions of an object */
    Annotation,

    /** Content derived from another source */
    Enrichment,

    /** Optional payload type that harvesters and transformers
        can use to provide error feedback in the portal */
    Error,

    /** Similar to Enrichment, but specifically designated
        as the single payload to use for preview display */
    Preview,

    /** Original or source content */
    Source,

    /** Alternative Preview(s). Similar to Preview, but there
        can be more than one and they are not the primary
        preview */
    AltPreview,

    /** Thumbnail image version of an object */
    Thumbnail

}
