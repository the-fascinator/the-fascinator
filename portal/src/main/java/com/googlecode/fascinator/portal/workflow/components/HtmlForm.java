package com.googlecode.fascinator.portal.workflow.components;

import java.util.ArrayList;
import java.util.List;

public class HtmlForm {

    private List<HtmlFieldElement> htmlFieldElements = new ArrayList<HtmlFieldElement>();
    private List<HtmlButton> htmlButtons = new ArrayList<HtmlButton>();
    private List<HtmlDiv> htmlDivs = new ArrayList<HtmlDiv>();
    private String htmlFooter = null;

    public void addHtmlFieldElement(HtmlFieldElement htmlFieldElement) {
        htmlFieldElements.add(htmlFieldElement);
    }

    public List<HtmlFieldElement> getHtmlFieldElements() {
        return htmlFieldElements;
    }

    public void addHtmlButton(HtmlButton htmlButton) {
        htmlButtons.add(htmlButton);
    }

    public List<HtmlButton> getHtmlButtons() {
        return htmlButtons;
    }

    public void addHtmlDiv(HtmlDiv htmlDiv) {
        htmlDivs.add(htmlDiv);

    }

    public List<HtmlDiv> getHtmlDivs() {
        return htmlDivs;
    }

    public String getHtmlFooter() {
        return htmlFooter;
    }

    public void setHtmlFooter(String htmlFooter) {
        this.htmlFooter = htmlFooter;
    }

}
