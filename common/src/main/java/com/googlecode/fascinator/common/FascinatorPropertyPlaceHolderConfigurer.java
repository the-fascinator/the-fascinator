/*
 * The Fascinator - FascinatorPropertyPlaceHolderConfigurer
 * Copyright (C) 2008-2010 University of Southern Queensland
 * Copyright (C) 2012 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.common;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Allows the use of configuration in the Fascinator system-config.json file in
 * the Spring application-context.
 * 
 * Nested attributes are mapped in dot notation.
 * 
 * e.g. { "config": { "configItem": "sample config" } } becomes
 * config.configItem=sample config in the properties configuration
 * 
 * @author andrewqcif
 * 
 */
public class FascinatorPropertyPlaceHolderConfigurer extends
        PropertyPlaceholderConfigurer {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            Properties configProperties = mapJsonToProperties(config);
            addConfigPropertiesToLocalPropertiesArray(configProperties);
            Properties mergedProps = mergeProperties();

            // Convert the merged properties, if necessary.
            convertProperties(mergedProps);

            // Let the subclass process the properties.
            processProperties(beanFactory, mergedProps);
        } catch (IOException ex) {
            throw new BeanInitializationException("Could not load properties",
                    ex);
        }

    }

    /**
     * Add our system-config.json based properties to the list of properties to
     * be merged. This needs to be done via reflection as the property is
     * private in the original Spring impl.
     * 
     * @param configProperties system-config.json based properties
     */
    private void addConfigPropertiesToLocalPropertiesArray(
            Properties configProperties) {
        try {
            Field localPropertiesField = this.getClass().getSuperclass()
                    .getSuperclass().getSuperclass()
                    .getDeclaredField("localProperties");
            localPropertiesField.setAccessible(true);

            Properties[] localProperties = (Properties[]) localPropertiesField
                    .get(this);
            List<Properties> propertyList = new ArrayList<Properties>();
            if (localProperties != null) {
                propertyList = Arrays.asList(localProperties);
            }
            propertyList.add(0, configProperties);

            localPropertiesField.set(this,
                    propertyList.toArray(new Properties[] {}));
        } catch (NoSuchFieldException e) {
            throw new BeanInitializationException("Could not load properties",
                    e);
        } catch (SecurityException e) {
            throw new BeanInitializationException("Could not load properties",
                    e);
        } catch (IllegalArgumentException e) {
            throw new BeanInitializationException("Could not load properties",
                    e);
        } catch (IllegalAccessException e) {
            throw new BeanInitializationException("Could not load properties",
                    e);
        }

    }

    /**
     * Map the system-config.json to a properties file. Nested attributes are
     * mapped in dot notation.
     * 
     * e.g. { "config": { "configItem": "sample config" } } becomes
     * config.configItem=sample config in the properties configuration
     * 
     * 
     * @param config
     * @return
     */
    private Properties mapJsonToProperties(JsonSimpleConfig config) {
        Properties properties = new Properties();
        JsonObject obj = config.getJsonObject();
        for (Object o : obj.keySet()) {
            String key = (String) o;
            Object value = obj.get(key);
            if (value instanceof String) {
                properties.put(key, value);
            } else if (value instanceof JsonObject) {
                processJsonObjectToProperties((JsonObject) value, key,
                        properties);
            }
        }
        return properties;
    }

    private void processJsonObjectToProperties(JsonObject jsonObject,
            String parentKey, Properties properties) {
        for (Object o : jsonObject.keySet()) {
            String key = (String) o;
            Object value = jsonObject.get(key);
            if (value instanceof String) {
                properties.put(parentKey + "." + key, value);
            } else if (value instanceof JsonObject) {
                processJsonObjectToProperties((JsonObject) value, parentKey
                        + "." + key, properties);
            }
        }

    }
}
