package com.googlecode.fascinator.portal.services;

import java.io.IOException;
import java.util.Map;

import com.googlecode.fascinator.portal.HarvestContent;

public interface HarvestManager {

    public Map<String, HarvestContent> getContents();

    public HarvestContent get(String name);

    public void add(HarvestContent portal);

    public void remove(String name);

    public void save(HarvestContent portal) throws IOException;

}
