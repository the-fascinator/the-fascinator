from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import Manifest, JsonObject

from java.lang import Exception
from java.util import ArrayList, HashMap

from org.apache.commons.lang import StringEscapeUtils

class OrganiserData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        #print "formData: %s" % self.vc("formData")
        self.__oid = self.vc("formData").get("oid")
        result = None
        try:
            # get the package manifest
            self.__manifest = self.__readManifest(self.__oid)
            # check if we need to do processing
            func = self.vc("formData").get("func")
            if func == "get-rvt-manifest":
                result = self.__getRvtManifest(self.getManifest())
                #print "Result: ", result
        except Exception, e:
            log.error("Failed to load manifest", e);
            result = '{ status: "error", message: "%s" }' % str(e)

        if result is not None:
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

    def getManifest(self):
        return self.__manifest

    def getFormData(self, field):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get(field, ""))

    def getPackageTitle(self):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get("title", self.__manifest.getTitle()))

    def getMeta(self, metaName):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get(metaName, self.__manifest.getString(None, [metaName])))

    def getManifestViewId(self):
        searchPortal = self.__manifest.getViewId()
        if searchPortal is None:
            searchPortal = defaultPortal
        if Services.portalManager.exists(searchPortal):
            return searchPortal
        else:
            return defaultPortal

    def getMimeType(self, oid):
        return self.__getContentType(oid) or ""

    def getMimeTypeIcon(self, path, oid, altText = None):
        format = self.__getContentType(oid)
        return self.__getMimeTypeIcon(path, format, altText)

    def __getMimeTypeIcon(self, path, format, altText = None):
        if format[-1:] == ".":
            format = format[0:-1]
        if altText is None:
            altText = format
        # check for specific icon
        iconPath = "images/icons/mimetype/%s/icon.png" % format
        resource = Services.getPageService().resourceExists(self.vc("portalId"), iconPath)
        if resource is not None:
            return "<img class=\"mime-type\" src=\"%s/%s\" title=\"%s\" alt=\"%s\" />" % (path, iconPath, altText, altText)
        elif format.find("/") != -1:
            # check for major type
            return self.__getMimeTypeIcon(path, format.split("/")[0], altText)
        # use default icon
        iconPath = "images/icons/mimetype/icon.png"
        return "<img class=\"mime-type\" src=\"%s/%s\" title=\"%s\" alt=\"%s\" />" % (path, iconPath, altText, altText)

    def __getContentType(self, oid):
        #print " *** __getContentType(%s)" % oid
        contentType = ""
        if oid == "blank":
            contentType = "application/x-fascinator-blank-node"
        else:
            try:
                object = Services.getStorage().getObject(oid)
                sourceId = object.getSourceId()
                payload = object.getPayload(sourceId)
                contentType = payload.getContentType()
                payload.close()
                object.close()
            except StorageException, e:
                log.error("Error during getObject()", e)
        return contentType

    def __readManifest(self, oid):
        object = Services.getStorage().getObject(oid)
        sourceId = object.getSourceId()
        payload = object.getPayload(sourceId)
        manifest = Manifest(payload.open())
        payload.close()
        object.close()
        return manifest

    def __getRvtManifest(self, manifest):
        rvtMap = HashMap()
        rvtMap.put("title", manifest.getTitle())
        rvtMap.put("toc", self.__getRvtNodes(manifest.getTopNodes()))
        json = JsonObject(rvtMap)
        #print json.toString()
        return json.toString()

    def __getRvtNodes(self, manifest):
        rvtNodes = ArrayList()
        #print "manifest=%s" % manifest
        for node in manifest:
            package = False
            try:
                # add the node
                rvtNode = HashMap()
                if not node.getHidden():
                    oid = node.getId()
                    # check if node is a package
                    if oid != "blank":
                        package = (self.__getContentType(oid) == "application/x-fascinator-package")
                    else:
                        oid = node.getKey().replace("node", "blank")
                    rvtNode.put("visible", True)
                    rvtNode.put("title", node.getTitle())
                    if package:
                        subManifest = self.__readManifest(oid)
                        if subManifest is not None:
                            rvtNode.put("children", self.__getRvtNodes(subManifest.getTopNodes()))
                        oid = node.getKey().replace("node", "package")
                    else:
                        rvtNode.put("children", self.__getRvtNodes(node.getChildren()))
                    rvtNode.put("relPath", oid)
                    rvtNodes.add(rvtNode)
            except Exception, e:
                log.error("Failed to process node '%s': '%s'" % (node.toString(), str(e)))
        return rvtNodes
