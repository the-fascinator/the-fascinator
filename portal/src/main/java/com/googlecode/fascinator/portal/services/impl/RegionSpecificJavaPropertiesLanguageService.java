package com.googlecode.fascinator.portal.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
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
public class RegionSpecificJavaPropertiesLanguageService implements
        LanguageService {

    private Logger log = LoggerFactory
            .getLogger(RegionSpecificJavaPropertiesLanguageService.class);

    private JsonSimple config;

    private Map<String, Properties> propertiesFiles = new HashMap<String, Properties>();

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
        log.debug("Initializing RegionSpecificJavaPropertiesLanguageService ...");
        File propertiesDir = new File(config.getString(null, "config",
                "propertiesDir"));
        if (!propertiesDir.exists()) {
            log.error("Can't find properties directory "
                    + propertiesDir.getPath());
            return;
        }

        log.debug("Loading from {} ", propertiesDir.getPath());

        File[] file = propertiesDir.listFiles();
        for (File propertyFileDirectory : file) {
            if (propertyFileDirectory.isDirectory()) {
                String regionName = propertyFileDirectory.getName();
                Properties mergedProperties = new Properties();
                for (File propertyFile : propertyFileDirectory.listFiles()) {
                    try {
                        Properties properties = new Properties();
                        properties.load(new FileInputStream(propertyFile));
                        mergedProperties.putAll(properties);
                    } catch (Exception e) {
                        log.error("Can't find properties properties file "
                                + propertyFile.getPath(), e);
                    }
                }

                propertiesFiles.put(regionName, mergedProperties);
            }
        }
    }

    /*
     * display a message defined by messageCode and region
     */
    @Override
    public String displayMessage(String messageCode, String region) {
        Properties regionProperties = propertiesFiles.get(region);
        if (regionProperties != null
                && regionProperties.get(messageCode) != null) {
            return (String) regionProperties.get(messageCode);
        } else {
            regionProperties = propertiesFiles.get("default");
            if (regionProperties != null
                    && regionProperties.get(messageCode) != null) {
                return (String) regionProperties.get(messageCode);
            }
        }

        return messageCode;
    }

    /*
     * display a message defined by messageCode and from default
     */
    @Override
    public String displayMessage(String messageCode) {
        return displayMessage(messageCode, "default");
    }

}