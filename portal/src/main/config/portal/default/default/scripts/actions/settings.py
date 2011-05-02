from au.edu.usq.fascinator.common import JsonSimpleConfig, JsonSimple, JsonObject
from java.io import FileWriter
from org.apache.commons.lang import StringUtils

class SettingsData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        self.writer = self.vc("response").getPrintWriter("text/html; charset=UTF-8")

        if self.vc("page").authentication.is_logged_in() and self.vc("page").authentication.is_admin():
            self.process()
        else:
            print " * settings.py : AJAX : Unauthorised access"
            self.throw_error("Only administrative users can access this feature")

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getWatcherFile(self):
        configFile = FascinatorHome.getPathFile("watcher/config.json")
        if configFile.exists():
            return JsonSimpleConfig(configFile)
        return None

    def process(self):
        print " * settings.py: formData=%s" % self.vc("formData")

        result = "{}"
        portalManager = Services.getPortalManager()
        portal = portalManager.get(self.vc("portalId"))
        func = self.vc("formData").get("func")

        if func == "view-update":
            portal.setDescription(self.vc("formData").get("view-description"))
            portal.setQuery(self.vc("formData").get("view-query"))
            portal.setSearchQuery(self.vc("formData").get("view-search-query"))
            portal.setRecordsPerPage(int(self.vc("formData").get("view-records-per-page")))
            portal.setFacetCount(int(self.vc("formData").get("view-facet-count")))
            portal.setFacetDisplay(int(self.vc("formData").get("view-facet-display")))
            portal.setFacetSort(self.vc("formData").get("view-facet-sort") is not None)
            portalManager.save(portal)

        elif func == "general-update":
            config = JsonSimpleConfig()
            email = StringUtils.trimToEmpty(self.vc("formData").get("general-email"))
            systemEmail = StringUtils.trimToEmpty(config.getString(None, ["email"]))
            if systemEmail != email:
                obj = config.writableSystemConfig()
                obj.put("email", self.vc("formData").get("general-email"))
                obj.put("configured", "true")
                config.storeSystemConfig()
                # mark restart
                Services.getHouseKeepingManager().requestUrgentRestart()
            else:
                print " * settings.py: email not updated: did not change"
                self.throw_error("Email address is the same! No change saved.")

        elif func == "facets-update":
            portal.getObject(["portal"]).remove("facet-fields")
            fields = self.vc("formData").getValues("field")
            labels = self.vc("formData").getValues("label")
            displays = self.vc("formData").getValues("display")
            deletes = self.vc("formData").getValues("delete")
            for i in range(0, len(fields)):
                field = fields[i]
                if deletes[i] == "false":
                    node = portal.writeObject(["portal", "facet-fields", field])
                    node.put("label", labels[i])
                    node.put("display", displays[i])
            portalManager.save(portal)

        elif func == "sort-update":
            portal.getObject(["portal"]).remove("sort-fields")
            fields = self.vc("formData").getValues("field")
            labels = self.vc("formData").getValues("label")
            deletes = self.vc("formData").getValues("delete")
            for i in range(0, len(fields)):
                field = fields[i]
                if deletes[i] == "false":
                    node = portal.writeObject(["portal", "sort-fields"])
                    node.put(field, labels[i])
            portalManager.save(portal)

        elif func == "watcher-update":
            configFile = self.getWatcherFile()
            if configFile is not None:
                json = JsonSimpleConfig(configFile)
                pathIds = self.vc("formData").get("pathIds").split(",")
                actives = self.vc("formData").getValues("watcher-active")
                if actives is None:
                    actives = []
                deletes = self.vc("formData").getValues("watcher-delete")
                if deletes is None:
                    deletes = []
                for pathId in pathIds:
                    if pathId not in deletes:
                        path = self.vc("formData").get("%s-path" % pathId)
                        stopped = str(pathId not in actives).lower()
                        watchDir = json.writeObject(["watcher", "watchDirs", path])
                        watchDir.put("ignoreFileFilter", self.vc("formData").get("%s-file" % pathId))
                        watchDir.put("ignoreDirectories", self.vc("formData").get("%s-dir" % pathId))
                        watchDir.put("stopped", stopped)
                        json.writeArray(["watcher", "watchDirs", path, "cxtTags"])
                writer = FileWriter(configFile)
                writer.write(json.toString(True))
                writer.close()
            else:
                result = "The Watcher is not installed properly."

        elif func == "restore-default-config":
            # backup the file
            JsonSimpleConfig.backupSystemFile()
            # delete the file
            JsonSimpleConfig.getSystemFile().delete()
            # restore default
            JsonSimpleConfig.getSystemFile()
            # mark restart
            Services.getHouseKeepingManager().requestUrgentRestart()

        elif func == "housekeeping-update":
            config = JsonSimpleConfig()
            freq = StringUtils.trimToEmpty(self.vc("formData").get("housekeeping-timeout"))
            systemFreq = StringUtils.trimToEmpty(config.getString(None, ["portal", "houseKeeping", "config", "frequency"]))
            result = "House Keeper refreshed"
            if systemFreq != freq:
                # Get writeable access to underlying system
                sysConfig = JsonSimple(config.writableSystemConfig())
                # Modify the house keeping node
                hkConfig = sysConfig.writeObject(["portal", "houseKeeping", "config"])
                hkConfig.put("frequency", freq)
                # Write the underlying config back to disk
                config.storeSystemConfig()
                result = "Frequency updated, refreshing House Keeper"
            # Refresh the HouseKeeper
            message = JsonObject()
            message.put("type", "refresh")
            Services.getHouseKeepingManager().sendMessage(message.toString())

        self.writer.println(result)
        self.writer.close()

    def throw_error(self, message):
        self.vc("response").setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()
