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

import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test a variety of small media files against FFmpeg
 * 
 * @author Greg Pendlebury
 */
public class FfmpegTransformerTest {

    /** RAM Storage plugin */
    private Storage ram;

    /** FFmpeg Tranformer */
    private Transformer transformer;

    /** Level of execution available on this system */
    private String execLevel;

    /** Temp file */
    private File file;

    /** Configuration */
    private JsonSimpleConfig config;

    /** Input object */
    private DigitalObject inputObject;

    /** Output object */
    private DigitalObject outputObject;

    /**
     * Preparation to be performed before each test
     *
     * @throws Exception on failure
     */
    @Before
    public void setup() throws Exception {
        ram = PluginManager.getStorage("ram");
        ram.init("{}");

        // Read config
        InputStream in = getClass().getResourceAsStream("/ffmpeg-config.json");
        config = new JsonSimpleConfig(in);

        // Start the transformer
        transformer = new FfmpegTransformer();
        transformer.init(config.toString());

        // Test functionality level
        this.execTest();
    }

    /**
     * Cleanup to be performed after each test
     *
     * @throws Exception on failure
     */
    @After
    public void shutdown() throws Exception {
        if (ram != null) ram.shutdown();
        if (inputObject != null) inputObject.close();
        if (outputObject != null) outputObject.close();
        // Cleanup temp space
        if (transformer != null) transformer.shutdown();
        String path = config.getString(null, "outputPath");
        if (!delete(new File(path))) {
            throw new Exception("Delete failed, something is still locked!");
        }
    }

    /**
     * Recursively delete a file/directory
     *
     * @params file File object to delete
     * @return boolean: True if deleted, False if not
     */
    private boolean delete(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        return file.delete();
    }

    /**
     * Store and transform a resource
     *
     * @params resource to extract and process
     * @return DigitalObject after transformation
     * @throws Exception on failure
     */
    private DigitalObject transform(String resource) throws Exception {
        file = new File(getClass().getResource(resource).toURI());
        inputObject = StorageUtils.storeFile(ram, file);
        return transformer.transform(inputObject, "{}");
    }

    /**
     * Test for what level of execution FFmpeg can perform on this system
     *
     * @throws Exception on failure
     */
    private void execTest() throws Exception {
        String rawMetadata = transformer.getPluginDetails().getMetadata();
        JsonSimpleConfig metadata = new JsonSimpleConfig(rawMetadata);
        execLevel = metadata.getString(null, "debug", "availability");
    }

    /**
     * Tests:
     *  1) Transform an audio file without failure
     *  2) 3 Payloads should be present (original, metadata, preview)
     *  3) That the preview payload has been correctly typed
     *
     * @throws Exception on failure
     */
    @Test
    public void transformAudio() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: transformAudio()\n======\n");
        outputObject = transform("/african_drum.aif");

        // Should have 4 payloads
        Assert.assertEquals("There should be 4 Payloads", 4, outputObject
                .getPayloadIdList().size());

        // Should have a preview payload
        String preview = null;
        for (String i : outputObject.getPayloadIdList()) {
            Payload p = outputObject.getPayload(i);
            if (p.getType() == PayloadType.Preview) {
                preview = i;
            }
        }
        Assert.assertNotNull("Should have a Preview", preview);
    }

    /**
     * Tests:
     *  1) Fail to transform an invalid file without crashing
     *  2) Only the original payload should be present
     *
     * @throws Exception on failure
     */
    @Test
    public void invalidFfmpeg() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: invalidFfmpeg()\n======\n");
        outputObject = transform("/mol19.cml");

        // Should have only 2 payloads, invalid = no 'ffmpeg.info'
        Assert.assertEquals(2, outputObject.getPayloadIdList().size());
    }

    /**
     * Tests:
     *  1) Transform a JPG image without failure
     *  2) It should correctly find a duration of 0
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void jpgMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: jpgMetadata()\n======\n");
        outputObject = transform("/wheel.jpg");

        // Get extracted metadata
        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "120");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "width"),    "600");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "height"),   "800");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 42000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            // Verify the metadata we expected came out
            Assert.assertNull(metadata.getString(null,
                    "audio", "codec", "simple"));
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),         "image2");
            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "mjpeg");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "yuvj420p");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "1704");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "2272");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),                 "0");
        }
    }

    /**
     * Tests:
     *  1) Transform a PNG image without failure
     *  2) It should correctly find a duration of 0
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void pngMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: pngMetadata()\n======\n");
        outputObject = transform("/diagram.png");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "120");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "120");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "width"),    "600");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "height"),   "602");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.jpg", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 37000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertNull(metadata.getString(null,
                    "audio", "codec", "simple"));
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),         "image2");
            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "png");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "rgb24");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "643");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "645");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),                 "0");
        }
    }

    /**
     * Tests:
     *  1) Transform an M4V image without failure
     *  2) It should correctly find a duration of 85s
     *
     * If FFprobe is found
     *  3) Correct audio stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void m4vMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: m4vMetadata()\n======\n");
        outputObject = transform("/ipod.m4v");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "120");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "width"),    "298");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "height"),   "224");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 2400000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "85");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),         "mov,mp4,m4a,3gp,3g2,mj2");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),                 "85");

            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "h264");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "yuv420p");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "320");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "240");
            Assert.assertEquals(metadata.getString(null,
                    "video", "language"),        "eng");

            Assert.assertEquals(metadata.getString(null,
                    "audio", "codec", "simple"), "aac");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "sample_rate"),     "44100");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "channels"),        "2");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "language"),        "eng");
        }

        // Verify custom display type is set
        Properties props = outputObject.getMetadata();
        String displayType = props.getProperty("displayType");
        Assert.assertEquals(displayType, "video");
    }

    /**
     * Tests:
     *  1) Transform an MP4 video (from Apple) without failure
     *  2) It should correctly find a duration of 4s
     *
     * If FFprobe is found
     *  3) Correct audeo stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void qMp4Metadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: qMp4Metadata()\n======\n");
        outputObject = transform("/quicktime.mp4");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "70");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "width"),    "176");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "height"),   "224");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 250000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "4");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"), "mov,mp4,m4a,3gp,3g2,mj2");
            Assert.assertEquals(metadata.getString(null,
                    "duration"), "4");

            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "mpeg4");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "yuv420p");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "190");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "240");
            Assert.assertEquals(metadata.getString(null,
                    "video", "language"),        "eng");

            Assert.assertEquals(metadata.getString(null,
                    "audio", "codec", "simple"), "aac");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "sample_rate"),     "32000");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "channels"),        "2");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "language"),        "eng");
        }

        // Verify custom display type is set
        Properties props = outputObject.getMetadata();
        String displayType = props.getProperty("displayType");
        Assert.assertEquals(displayType, "video");
    }

    /**
     * Tests:
     *  1) Transform an MP4 video (without audio) without failure
     *  2) It should correctly find a duration of 109s
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void mp4Metadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: mp4Metadata()\n======\n");
        outputObject = transform("/nasa.mp4");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "160");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "width"),    "398");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "height"),   "224");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 2700000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "109");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"), "mov,mp4,m4a,3gp,3g2,mj2");
            Assert.assertEquals(metadata.getString(null,
                    "duration"), "109");

            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "mpeg4");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "yuv420p");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "380");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "214");
            Assert.assertEquals(metadata.getString(null,
                    "video", "language"),        "eng");

            // This video has no audio
            Assert.assertNull(metadata.getString(null,
                    "audio", "codec", "simple"));
        }

        // Verify custom display type is set
        Properties props = outputObject.getMetadata();
        String displayType = props.getProperty("displayType");
        Assert.assertEquals(displayType, "video");
    }

    /**
     * Tests:
     *  1) Transform an AIF audio file without failure
     *  2) It should correctly find a duration of 0
     *     (integer value of a 0.32s snippet)
     *
     * If FFprobe is found
     *  3) That no video stream was recorded
     *  4) Correct audio stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void aifMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: aifMetadata()\n======\n");
        outputObject = transform("/african_drum.aif");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.mp3", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 2700);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),         "aiff");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),                 "0");

            Assert.assertEquals(metadata.getString(null,
                    "audio", "codec", "simple"), "pcm_s24be");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "sample_rate"),     "44100");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "channels"),        "1");

            // Audio only
            Assert.assertNull(metadata.getString(null,
                    "video", "codec", "simple"));
        }
    }

    /**
     * Tests:
     *  1) Transform a MOV image without failure
     *  2) It should correctly find a duration of 5s
     *
     * If FFprobe is found
     *  3) Correct audio stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void movMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: movMetadata()\n======\n");
        outputObject = transform("/quicktime.mov");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "70");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "width"),    "176");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "height"),   "224");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 190000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "5");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"), "mov,mp4,m4a,3gp,3g2,mj2");
            Assert.assertEquals(metadata.getString(null,
                    "duration"), "5");

            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "svq1");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "yuv410p");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "190");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "240");
            Assert.assertEquals(metadata.getString(null,
                    "video", "language"),        "eng");

            Assert.assertEquals(metadata.getString(null,
                    "audio", "codec", "simple"), "qdm2");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "sample_rate"),     "22050");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "channels"),        "2");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "language"),        "eng");
        }

        // Verify custom display type is set
        Properties props = outputObject.getMetadata();
        String displayType = props.getProperty("displayType");
        Assert.assertEquals(displayType, "video");
    }

    /**
     * Tests:
     *  1) Transform an AVI with spaces in its name
     *
     * @throws Exception on failure
     */
    @Test
    public void spacesTest() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: spacesTest()\n======\n");
        outputObject = transform("/relay sample.avi");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // Check some output data
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "width"),  "112");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "height"), "90");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "width"),    "280");
        Assert.assertEquals(metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "height"),   "224");

        // And some rough file sizes (don't want to be too specific
        //  incase there are slight variations on systems)
        String tSize = metadata.getString(null,
                "outputs", "ffmpegThumbnail.jpg", "size");
        int thumbSize = Integer.valueOf(tSize);
        // Thumbnails in particular, just test for larger then zero, since we
        //   pick a random frame each execution.
        Assert.assertTrue(thumbSize > 0);
        String pSize = metadata.getString(null,
                "outputs", "ffmpegPreview.flv", "size");
        int previewSize = Integer.valueOf(pSize);
        Assert.assertTrue(previewSize > 250000);

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "10");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),         "avi");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),                 "10");

            Assert.assertEquals(metadata.getString(null,
                    "video", "codec", "simple"), "camtasia");
            Assert.assertEquals(metadata.getString(null,
                    "video", "pixel_format"),    "bgr24");
            Assert.assertEquals(metadata.getString(null,
                    "video", "width"),           "1280");
            Assert.assertEquals(metadata.getString(null,
                    "video", "height"),          "1024");

            Assert.assertEquals(metadata.getString(null,
                    "audio", "codec", "simple"), "pcm_s16le");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "sample_rate"),     "22050");
            Assert.assertEquals(metadata.getString(null,
                    "audio", "channels"),        "1");
        }

        // Verify custom display type is set
        Properties props = outputObject.getMetadata();
        String displayType = props.getProperty("displayType");
        Assert.assertEquals(displayType, "video");
    }

    /**
     * Tests:
     *  1) Two 4s AVIs will be merged into an 8s AVI
     *
     * @throws Exception on failure
     */
    @Test
    public void segmentsTest() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: segmentsTest()\n======\n");

        // Get the first segment and create an object
        inputObject = ram.createObject("segmentOid");
        StorageUtils.createOrUpdatePayload(inputObject, "segment0.avi",
                getClass().getResourceAsStream("/segment0.avi"));
        // Store the second segment and update properties
        StorageUtils.createOrUpdatePayload(inputObject, "segment1.avi",
                getClass().getResourceAsStream("/segment1.avi"));
        Properties props = inputObject.getMetadata();
        props.setProperty("mediaSegments", "2");

        // Transform the object
        outputObject = transformer.transform(inputObject, "{}");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonSimpleConfig metadata = new JsonSimpleConfig(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.getString(null, "duration"), "4");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.getString(null,
                    "format", "simple"),      "avi");
            Assert.assertEquals(metadata.getString(null,
                    "duration"),              "4");
        }
    }
}
