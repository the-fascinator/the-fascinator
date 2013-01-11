package com.googlecode.fascinator.portal.workflow.components;

import java.util.HashMap;
import java.util.Map;

public class HtmlFieldElement implements HtmlComponent {

    private String componentTemplateName = null;

    private Map<String, Object> parameterMap = new HashMap<String, Object>();

    public String getComponentTemplateName() {
        return componentTemplateName;
    }

    public void setComponentTemplateName(String componentTemplateName) {
        this.componentTemplateName = componentTemplateName;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap = parameterMap;
    }

}
