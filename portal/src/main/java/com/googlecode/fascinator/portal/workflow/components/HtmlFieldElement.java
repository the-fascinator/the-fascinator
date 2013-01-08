package com.googlecode.fascinator.portal.workflow.components;

import java.util.HashMap;
import java.util.Map;

public class HtmlFieldElement implements HtmlComponent {

    private String componentTemplateName = null;

    private Map<String, String> parameterMap = new HashMap<String, String>();

    public String getComponentTemplateName() {
        return componentTemplateName;
    }

    public void setComponentTemplateName(String componentTemplateName) {
        this.componentTemplateName = componentTemplateName;
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

}
