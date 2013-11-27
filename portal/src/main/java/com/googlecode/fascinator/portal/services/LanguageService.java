package com.googlecode.fascinator.portal.services;

public interface LanguageService extends FascinatorService {

    public abstract String displayMessage(String messageCode);

    public abstract String displayMessage(String messageCode, String region);

}