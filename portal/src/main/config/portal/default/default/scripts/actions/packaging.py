import uuid

from au.edu.usq.fascinator import HarvestClient
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import FascinatorHome, JsonSimpleConfig, Manifest
from au.edu.usq.fascinator.common.storage import StorageUtils

from java.io import File, FileOutputStream, OutputStreamWriter
from java.lang import Exception

from org.apache.commons.io import FileUtils, IOUtils

class PackagingData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        #self.vc("log").debug("formData = {}", self.vc("formData"))

        result = "{}"
        func = self.vc("formData").get("func")
        if func == "create-new":
            result = self.__createNew()
        elif func == "create-from-selected":
            result = self.__createFromSelected()
        elif func == "update":
            result = self.__update()
        elif func == "deselect":
            result = self.__deselect()
        elif func == "clear":
            result = self.__clear()
        elif func == "modify":
            result = self.__modify()

        writer = self.vc("response").getPrintWriter("application/json; charset=UTF-8")
        writer.println(result)
        writer.close()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __createNew(self):
        self.vc("log").debug("Creating a new package...")
        packageType, jsonConfigFile = self.__getPackageTypeAndJsonConfigFile()
        #self.vc("log").debug("packageType = '{}'", packageType)
        #self.vc("log").debug("jsonConfigFile = '{}'", jsonConfigFile)

        manifestHash = "%s.tfpackage" % uuid.uuid4()
        # store the manifest file for harvesting
        packageDir = FascinatorHome.getPathFile("packages")
        packageDir.mkdirs()
        manifestFile = File(packageDir, manifestHash)
        outStream = FileOutputStream(manifestFile)
        outWriter = OutputStreamWriter(outStream, "UTF-8")

        self.vc("sessionState").set("package/active", None)
        manifest = self.__getActiveManifest()
        manifest.setType(packageType)
        metaList = list(self.vc("formData").getValues("metaList"))
        jsonObj = manifest.getJsonObject()
        for metaName in metaList:
            value = self.vc("formData").get(metaName)
            jsonObj.put(metaName, value)

        outWriter.write(manifest.toString(True))
        outWriter.close()

        try:
            # harvest the package as an object
            username = self.vc("sessionState").get("username")
            if username is None:
                username = "guest" # necessary?
            harvester = None
            # set up config files, creating if necessary
            workflowsDir = FascinatorHome.getPathFile("harvest/workflows")
            configFile = self.__getFile(workflowsDir, jsonConfigFile)
            self.__getFile(workflowsDir, "packaging-rules.py")
            # run the harvest client with our packaging workflow config
            harvester = HarvestClient(configFile, manifestFile, username)
            harvester.start()
            manifestId = harvester.getUploadOid()
            harvester.shutdown()
        except Exception, ex:
            error = "Packager workflow failed: %s" % str(ex)
            log.error(error, ex)
            if harvester is not None:
                harvester.shutdown()
            return '{ "status": "failed" }'
        # clean up
        self.__deselect()
        # return url to workflow screen
        return '{"status": "ok", "url": "%s/workflow/%s" }' % (self.vc("portalPath"), manifestId)

    def __createFromSelected(self):
        self.vc("log").debug("Creating package from selected...")
        packageType, jsonConfigFile = self.__getPackageTypeAndJsonConfigFile()
        #self.vc("log").debug("packageType = '{}'", packageType)
        #self.vc("log").debug("jsonConfigFile = '{}'", jsonConfigFile)

        # if modifying existing manifest, we already have an identifier,
        # otherwise create a new one
        manifestId = self.__getActiveManifestId()
        if manifestId is None:
            manifestHash = "%s.tfpackage" % uuid.uuid4()
        else:
            manifestHash = self.__getActiveManifestPid()

        # store the manifest file for harvesting
        packageDir = FascinatorHome.getPathFile("packages")
        packageDir.mkdirs()
        manifestFile = File(packageDir, manifestHash)
        outStream = FileOutputStream(manifestFile)
        outWriter = OutputStreamWriter(outStream, "UTF-8")
        manifest = self.__getActiveManifest()
        oldType = manifest.getType()
        if oldType is None:
            manifest.setType(packageType)
        else:
            manifest.setType(oldType)

        #self.vc("log").debug("Manifest: {}", manifest)
        outWriter.write(manifest.toString(True))
        outWriter.close()

        try:
            if manifestId is None:
                # harvest the package as an object
                username = self.vc("sessionState").get("username")
                if username is None:
                    username = "guest" # necessary?
                harvester = None
                # set up config files, and make sure they are both deployed
                workflowsDir = FascinatorHome.getPathFile("harvest/workflows")
                configFile = self.__getFile(workflowsDir, jsonConfigFile)
                rulesFile = self.__getFile(workflowsDir, "packaging-rules.py")
                # run the harvest client with our packaging workflow config
                harvester = HarvestClient(configFile, manifestFile, username)
                harvester.start()
                manifestId = harvester.getUploadOid()
                harvester.shutdown()
            else:
                # update existing object
                object = StorageUtils.getDigitalObject(Services.getStorage(), manifestId)
                manifestStream = FileUtils.openInputStream(manifestFile)
                StorageUtils.createOrUpdatePayload(object, manifestHash, manifestStream)
                manifestStream.close()
                object.close()
        except Exception, ex:
            error = "Packager workflow failed: %s" % str(ex)
            log.error(error, ex)
            if harvester is not None:
                harvester.shutdown()
            return '{ "status": "failed" }'
        # clean up
        ##manifestFile.delete()
        self.__deselect()
        # return url to workflow screen
        return '{ "status": "ok", "url": "%s/workflow/%s" }' % (self.vc("portalPath"), manifestId)

    def __update(self):
        self.vc("log").debug("Updating package selection...")
        activeManifest = self.__getActiveManifest()
        added = self.vc("formData").getValues("added")
        if added:
            titles = self.vc("formData").getValues("titles")
            for i in range(len(added)):
                id = added[i]
                title = titles[i]
                node = activeManifest.getNode("node-%s" % id)
                if node is None:
                    self.vc("log").debug("adding: '{}', '{}'", id, title.encode("UTF-8"))
                    activeManifest.addTopNode(id, title)
                else:
                    self.vc("log").debug("'{}' already in manifest", id)
        removed = self.vc("formData").getValues("removed")
        if removed:
            for id in removed:
                node = activeManifest.getNode("node-%s" % id)
                if node is not None:
                    self.vc("log").debug("removing: '{}'", id)
                    activeManifest.delete("node-%s" % id)
        #self.vc("log").debug("activeManifest: {}", activeManifest)
        return '{ "count": %s }' % activeManifest.size()

    def __deselect(self):
        self.vc("log").debug("Clearing package selection...")
        self.vc("sessionState").remove("package/active")
        self.vc("sessionState").remove("package/active/id")
        self.vc("sessionState").remove("package/active/pid")
        return "{}"

    def __clear(self):
        self.vc("log").debug("Removing all nodes from manifest...")
        activeManifest = self.__getActiveManifest()
        nodeList = activeManifest.getTopNodes()
        for node in nodeList:
            activeManifest.delete(node.getKey())
        return "{}"

    def __modify(self):
        self.vc("log").debug("Set active package...")
        oid = self.vc("formData").get("oid")
        try:
            object = Services.getStorage().getObject(oid)
            sourceId = object.getSourceId()
            payload = object.getPayload(sourceId)
            manifest = Manifest(payload.open())
            payload.close()
            object.close()
            self.vc("sessionState").set("package/active", manifest)
            self.vc("sessionState").set("package/active/id", oid)
            self.vc("sessionState").set("package/active/pid", sourceId)
        except StorageException, e:
            self.vc("response").setStatus(500)
            return '{ error: %s }' % str(e)
        return '{ "count": %s }' % manifest.size()

    def __getPackageTypeAndJsonConfigFile(self):
        try:
            packageType = self.vc("formData").get("packageType", "default")
            if packageType == "":
                packageType = "default"
            types = JsonSimpleConfig().getJsonSimpleMap(["portal", "packageTypes"])
            pt = None
            if types is not None and not types.isEmpty():
                pt = types.get(packageType)
            if pt is None:
                configFile = "packaging-config.json"
            else:
                configFile = pt.getString("packaging-config.json", ["jsonconfig"])
        except Exception, e:
            configFile = "packaging-config.json"
        return (packageType, configFile)

    def __getActiveManifestId(self):
        return self.vc("sessionState").get("package/active/id")

    def __getActiveManifestPid(self):
        return self.vc("sessionState").get("package/active/pid")

    def __getActiveManifest(self):
        activeManifest = self.vc("sessionState").get("package/active")
        if not activeManifest:
            activeManifest = Manifest(None)
            activeManifest.setTitle("New package")
            activeManifest.setViewId(self.vc("portalId"))
            self.vc("sessionState").set("package/active", activeManifest)
        return activeManifest

    def __getFile(self, packageDir, filename):
        file = File(packageDir, filename)
        if not file.exists():
            out = FileOutputStream(file)
            IOUtils.copy(Services.getClass().getResourceAsStream("/workflows/" + filename), out)
            out.close()
        return file
