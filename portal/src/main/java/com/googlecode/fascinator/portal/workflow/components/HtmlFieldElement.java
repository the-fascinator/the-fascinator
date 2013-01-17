package com.googlecode.fascinator.portal.workflow.components;

import java.util.HashMap;
import java.util.Map;

public class HtmlFieldElement implements HtmlComponent {

    private String componentTemplateName = null;
    private Object[] validation = null;

    private Map<String, Object> parameterMap = new HashMap<String, Object>();

    public String getComponentTemplateName() {
        return componentTemplateName;
    }

    public void setComponentTemplateName(String componentTemplateName) {
        this.componentTemplateName = componentTemplateName;
    }

    public Object[] getValidation() {
        return validation;
    }

    public void setValidation(Object[] validation) {
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
