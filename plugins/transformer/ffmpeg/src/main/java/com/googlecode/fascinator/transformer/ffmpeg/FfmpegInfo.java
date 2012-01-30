/*
 * The Fascinator - Plugin - Transformer - FFMPEG
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
package com.googlecode.fascinator.transformer.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

/**
 * Extract metadata about a media file
 * 
 * @author Oliver Lucido
 */
public class FfmpegInfo {

    /** Logger */
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(FfmpegInfo.class);

    /** Supported file format */
    private boolean supported = true;

    /** Audio flag */
    private boolean audio = false;

    /** Video flag */
    private boolean video = false;

    /** Raw output */
    private String rawMediaData;

    /** Processed output */
    private String metadata;

    /** Media duration */
    private int duration;

    /** Media width */
    private int width;

    /** Media height */
    private int height;

    /** Format data */
    private JsonObject format = new JsonObject();

    /** Raw stream data */
    private List<JsonObject> streams = new ArrayList<JsonObject>();

    /** Processed video stream data */
    private JsonObject videoStream = new JsonObject();

    /** Processed video stream data */
    private JsonObject audioStream = new JsonObject();

    /**
     * Extract metadata from the given file using the provided FFmpeg object
     * 
     * @param ffmpeg implementation to used for extract
     * @param inputFile to extract metadata from
     * @throws IOException if execution failed
     */
    public FfmpegInfo(Ffmpeg ffmpeg, File inputFile) throws IOException {
        // Process and grab output
        rawMediaData = ffmpeg.extract(inputFile);
        if (rawMediaData == null || rawMediaData.length() == 0) {
            supported = false;
            return;
        }

        // log.debug("\n\n=====\n\\/ \\/\n\n{}\n/\\ /\\\n=====\n",
        // rawMediaData);

        // FFprobe
        if (ffmpeg.testAvailability().equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            parseFFprobeMetadata(rawMediaData);
            processFFprobeMetadata();

            // FFmpeg
        } else {
            parseFFmpegMetadata(rawMediaData);
            JsonObject mData = new JsonObject();
            mData.put("duration", "" + duration);
            metadata = mData.toString();
        }
    }

    /**
     * Get and clean a value from the raw output
     * 
     * @param json raw output
     * @param path where the data should be stored
     * @return String containing the cleaned output
     */
    private String getCleanValue(JsonObject json, String path) {
        String result = new JsonSimple(json).getString(null, path);
        if (result != null) {
            result = result.trim();
        }
        return result;
    }

    /**
     * Parse raw output from FFprobe into object properties
     * 
     * @param rawMetaData to parse
     */
    private void parseFFprobeMetadata(String rawMetaData) {
        JsonObject stream = null;
        int eq;

        // Parse the output from FFprobe
        for (String line : rawMetaData.split("\r\n|\r|\n")) {
            // Section wrappers
            if (line.equals("[STREAM]")) {
                stream = new JsonObject();
                continue;
            }
            if (line.equals("[/STREAM]")) {
                streams.add(stream);
                stream = null;
                continue;
            }
            if (line.equals("[FORMAT]") || line.equals("[/FORMAT]")) {
                continue;
            }

            // Tags
            if (line.startsWith("TAG:")) {
                line = "tags/" + line.substring(4);
            }

            // File the data
            eq = line.indexOf("=");
            if (eq != -1) {
                // Make sure spaces in the key are removed
                String key = line.substring(0, eq).replace(" ", "_");
                String value = line.substring(eq + 1);

                if (stream == null) {
                    format.put(key, value);
                } else {
                    stream.put(key, value);
                }
            }
        }
    }

    /**
     * Parse raw output from FFmpeg into object properties
     * 
     * @param rawMetaData to parse
     */
    private void parseFFmpegMetadata(String rawMetaData) {
        // Check if supported
        if (supported = (rawMetaData.indexOf(": Unknown format") == -1)) {
            // get size
            width = 0;
            height = 0;
            Pattern p = Pattern.compile(", ((\\d+)x(\\d+))(,)* ");
            Matcher m = p.matcher(rawMetaData);
            if (m.find()) {
                width = Integer.valueOf(m.group(2));
                height = Integer.valueOf(m.group(3));
            }
            // get duration
            p = Pattern.compile("Duration: ((\\d+):(\\d+):(\\d+))");
            m = p.matcher(rawMetaData);
            if (m.find()) {
                long hrs = Long.parseLong(m.group(2)) * 3600;
                long min = Long.parseLong(m.group(3)) * 60;
                long sec = Long.parseLong(m.group(4));
                duration = Long.valueOf(hrs + min + sec).intValue();
            }
            // check for video
            video = Pattern.compile("Stream #.*Video:.*").matcher(rawMetaData)
                    .find();
            // check for audio
            audio = Pattern.compile("Stream #.*Audio:.*").matcher(rawMetaData)
                    .find();
        }
    }

    /**
     * Process parsed output from object properties into a return value
     * 
     */
    private void processFFprobeMetadata() {
        getPrimaryStreams();
        JsonObject mData = new JsonObject();

        // log.debug("\n========\nFORMAT:\n\n{}\n", format.toString());
        // for (JsonConfigHelper stream : streams) {
        // log.debug("\n========\nSTREAM:\n\n{}\n", stream.toString());
        // }

        // Duration
        String dString = getCleanValue(format, "duration");
        mData.put("duration_float", dString);
        duration = Float.valueOf(dString).intValue();
        mData.put("duration", duration);

        // Generic format data
        JsonObject formatData = new JsonObject();
        formatData.put("simple", getCleanValue(format, "format_name"));
        formatData.put("label", getCleanValue(format, "format_long_name"));
        mData.put("format", formatData);

        // Decode Video
        width = 0;
        height = 0;
        if (videoStream != null) {
            JsonObject videoData = new JsonObject();
            String codec = getCleanValue(videoStream, "codec_name");
            if (codec != null) {
                video = true;
            }

            // Language, two options
            String lang = getCleanValue(videoStream, "language");
            if (lang == null) {
                lang = getCleanValue(videoStream, "tags/language");
            }
            videoData.put("language", lang);

            // Dimensions
            String widthStr = getCleanValue(videoStream, "width");
            if (widthStr != null) {
                width = Integer.valueOf(widthStr);
                videoData.put("width", widthStr);
            }
            String heightStr = getCleanValue(videoStream, "height");
            if (heightStr != null) {
                height = Integer.valueOf(heightStr);
                videoData.put("height", heightStr);
            }

            // Codec
            JsonObject videoCodec = new JsonObject();
            videoCodec.put("tag", getCleanValue(videoStream, "codec_tag"));
            videoCodec.put("tag_string",
                    getCleanValue(videoStream, "codec_tag_string"));
            videoCodec.put("simple", getCleanValue(videoStream, "codec_name"));
            videoCodec.put("label",
                    getCleanValue(videoStream, "codec_long_name"));
            videoData.put("codec", videoCodec);
            videoData
                    .put("pixel_format", getCleanValue(videoStream, "pix_fmt"));

            // Add video to metadata
            mData.put("video", videoData);
        }

        // Decode Audio
        if (audioStream != null) {
            JsonObject audioData = new JsonObject();
            String codec = getCleanValue(audioStream, "codec_name");
            if (codec != null) {
                audio = true;
            }

            // Language, two options
            String lang = getCleanValue(audioStream, "language");
            if (lang == null) {
                lang = getCleanValue(audioStream, "tags/language");
            }
            audioData.put("language", lang);

            // Codec
            JsonObject audioCodec = new JsonObject();
            audioCodec.put("tag", getCleanValue(audioStream, "codec_tag"));
            audioCodec.put("tag_string",
                    getCleanValue(audioStream, "codec_tag_string"));
            audioCodec.put("simple", getCleanValue(audioStream, "codec_name"));
            audioCodec.put("label",
                    getCleanValue(audioStream, "codec_long_name"));
            audioData.put("codec", audioCodec);

            // Sample rate
            String sample_rate = getCleanValue(audioStream, "sample_rate");
            if (sample_rate != null) {
                audioData.put("sample_rate", Float.valueOf(sample_rate)
                        .intValue());
            }
            // Channels
            audioData.put("channels", getCleanValue(audioStream, "channels"));

            // Add audio to metadata
            mData.put("audio", audioData);
        }
        metadata = mData.toString();
    }

    /**
     * Process raw stream data from object properties into primary video/audio
     * streams
     * 
     */
    private void getPrimaryStreams() {
        for (JsonObject stream : streams) {
            String type = (String) stream.get("codec_type");
            if (type == null) {
                continue;
            }

            // The highest index video stream should be considered primary
            if (type.equals("video") && videoStream != null) {
                videoStream = stream;
            }
            // The highest index audio stream should be considered primary
            if (type.equals("audio") && audioStream != null) {
                audioStream = stream;
            }
        }
    }

    /**
     * Return the raw ouput that came from the binary
     * 
     * @return String containing the raw output
     */
    public String getRaw() {
        return rawMediaData;
    }

    /**
     * Return the duration of the media
     * 
     * @return int duration in seconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Return the duration of the media
     * 
     * @return int duration in seconds
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the duration of the media
     * 
     * @return int duration in seconds
     */
    public int getHeight() {
        return height;
    }

    /**
     * Is the file format supported?
     * 
     * @return boolean flag if supported
     */
    public boolean isSupported() {
        return supported;
    }

    /**
     * Does the media have audio?
     * 
     * @return boolean if media has audio
     */
    public boolean hasAudio() {
        return audio;
    }

    /**
     * Does the media have video?
     * 
     * @return boolean if media has video
     */
    public boolean hasVideo() {
        return video;
    }

    /**
     * Return the processed metadata as a string
     * 
     * @return String containing the processed output
     */
    @Override
    public String toString() {
        return metadata;
    }
}
