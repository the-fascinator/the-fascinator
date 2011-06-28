/*
 * The Fascinator - Plugin - Transformer - FFMPEG
 * Copyright (C) 2010 University of Southern Queensland
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
package com.googlecode.fascinator.transformer.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputHandler extends Thread {

    private InputStream in;

    private OutputStream out;

    public InputHandler(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } catch (Throwable t) {
        } finally {
            try {
                out.flush();
            } catch (IOException e) {
            }
        }
    }
}
