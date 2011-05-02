from au.edu.usq.fascinator.api.storage import Payload
from au.edu.usq.fascinator.common import JsonSimple
from java.io import ByteArrayOutputStream
from java.lang import Exception, String
from java.util.zip import ZipOutputStream, ZipEntry
from org.apache.commons.io import IOUtils
from org.dom4j import QName
from org.dom4j.io import XMLWriter, OutputFormat, SAXReader

import os
import hashlib
import urllib
import traceback
from xml.etree import ElementTree as ElementTree
from json2 import read as jsonReader, write as jsonWriter                       ##
import re


class ZippackageData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        response = self.vc("response")
        formData = self.vc("formData")
        oid = formData.get("oid")

        print "--- Creating package.zip for: %s ---" % oid
        try:
            manifest = self.__getPackageManifest(oid)
            items = self.__getItems(manifest.getJsonSimpleMap(["manifest"]))
            title = manifest.getString(None, ["title"])
            contentDisp = "attachment; filename=%s.zip" % self.__safeFilename(title)
            response.setHeader("Content-Disposition", contentDisp)
            out = response.getOutputStream("application/zip")
            zip = Zip(out)
            self.__createZipPackage(zip, title, items)
            zip.close()
        except Exception, e:
            #log.error("Failed to create epub", e)
            print "Failed to create epub - '%s'" % str(e)
            response.setStatus(500)
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println(str(e))
            writer.close()


    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '%s' doesn't exist" % index)
            return None


    def __getPackageManifest(self, oid):
        object = Services.getStorage().getObject(oid)
        sourceId = object.getSourceId()
        payload = object.getPayload(sourceId)
        manifest = JsonSimple(payload.open())
        payload.close()
        object.close()
        return manifest


    def __createZipPackage(self, zip, title, items):
        # items = [item1, item2, etc]
        #    item = {"id":id, "title":title, "hidden":hidden,
        #            "children":children,
        #            "sourcePayload":sourcePayload, "payloadList":payloadList}
        #       payloadListItem = (payloadObject, isHtmlVersion, isSourcePayload)
        print "__createZipPackage()"  # + "items=%s" % items
        # add all data
        def addItemsPayload(items):
            def addPayloadItems(id, payloadList):
                for obj in [i[0] for i in payloadList]:
                    #obj.id, obj.label, obj.contentType, obj.inputStream
                    zip.addStream("%s/%s" % (id, obj.label), obj.open())
                    obj.close()
            for item in items:
                id = item["id"]
                addPayloadItems(id, item["payloadList"])
                addItemsPayload(item["children"])
        addItemsPayload(items)
        # now create the index.html file (TOC)
        indexHtml = self.__createIndexHtml(title, items)
        zip.add("index.html", indexHtml)

    def __createIndexHtml(self, title, items):
        htmlTemplate = """<!DOCTYPE html>
<html>
 <head lang="en">
  <meta charset="utf-8"/>
  <title>%s</title>
 </head>
 <body>
    <h1>%s</h1>
    %s
 </body>
</html>"""
        toc = self.__createTOC(items)
        return htmlTemplate % (title, title, toc)

    def __createTOC(self, items):
        if items==[]:
            return ""
        list = []

        for item in items:
            #  item = {"id":id, "title":title, "hidden":hidden,
            #         "children":children,
            #         "sourcePayload":sourcePayload, "payloadList":payloadList}
            #    payloadListItem = (payloadObject, isHtmlVersion, isSourcePayload)
            id = item["id"]
            title = item["title"]
            children = item["children"]
            title = self.__escapeHtml(title)
            if item["hidden"]:
                pass
            elif id:
                # look for htmlVersion
                htmlVersions = [i for i in item["payloadList"] if i[1]]
                if htmlVersions!=[]:
                    pid = htmlVersions[0][0].id
                else:
                    pid = item["sourcePayload"].id
                pid = self.__escapeHtml(pid)
                s = "<a href='%s/%s'>%s</a>" % (id, pid, title)
                s += self.__createTOC(children)
            else:
                s = title
                s += self.__createTOC(children)
            s = "  " + s
            list.append(s)
        return "<ul>\n<li>%s</li>\n</ul>" % "\n</li><li>\n".join(list)

    def __getItems(self, manifest):
        items = []
        for itemHash in manifest.keySet():
            payloadDict = {}
            mItem = manifest.get(itemHash)
            id = mItem.getString(None, ["id"])
            title = mItem.getString(None, ["title"])
            hidden = mItem.getBoolean(False, ["hidden"])
            children = mItem.getJsonSimpleMap(["children"])
            if children is None or children=={}:
                children = []
            else:
                children = self.__getItems(children)
            if id!="blank":
                object = Services.storage.getObject(id)
                pid = object.getSourceId()
                htmlName = pid[:pid.rfind(".")] + ".htm"
                sourcePayload = object.getPayload(pid)
                #payloadType = sourcePayload.contentType
                payloadList = object.getPayloadIdList()
                for removeKey in ("TF-OBJ-META", "aperture.rdf"):
                    if removeKey in payloadList:
                        payloadList.remove(removeKey)
                payloadList = [(object.getPayload(p), p==htmlName, p==pid) for p in payloadList]
            else:
                id = None
                sourcePayload = None
                payloadList = []
            item = {"id":id, "title":title, "hidden":hidden,
                    "children":children,
                    "sourcePayload":sourcePayload, "payloadList":payloadList}
            items.append(item)
        return items

    def __escapeHtml(self, str):
        str = str.replace("&", "&amp;").replace("'", "&#39;").replace('"', "&#34;")
        str = str.replace("<", "&lt;").replace(">", "&gt;")
        return str

    def __safeFilename(self, name):
        name = re.sub("[^\w\(\)']", "-", name)
        return name


class Zip(object):
    def __init__(self, out):
        self.zipOutputStream = ZipOutputStream(out)

    def close(self):
        self.zipOutputStream.close()

    def addStream(self, name, s):
        self.zipOutputStream.putNextEntry(ZipEntry(name))
        IOUtils.copy(s, self.zipOutputStream)
        self.zipOutputStream.closeEntry()

    def add(self, name, data):
        self.zipOutputStream.putNextEntry(ZipEntry(name))
        self.__copyString(data, self.zipOutputStream)
        self.zipOutputStream.closeEntry()

    def __copyString(self, s, out):
        IOUtils.copy(IOUtils.toInputStream(String(s), "UTF-8"), out)



