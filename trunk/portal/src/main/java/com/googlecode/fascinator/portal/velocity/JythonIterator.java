/* 
 * The Fascinator - Portal
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
package com.googlecode.fascinator.portal.velocity;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.python.core.PySequence;

public class JythonIterator implements Iterator<Object> {

    private PySequence seq;

    private int pos;

    private int size;

    public JythonIterator(PySequence sequence) {
        seq = sequence;
        pos = 0;
        size = sequence.__len__();
    }

    @Override
    public Object next() {
        if (hasNext()) {
            return seq.__getitem__(pos++);
        }
        throw new NoSuchElementException("No more elements: " + pos + " / "
                + size);
    }

    @Override
    public boolean hasNext() {
        return pos < size;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
