package com.googlecode.fascinator.portal.workflow.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlDiv implements HtmlComponent {

    private List<HtmlFieldElement> htmlFieldElements = new ArrayList<HtmlFieldElement>();

    private String componentTemplateName = "default-div";
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

    public void addHtmlFieldElement(HtmlFieldElement htmlFieldElement) {
        htmlFieldElements.add(htmlFieldElement);

    }

    public List<HtmlFieldElement> getHtmlFieldElements() {
        return htmlFieldElements;
    }

}
