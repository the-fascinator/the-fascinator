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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for the FFMPEG program
 * 
 * @author Oliver Lucido
 */
public class FfmpegImpl implements Ffmpeg {

    /** Logger */
    private Logger log = LoggerFactory.getLogger(FfmpegImpl.class);

    /** Flag for metadata extraction */
    private boolean extraction;

    /** Transcoding binary */
    private String executable;

    /** Extraction binary */
    private String metadata;

    /** Level of available functionality */
    private String availability = "Unknown";

    /** Environment variables */
    private Map<String, String> env;

    /**
     * Instantiate using default binaries
     *
     */
    public FfmpegImpl() {
        this(DEFAULT_BIN_TRANSCODE, DEFAULT_BIN_METADATA);
    }

    /**
     * Instantiate using provided binaries (if found)
     *
     * @param executable Binary for transcoding
     * @param metadata Binary for metadata extraction
     */
    public FfmpegImpl(String executable, String metadata) {
        this.extraction = false;
        this.executable = executable == null ? DEFAULT_BIN_TRANSCODE : executable;
        this.metadata   = metadata   == null ? DEFAULT_BIN_METADATA  : metadata;
        env = new HashMap();
    }

    /**
     * What level of functionality is available from FFmpeg on this system
     *
     * @return String indicating the level of available functionality
     */
    @Override
    public String testAvailability() {
        // Cache our response
        if (availability != null && availability.equals("Unknown")) {
            // Test our install
            boolean ffprobe = testExtractor();
            boolean ffmpeg  = testConverter();

            if (ffprobe && ffmpeg) {
                // A recent FFmpeg build is installed
                availability = DEFAULT_BIN_METADATA;
            } else {
                if (ffmpeg) {
                    // An older FFmpeg build is installed
                    availability = DEFAULT_BIN_TRANSCODE;
                    this.metadata = this.executable;
                } else {
                    // We couldn't find an intall
                    availability = null;
                }
            }
        }

        return availability;
    }

    /**
     * Test for the existance of either the provided
     * or default metadata extractor (ffprobe)
     *
     * @return boolean Flag if it was found
     */
    private boolean testExtractor() {
        // Highest level of functionality
        // DEFAULT_EXTRACTER = FFprobe
        if (this.metadata.contains(DEFAULT_BIN_METADATA)) {
            extraction = true;
            try {
                execute();
                extraction = false;
                return true;

            // Try searching for it on the path
            } catch (IOException ioe) {
                File found = searchPathForExecutable(DEFAULT_BIN_METADATA);
                if (found != null) {
                    metadata = found.getAbsolutePath();
                    log.info("FFprobe found at {}", metadata);
                    try {
                        execute();
                        extraction = false;
                        return true;
                    } catch (IOException ioe2) {
                        log.error("FFprobe not functioning correctly!");
                    }
                }
            }
        }
        extraction = false;
        return false;
    }

    /**
     * Test for the existance of either the provided
     * or default transcoder (ffmpeg)
     *
     * @return boolean Flag if it was found
     */
    private boolean testConverter() {
        // DEFAULT_EXECUTABLE = FFmpeg
        if (this.executable.contains(DEFAULT_BIN_TRANSCODE)) {
            try {
                execute();
                return true;

            // Try searching for it on the path
            } catch (IOException ioe) {
                File found = searchPathForExecutable(DEFAULT_BIN_TRANSCODE);
                if (found != null) {
                    executable = found.getAbsolutePath();
                    log.info("FFmpeg found at {}", executable);
                    try {
                        execute();
                        return true;
                    } catch (IOException ioe2) {
                        log.error("FFmpeg not functioning correctly!");
                    }
                }
            }
        }
        return false;
    }

    /**
     * Execute the binary without parameters to test
     *  for existance
     *
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process execute() throws IOException {
        List<String> noParams = Collections.emptyList();
        return execute(noParams, null);
    }

    /**
     * Execute that binary with provided parameters
     *
     * @param params to execute with
     * @param location A File object of the working directory to use during transcoding
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process execute(List<String> params, File location)
            throws IOException {
        List<String> cmd = new ArrayList<String>();
        if (extraction) {
            cmd.add(metadata);
        } else {
            cmd.add(executable);
        }
        cmd.addAll(params);
        log.debug("Executing: {}", cmd);
        //for (String param : params) {
        //    log.debug("PARAM: {}", param);
        //}
        return createProcess(cmd, location).start();
    }

    /**
     * Create and return a new process, making sure any custom environment
     * variables have been set.
     *
     * @param commands : The list of commands to execute in the new process
     * @param location A File object of the working directory to use during transcoding
     * @return ProcessBuilder : The process to be executed
     * @throws IOException if execution failed
     */
    private ProcessBuilder createProcess(List<String> commands, File location) {
        // Very simple... nothing to do
        if (env.isEmpty()) {
            return new ProcessBuilder(commands);
        }

        // Otherwise, add our custom data
        ProcessBuilder process = new ProcessBuilder(commands);
        // Working directory
        if (location != null) {
            process.directory(location);
        }
        // Environment
        Map<String, String> currentEnv = process.environment();
        for (String key : env.keySet()) {
            currentEnv.put(key, env.get(key));
        }
        // Then return
        return process;
    }

    /**
     * Wait for the completion of the provided Process and
     *  stream its output to the provided location.
     *
     * @param proc Process to wait on
     * @param out Stream to route output
     * @return Process that was executed
     */
    private Process waitFor(Process proc, OutputStream out) {
        if (extraction && metadata.equals("ffprobe")) {
            // FFprobe, we're reading from STDOUT
            new InputHandler("stdout", proc.getInputStream(), out).start();
            new InputGobbler("stderr", proc.getErrorStream()).start();
        } else {
            // FFmpeg, we're reading from STDERR
            new InputHandler("stderr", proc.getErrorStream(), out).start();
            new InputGobbler("stdout", proc.getInputStream()).start();
        }
        try {
            // Wait for execution to finish
            proc.waitFor();
            // We need a tiny pause on Solaris
            //  or consecutive calls will fail
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            log.error("ffmpeg was interrupted!", ie);
            proc.destroy();
        }
        return proc;
    }

    /**
     * Start a process with the provided parameters,
     *  wait for its completion and route the output
     *  to the provided stream
     *
     * @param params List of parameters to provide the process
     * @param out Stream to route output
     * @param location A File object of the working directory to use during transcoding
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process executeAndWait(List<String> params, OutputStream out,
            File location) throws IOException {
        return waitFor(execute(params, location), out);
    }

    /**
     * Extract metadata from the given file
     *
     * @param file to extract metadata from
     * @return String containing the raw output
     * @throws IOException if execution failed
     */
    @Override
    public String extract(File inputFile) throws IOException {
        if (testAvailability() == null) {
            return null;
        }

        this.extraction = true;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> params = new ArrayList<String>();

        // FFprobe
        if (testAvailability().equals(DEFAULT_BIN_METADATA)) {
            params.add("-show_format");
            params.add("-show_streams");
        } else {
            params.add("-i");
        }

        params.add(inputFile.getAbsolutePath());
        executeAndWait(params, out, null);
        this.extraction = false;
        return out.toString("UTF-8");
    }

    /**
     * Transform a file using the provided parameters
     *
     * @param params List of parameters to provide the binary
     * @param location A File object of the working directory to use during transcoding
     * @return String containing the raw output
     * @throws IOException if execution failed
     */
    @Override
    public String transform(List<String> params, File location)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        executeAndWait(params, out, location);
        String response = out.toString("UTF-8");
        if (response.contains("Compile-time maximum width")) {
            throw new IOException("Maximum resolution exceeded!\n=====\n" + response);
        }
        return response;
    }

    /**
     * Extract and process metadata from the given file
     *
     * @param file to extract metadata from
     * @return FfmpegInfo containing the processed output
     * @throws IOException if execution failed
     */
    @Override
    public FfmpegInfo getInfo(File inputFile) throws IOException {
        return new FfmpegInfo(this, inputFile);
    }

    /**
     * Search along the system path for an executable with
     * the provided name
     *
     * @param name of the exectuable to find
     * @return File on disk is found, null if not found
     */
    private File searchPathForExecutable(String name) {
        if (System.getProperty("os.name").startsWith("Windows")
                && !name.endsWith(".exe")) {
            name += ".exe";
        }
        String[] dirs = System.getenv("PATH").split(File.pathSeparator);
        for (String dir : dirs) {
            File file = new File(dir, name);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Set an environment variable to be made available to the executing FFmpeg
     *
     * @param key : The name of environment variable
     * @param value : The value to assign
     */
    @Override
    public void setEnvironmentVariable(String key, String value) {
        env.put(key, value);
    }
}
