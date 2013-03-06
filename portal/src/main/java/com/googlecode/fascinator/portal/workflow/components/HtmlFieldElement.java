package com.googlecode.fascinator.portal.workflow.components;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.fascinator.common.JsonObject;

public class HtmlFieldElement implements HtmlComponent {

    private String componentTemplateName = null;
    private JsonObject validation = null;

    private Map<String, Object> parameterMap = new HashMap<String, Object>();

    public String getComponentTemplateName() {
        return componentTemplateName;
    }

    public void setComponentTemplateName(String componentTemplateName) {
        this.componentTemplateName = componentTemplateName;
    }

    public JsonObject getValidation() {
        return validation;
    }

    public void setValidation(JsonObject validation) {
        this.validation = validation;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public boolean hasValidation() {
        return validation != null;
    }

}
