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
package au.edu.usq.fascinator.transformer.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Wrapper for the FFMPEG program
 * 
 * @author Oliver Lucido
 */
public interface Ffmpeg {

    /** Default transcoder binary name */
    public static final String DEFAULT_BIN_TRANSCODE = "ffmpeg";

    /** Default extractor binary name */
    public static final String DEFAULT_BIN_METADATA = "ffprobe";

    /**
     * Test what level of functionality is available for ffmpeg
     *  on the current system.
     *
     * @return String showing binary name or null
     */
    public String testAvailability();

    /**
     * Extract metadata from the given file
     *
     * @param file to extract metadata from
     * @return String containing the raw output
     * @throws IOException if execution failed
     */
    public String extract(File file) throws IOException;

    /**
     * Transform a file using given parameters
     *
     * @param params List of parameters to pass to the command line executable
     * @param location A File object of the working directory to use during transcoding
     * @return String containing the raw output
     * @throws IOException if execution failed
     */
    public String transform(List<String> params, File location) throws IOException;

    /**
     * Extract and process metadata from the given file
     *
     * @param file to extract metadata from
     * @return FfmpegInfo containing the processed output
     * @throws IOException if execution failed
     */
    public FfmpegInfo getInfo(File inputFile) throws IOException;

    /**
     * Set an environment variable to be made available to the executing FFmpeg
     *
     * @param key : The name of environment variable
     * @param value : The value to assign
     */
    public void setEnvironmentVariable(String key, String value);
}
