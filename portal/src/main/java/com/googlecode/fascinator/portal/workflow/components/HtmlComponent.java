package com.googlecode.fascinator.portal.workflow.components;

import java.util.Map;

public interface HtmlComponent {

    public abstract String getComponentTemplateName();

    public abstract void setComponentTemplateName(String componentTemplateName);

    public abstract Map<String, String> getParameterMap();

    public abstract void setParameterMap(Map<String, String> parameterMap);

}