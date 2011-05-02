/*
 * The Fascinator - GUI Form Renderer
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

import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.util.Map;

/**
 * Render web form elements based on descriptive metadata.
 * 
 * @author Greg Pendlebury
 */
public class GUIFormRenderer {

    /**
     * Constructor. Access to system configuration is required
     *
     * @param config : System configuration
     */
    public GUIFormRenderer(JsonSimpleConfig config) {
    }

    /**
     * Render an error message text container.
     *
     * @param prefix : A String to prepend to element IDs
     * @return String : The html for the element
     */
    public String ajaxFluidErrorHolder(String prefix) {
        return "<div class='stop-error hidden' id='" + prefix + "-error'>\n"
                + "<span id='" + prefix + "-message'></span>\n" + "</div>\n";
    }

    /**
     * Render a circular loading animation.
     *
     * @param prefix : A String to prepend to element IDs
     * @return String : The html for the element
     */
    public String ajaxFluidLoader(String prefix) {
        return "<img class='hidden' id='" + prefix
                + "-loading' src='images/icons/loading.gif' alt='Loading'/>\n";
    }

    /**
     * Render a horizontal progress loading animation.
     *
     * @param prefix : A String to prepend to element IDs
     * @return String : The html for the element
     */
    public String ajaxProgressLoader(String prefix) {
        return "<img class='hidden' id='" + prefix + "-loading'" +
                " src='images/loading-progress.gif' alt='Loading'/>\n";
    }

    /**
     * Render a simple form 'input' element with optional 'label'.
     *
     * @param name : Value of the 'name' attribute
     * @param type : Value of the 'type' attribute
     * @param label : Label text
     * @return String : The html for the element
     */
    public String renderFormElement(String name, String type, String label) {
        return renderFormElement(name, type, label, "");
    }

    /**
     * Render a simple form 'input' element with optional 'label' and a
     * starting value.
     *
     * @param name : Value of the 'name' attribute
     * @param type : Value of the 'type' attribute
     * @param label : Label text
     * @param value : The starting text for the element value
     * @return String : The html for the element
     */
    public String renderFormElement(String name, String type, String label,
            String value) {
        String element = "";
        if (label != null && !label.equals("")) {
            element += "<label for='" + name + "'>" + label + "</label>\n";
        }
        element += "<input type='" + type + "' id='" + name + "' name='" + name
                + "'";
        if (value != null && !value.equals("")) {
            element += " value='" + value + "'";
        }
        element += "/>\n";
        return element;
    }

    /**
     * Render a 'select' drop-down element with optional 'label'.
     *
     * @param name : Value of the 'name' attribute
     * @param label : Label text
     * @param values : A Map of values of label for the drop-down.
     * @return String : The html for the element
     */
    public String renderFormSelect(String name, String label,
            Map<String, String> values) {
        String select = "";
        if (label != null && !label.equals("")) {
            select += "<label for='" + name + "'>" + label + "</label>\n";
        }
        select += "<select id='" + name + "' name='" + name + "'>\n";
        for (String plugins : values.keySet()) {
            select += "<option value='" + plugins + "'>" + values.get(plugins)
                    + "</option>\n";
        }
        select += "</select>\n";
        return select;
    }
}
