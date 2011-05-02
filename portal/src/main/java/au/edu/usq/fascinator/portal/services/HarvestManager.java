package au.edu.usq.fascinator.portal.services;

import java.io.IOException;
import java.util.Map;

import au.edu.usq.fascinator.portal.HarvestContent;

public interface HarvestManager {

    public Map<String, HarvestContent> getContents();

    public HarvestContent get(String name);

    public void add(HarvestContent portal);

    public void remove(String name);

    public void save(HarvestContent portal) throws IOException;

}
