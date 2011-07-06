from com.googlecode.fascinator.api import PluginManager
from com.googlecode.fascinator.common import JsonSimpleConfig
from com.googlecode.fascinator.portal import Portal

from java.io import File
from java.util import HashMap

class SettingsData:

    def __init__(self):
        self.sysConfig = JsonSimpleConfig()

    def __activate__(self, context):
        self.velocityContext = context

        self.__portal = None
        action = self.vc("formData").get("verb")
        if action == "create_view":
            fq = [q for q in self.vc("sessionState").get("fq") if q != 'item_type:"object"']
            if fq == []:
                name = "new"
                desc = "New View"
                query = ""
            else:
                name = ""
                desc = ""
                query = str(" ".join(fq))
            newPortal = Portal(name, desc, query)
            newPortal.setFacetFields(Services.portalManager.default.facetFields)
            newPortal.setQuery(query)
            self.__portal = newPortal
        else:
            portalName = self.vc("formData").get("portalName")
            print " * settings.py: portalName=%s, portalId=%s" % (portalName, self.vc("portalId"))
            if portalName is None or (self.vc("formData").get("portalAction") == "Cancel"):
                self.__portal = Services.portalManager.get(self.vc("portalId"))
            else:
                self.__portal = Portal()
                self.__portal.name = portalName
                Services.portalManager.add(self.__portal)
            if self.vc("formData").get("portalAction") == "Update":
                self.__updatePortal()
            if self.vc("formData").get("emailAction") == "Update":
                self.__updateEmail()
    
    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def isSelected(self, category):
        selected = self.vc("sessionState").get("settingsCategory")
        if category == selected:
            return "selected"
        return ""
    
    def __updatePortal(self):
        self.__portal.name = self.vc("formData").get("portalName")
        self.__portal.description = self.vc("formData").get("portalDescription")
        self.__portal.query = self.vc("formData").get("portalQuery")
        self.__portal.recordsPerPage = int(self.vc("formData").get("portalRecordsPerPage"))
        self.__portal.facetCount = int(self.vc("formData").get("portalFacetLimit"))
        self.__portal.facetSort = self.vc("formData").get("portalFacetSort") is not None
        facetFields = self.__portal.facetFields
        facetFields.clear()
        size = int(self.vc("formData").get("portalFacetSize"))
        for i in range(1,size+2):
            nameKey = "portalFacet_%s_name" % i
            labelKey = "portalFacet_%s_label" % i
            name = self.vc("formData").get(nameKey)
            label = self.vc("formData").get(labelKey)
            print "facet: key: %s, label: %s" % (name, label)
            if name is not None and label is not None:
                facetFields.put(name, label)
        self.__portal.setFacetFields(facetFields)
        sortFields = self.__portal.sortFields
        sortFields.clear()
        size = int(self.vc("formData").get("portalSortSize"))
        for i in range(1,size+2):
            nameKey = "portalSort_%s_name" % i
            labelKey = "portalSort_%s_label" % i
            name = self.vc("formData").get(nameKey)
            label = self.vc("formData").get(labelKey)
            print "sort: key: %s, label: %s" % (name, label)
            if name is not None and label is not None:
                sortFields.put(name, label)
        self.__portal.setSortFields(sortFields)
        Services.portalManager.save(self.__portal)
    
    def getPortal(self):
        return self.__portal
    
    def getIndexerPlugins(self):
        return PluginManager.getIndexerPlugins()
    
    def getStoragePlugins(self):
        return PluginManager.getStoragePlugins()
    
    def getHarvesterPlugins(self):
        return PluginManager.getHarvesterPlugins()
    
    def getTransformerPlugins(self):
        return PluginManager.getTransformerPlugins()
    
    def getWatcherConfig(self):
        watcherPath = self.sysConfig.getString("${fascinator.home}/watcher)", ["watcher", "path"])
        configFile = File("%s/app/config.json" % watcherPath)
        if configFile.exists():
            return JsonSimpleConfig(configFile)
        return None
    
    def getEmail(self):
        return self.sysConfig.getString(None, ["email"])
    
    def getTimeout(self):
        return self.sysConfig.getString(None, ["portal", "houseKeeping", "config", "frequency"])
    
    def getFacetDisplays(self):
        facetDisplays = self.__portal.getObject(["portal", "facet-displays"])
        if facetDisplays is None or facetDisplays.isEmpty():
            facetDisplays = HashMap()
            facetDisplays.put("list", "List menu")
            facetDisplays.put("tree", "Dynamic tree")
        return facetDisplays
