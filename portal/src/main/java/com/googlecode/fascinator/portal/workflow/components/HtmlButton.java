package com.googlecode.fascinator.portal.workflow.components;

import java.util.HashMap;
import java.util.Map;

public class HtmlButton implements HtmlComponent {
    private String componentTemplateName = null;

    private Map<String, String> parameterMap = new HashMap<String, String>();

    /* (non-Javadoc)
     * @see com.googlecode.fascinator.portal.workflow.components.HtmlComponent#getComponentTemplateName()
     */
    @Override
    public String getComponentTemplateName() {
        return componentTemplateName;
    }

    /* (non-Javadoc)
     * @see com.googlecode.fascinator.portal.workflow.components.HtmlComponent#setComponentTemplateName(java.lang.String)
     */
    @Override
    public void setComponentTemplateName(String componentTemplateName) {
        this.componentTemplateName = componentTemplateName;
    }

    /* (non-Javadoc)
     * @see com.googlecode.fascinator.portal.workflow.components.HtmlComponent#getParameterMap()
     */
    @Override
    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

    /* (non-Javadoc)
     * @see com.googlecode.fascinator.portal.workflow.components.HtmlComponent#setParameterMap(java.util.Map)
     */
    @Override
    public void setParameterMap(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }
}
