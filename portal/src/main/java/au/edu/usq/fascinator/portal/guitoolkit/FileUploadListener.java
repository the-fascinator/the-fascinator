/*
 * The Fascinator - File Upload Listener
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.guitoolkit;

import org.apache.commons.fileupload.ProgressListener;

/**
 * <p>
 * A listener object to be used in ajax queries for
 * updating upload progress bars.
 * <p>
 *
 * <strong>Currently this should be considered deprecated code. We found it not
 * possible to integrate with Tapestry in the time scoped for this task. It may
 * not be possible at all.</strong>
 *
 * @author Greg Pendlebury
 */
public class FileUploadListener implements ProgressListener {
    private volatile long
            bytesRead = 0L,
            contentLength = 0L,
            item = 0L;

    public FileUploadListener() {

        super();
    }

    @Override
    public void update(long aBytesRead, long aContentLength, int anItem) {
        bytesRead = aBytesRead;
        contentLength = aContentLength;
        item = anItem;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getItem() {
        return item;
    }
}
