package com.googlecode.fascinator.portal.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.services.LanguageService;

/**
 * Service that will read in a set of Java properties files from a given
 * directory and then use them to resolve strings based on message codes
 * 
 * These can be called from your velocity templates using the #displayMessage
 * template
 * 
 * 
 * @author andrewbrazzatti
 * 
 */
public class JavaPropertiesLanguageService implements LanguageService {

    private Logger log = LoggerFactory
            .getLogger(JavaPropertiesLanguageService.class);

    private JsonSimple config;

    private List<Properties> propertiesFiles = new ArrayList<Properties>();

    @Override
    public JsonSimple getConfig() {
        return config;
    }

    @Override
    public void setConfig(JsonSimple config) {
        this.config = config;
    }

    @Override
    public void init() {
        log.debug("Initializing JavaPropertiesLanguageService...");
        File propertiesDir = new File(config.getString(null, "config",
                "propertiesDir"));
        if (!propertiesDir.exists()) {
            log.error("Can't find properties directory "
                    + propertiesDir.getPath());
            return;
        }

        File[] file = propertiesDir.listFiles();
        for (File propertyFile : file) {
            try {
                Properties properties = new Properties();

                properties.load(new FileInputStream(propertyFile));

                propertiesFiles.add(properties);
            } catch (Exception e) {
                log.error("Can't find properties properties file "
                        + propertyFile.getPath(), e);
            }
        }
    }

    @Override
    public String displayMessage(String messageCode, String region) {
        return displayMessage(messageCode);
    }

    @Override
    public String displayMessage(String messageCode) {
        for (Properties propertyFile : propertiesFiles) {
            if (propertyFile.get(messageCode) != null) {
                return (String) propertyFile.get(messageCode);
            }
        }

        return messageCode;
    }

}