/*
 * The Fascinator - GUI File Uploader
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
package com.googlecode.fascinator.portal.guitoolkit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * Present a file upload interface to the user for ingesting into various
 * harvest plugins.
 * 
 * @author Greg Pendlebury
 */
public class GUIFileUploader {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(GUIFileUploader.class);

    private GUIFormRenderer fr;
    private Map<String, String> harvesters;

    /**
     * Constructor. Access to system configuration and a list of the user's
     * security roles is required.
     * 
     * @param config : System configuration
     * @param user_roles : An array with the list of roles the current user has
     */
    public GUIFileUploader(JsonSimpleConfig config, List<String> user_roles) {
        // Init our form renderer
        fr = new GUIFormRenderer(config);

        // Get our workflow config
        JsonObject object = config.getObject("uploader");
        Map<String, JsonSimple> workflows = JsonSimple.toJavaMap(object);

        harvesters = new LinkedHashMap<String, String>();
        // Foreach workflow
        for (String workflow : workflows.keySet()) {
            // Check this user is allowed to upload files for it
            if (workflows.get(workflow).getString("", "upload-template")
                    .equals("")) {
                for (Object role : workflows.get(workflow).getArray("security")) {
                    if (user_roles.contains(role.toString())) {
                        // Add it to the list
                        harvesters.put(workflow, workflows.get(workflow)
                                .getString(null, "screen-label"));
                    }
                }
            }
        }
    }

    /**
     * Render the form required for file uploads.
     * 
     * @return String : The rendered form
     */
    public String renderForm() {
        if (harvesters.isEmpty()) {
            return "Sorry, but your current security permissions don't"
                    + " allow for file uploading.";
        }

        String form_string = ""
                + "<form enctype='multipart/form-data' id='upload-file'"
                + " method='post' action='workflow'>\n"
                + "<fieldset class='login'>\n"
                + "<legend>File Upload</legend>\n"
                + fr.ajaxFluidErrorHolder("upload-file")
                + "<p>\n"
                + fr.renderFormElement("upload-file-file", "file",
                        "Select a file to upload:")
                + "</p>\n"
                + "<p>\n"
                + fr.renderFormSelect("upload-file-workflow",
                        "Select the harvester to process the file:", harvesters)
                + "</p>\n"
                + "<div class='center'>"
                + fr.renderFormElement("upload-file-submit", "button", null,
                        "Upload") + fr.ajaxProgressLoader("upload-file")
                + "</div>" +

                /* A real, ajax driven progess bar has been cut since
                 * Tapestry doesn't support setProgressListener().
                "<div id='upload-progress' class='hidden'>" +
                  "<div id='upload-progress-number'></div>" +
                  "<div class='upload-progress-holder'>" +
                    "<div id='upload-progress-filler'>&nbsp;</div>" +
                  "</div>" +
                "</div>" +
                 */

                "</fieldset>\n" + "</form>\n";

        return form_string;
    }
}
