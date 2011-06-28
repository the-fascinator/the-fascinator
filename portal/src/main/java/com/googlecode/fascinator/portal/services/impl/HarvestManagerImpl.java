package com.googlecode.fascinator.portal.services.impl;

import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.portal.HarvestContent;
import com.googlecode.fascinator.portal.services.HarvestManager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarvestManagerImpl implements HarvestManager {

    private Logger log = LoggerFactory.getLogger(HarvestManagerImpl.class);

    private File contentDir;

    private Map<String, HarvestContent> contentMap;

    public HarvestManagerImpl() {
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            contentDir = new File(
                    config.getString(null, "portal", "contentDir"));
            load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, HarvestContent> getContents() {
        if (contentMap == null) {
            contentMap = new HashMap<String, HarvestContent>();
        }
        return contentMap;
    }

    @Override
    public void add(HarvestContent content) {
        getContents().put(content.getId(), content);
    }

    @Override
    public HarvestContent get(String id) {
        return getContents().get(id);
    }

    @Override
    public void remove(String id) {
        HarvestContent content = getContents().remove(id);
        getContents().remove(id);

        // TODO remove .json and .py files
    }

    @Override
    public void save(HarvestContent content) throws IOException {
        FileWriter writer = new FileWriter(
                new File(contentDir, content.getId()));
        writer.write(content.toString());
        writer.close();
    }

    private void load() {
        File[] contentFiles = contentDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();
                return file.isDirectory() || name.endsWith(".json");
            }
        });
        for (File contentFile : contentFiles) {
            log.debug("Found content file: {}", contentFile);
            try {
                add(new HarvestContent(contentFile));
            } catch (IOException ioe) {
                // TODO Auto-generated catch block
                ioe.printStackTrace();
            }
        }
    }

}
