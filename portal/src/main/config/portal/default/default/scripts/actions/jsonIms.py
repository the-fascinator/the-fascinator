
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonSimple
from au.edu.usq.fascinator.common import JsonObject

from java.lang import Exception
from java.util import ArrayList, HashMap

from org.apache.commons.io import IOUtils

class JsonImsData(object):
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        json = self.__getJson()
        out = self.vc("response").getOutputStream("application/json; charset=UTF-8")
        out.write(json)
        out.close()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __getJson(self):
        rvtMap = JsonObject()
        try:
            oid = self.vc("formData").get("oid")
            object = Services.storage.getObject(oid)
            payload = object.getPayload("imsmanifest.xml")
            try:
                from xml.etree import ElementTree
                xmlStr = IOUtils.toString(payload.open(), "UTF-8")
                payload.close()
                xml = ElementTree.XML(xmlStr.encode("UTF-8"))
                ns = xml.tag[:xml.tag.find("}")+1]
                resources = {}
                for res in xml.findall(ns+"resources/"+ns+"resource"):
                    resources[res.attrib.get("identifier")] = res.attrib.get("href")
                organizations = xml.find(ns+"organizations")
                defaultName = organizations.attrib.get("default")
                organizations = organizations.findall(ns+"organization")
                organizations = [o for o in organizations if o.attrib.get("identifier")==defaultName]
                organization = organizations[0]
                title = organization.find(ns+"title").text
                rvtMap.put("title", title)
                items = organization.findall(ns+"item")
                rvtMap.put("toc", self.__getJsonItems(ns, items, resources))
            except Exception, e:
                 data["error"] = "Error - %s" % str(e)
                 print data["error"]
            object.close()
        except StorageException, e:
            data["DEBUG"] = str(e.getMessage())

        rvtManifest = JsonSimple(rvtMap)
        return rvtManifest.toString()

    def __getJsonItems(self, ns, items, resources):
        rvtNodes = ArrayList()
        for item in items:
            attr = item.attrib
            isvisible = attr.get("isvisible") == "true"
            idref = attr.get("identifierref")
            id = resources.get(idref)
            title = item.find(ns+"title").text
            if isvisible and id and id.endswith(".htm"):
                rvtNode = HashMap()
                rvtNode.put("visible", True)
                rvtNode.put("relPath", id)
                rvtNode.put("title", title)
                rvtNode.put("children", self.__getJsonItems(ns, item.findall(ns+"item"), resources))
                rvtNodes.add(rvtNode)
        return rvtNodes
