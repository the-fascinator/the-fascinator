package au.edu.usq.fascinator.portal;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class HarvestContent extends JsonSimpleConfig {

    private File jsonFile;

    public HarvestContent(File jsonFile) throws IOException {
        super(jsonFile);
        this.jsonFile = jsonFile;
    }

    public String getId() {
        return FilenameUtils.getBaseName(jsonFile.getName());
    }

    public String getDescription() {
        return getString(null, "content", "description");
    }

    public Map<String, Object> getIndexerParams() {
        Map<String, Object> response = new LinkedHashMap();
        JsonObject object = getObject("indexer", "params");
        for (Object key : object.keySet()) {
            response.put(key.toString(), object.get(key));
        }
        return response;
    }

    public String getIndexerParam(String name) {
        return getIndexerParams().get(name).toString();
    }

    public void setIndexerParam(String name, String value) {
        JsonObject object = getObject("indexer", "params");
        if (object != null) {
            object.put(name, value);
        }
    }

    public File getRulesFile() {
        return new File(getString(null, "indexer", "script", "rules"));
    }

    public Harvester getHarvester() {
        Storage storage = PluginManager.getStorage(
                getString(null, "storage", "type"));
        Harvester harvester = PluginManager.getHarvester(
                getString(null, "harvester", "type"), storage);
        if (harvester != null) {
            try {
                harvester.init(jsonFile);
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }
        return harvester;
    }
}
