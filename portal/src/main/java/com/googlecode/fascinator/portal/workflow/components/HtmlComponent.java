package com.googlecode.fascinator.portal.workflow.components;

import java.util.Map;

public interface HtmlComponent {

    public abstract String getComponentTemplateName();

    public abstract void setComponentTemplateName(String componentTemplateName);

    public abstract Map<String, Object> getParameterMap();

    public abstract void setParameterMap(Map<String, Object> parameterMap);

}