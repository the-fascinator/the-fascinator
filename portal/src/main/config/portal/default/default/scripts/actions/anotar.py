import re, time, json2 as json

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.api.storage import StorageException
from com.googlecode.fascinator.common import JsonSimple
from com.googlecode.fascinator.common.solr import SolrResult
from com.googlecode.fascinator.common.storage import StorageUtils

from java.io import ByteArrayInputStream, ByteArrayOutputStream

from org.apache.commons.io import IOUtils

class AnotarData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        
        self.__auth = context["page"].authentication
        # This gets called a lot
        self.fd = self.vc("formData").get

        self.action = self.fd("action")
        self.rootUri = self.fd("rootUri")
        self.json = self.fd("json")
        self.type = self.fd("type")
        self.rootUriList = self.vc("formData").getValues("rootUriList")
        if self.rootUriList is None:
            self.rootUriList = self.vc("formData").getValues("rootUriList[]")
        self.portalPath = self.vc("portalPath")
        #print "action:'%s' formData:'%s'" % (self.action, formData)

        # used so that ajax requests don't cache
        if self.rootUri and self.rootUri.find("?ticks") > -1:
            self.rootUri = self.rootUri[:self.rootUri.find("?ticks")]

        # Portal path info
        portalPath = self.portalPath + "/"
        self.oid = self.rootUri
        if self.oid and self.oid.startswith(portalPath):
            self.oid = self.oid[len(portalPath):]

        # oid for packaged items
        if self.oid:
            hashIndex = self.oid.find("#")
            if hashIndex > -1:
                self.oid = self.oid[hashIndex + 1:]

        result = ""
        if self.action == "getList":
            # Response is a list of object (nested)
            #print "**** anotar.py : GET_SOLR : " + self.rootUri
            result = self.search_solr()
        elif self.action == "put":
            result = self.__authenticate()
            if result is None:
                # Response is an ID
                #print "**** anotar.py : PUT : " + self.rootUri
                result = self.put()
        elif self.action == "delete":
            result = self.__authenticate()
            if result is None:
                # Response is empty
                result = self.delete()
                if result != "":
                    self.vc("response").setStatus(500)
        elif self.action == "get-image":
            # Response is the JSON format expected by image annotation plugin
            result = self.get_image()
        elif self.action == "save-image":
            result = self.__authenticate()
            if result is None:
                # Response is anotar JSON
                result = self.save_image()
        elif self.action == "delete-image":
            result = self.__authenticate()
            if result is None:
                result = self.delete_image()
        
        writer = self.vc("response").getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()

    def __authenticate (self):
        if not self.__auth.is_logged_in():
            self.vc("response").setStatus(500)
            return "Only registered users can access this API"
        return None

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def generate_id(self):
        counter = 0
        fileName = "anotar." + str(counter)
        payloadList = self.obj.getPayloadIdList()
        while fileName in payloadList:
            counter = counter + 1
            fileName = "anotar." + str(counter)
        self.pid = fileName
        print "New ID (" + self.pid + ")"

    def modify_json(self):
        #print "**** anotar.py : add_json() : adding json : " + json
        jsonSimple = JsonSimple(self.json)
        jsonObj = jsonSimple.getJsonObject()
        jsonObj.put("id", self.pid)
        rootUri = jsonSimple.getString(None, ["annotates", "rootUri"])
        if rootUri is not None:
            baseUrl = "http://%s:%s/" % (self.vc("request").serverName, self.vc("serverPort"))
            myUri = baseUrl + rootUri + "#" + self.pid
            jsonObj.put("uri", myUri)

        jsonObj.put("schemaVersionUri", "http://www.purl.org/anotar/schema/0.1")
        self.json = jsonSimple.toString()

    def process_response(self, result):
        #print " ******** result =", result
        docs = []
        rootDocs = []
        docsDict = {}
        # Build a dictionary of the annotations
        for doc in result:
            #hack is done here to replace [] with null as json.py does not properly parse 
            jsonStr = unicode(doc.get("jsonString").replace("[]", "null")).encode("utf-8")
            doc = json.read(jsonStr)
            doc["replies"] = []
            docs.append(doc)
            docsDict[doc["uri"]] = doc
            if doc["annotates"]["uri"] == doc["annotates"]["rootUri"]:
                rootDocs.append(doc)

        # Now process the dictionary
        for doc in docs:
            # If we are NOT a top level annotation
            if doc["annotates"]["uri"] != doc["annotates"]["rootUri"]:
                # Find what we are annotating
                try:
                    d = docsDict[doc["annotates"]["uri"]]
                    d["replies"].append(doc) # Add ourselves to its reply list
                except:
                    # TODO KeyError
                    pass
        return json.write(rootDocs)

    def process_tags(self, result):
        tags = []
        tagsDict = {}
        # Build a dictionary of the tags
        for doc in result:
            # Get Anotar data from Solr data
            doc = JsonSimple(doc.get("jsonString"))
            # Get actual tag text
            tag = doc.getString(None, ["content", "literal"])
            # Find out if they have locators
            locs = doc.getJsonSimpleList(["annotates", "locators"]).size()
            if locs == 0:
                # Basic tags, just aggregate counts
                if tag in tagsDict:
                    # We've seen it before, just increment the counter
                    existing = tagsDict[tag]
                    count = existing.getInteger(0, ["tagCount"])
                    existing.getJsonObject().put("tagCount", str(count + 1))
                else:
                    # First time, store this object
                    doc.getJsonObject().put("tagCount", str(1))
                    tagsDict[tag] = doc
            else:
                # Tags with a locator, special case for images etc.
                tags.append(doc.toString())

        # Push all the 'basic' counts into the list to return
        for tag in tagsDict:
            tags.append(tagsDict[tag].toString())
        return "[" + ",".join(tags) + "]"

    def put(self, pid=None):
        try:
            self.obj = Services.storage.getObject(self.oid)
        except StorageException, e:
            print " * anotar.py : Error creating object : ", e
            return e.getMessage()

        if pid:
            self.pid = pid
        else:
            self.generate_id()
        self.modify_json()

        try:
            p = StorageUtils.createOrUpdatePayload(
                    self.obj, self.pid, IOUtils.toInputStream(self.json, "UTF-8"))
        except StorageException, e:
            print " * anotar.py : Error creating payload :", e
            return e.getMessage()

        Services.indexer.annotate(self.oid, self.pid)
        return self.json

    def delete(self):
        self.obj = None
        pidList = self.vc("formData").getValues("pidList")
        try:
            try:
                self.obj = Services.storage.getObject(self.oid)
            except StorageException, se:
                print "Storage error getting object:", self.oid, ":", se
                if self.obj:
                    self.obj.close()
                return se.getMessage()
            
            for pid in pidList:
                self.__delete(self.rootUri, pid)
            
        finally:
            if self.obj:
                self.obj.close()
        return ""

    def __delete(self, rootUri, id):
        # delete the annotation from the object and anotar index
        try:
            self.obj.removePayload(id)
        except StorageException, se:
            print "Storage error removing payload:", id, ":", se
            if self.obj:
                self.obj.close()
            return se.getMessage()
        Services.indexer.annotateRemove(rootUri, id)

    def search_solr(self):
        query = "(rootUri:"
        if self.rootUriList:
            query += "(" + " OR ".join(self.rootUriList) + ")"
        else:
            query += "\"" + self.rootUri + "\""
        if self.type:
            query += " AND type:\"" + self.type + "\""
        query += ")"
        #print "**********", query

        req = SearchRequest(query)
        req.setParam("facet", "false")
        req.setParam("rows", str(99999))
        req.setParam("sort", "dateCreated asc")
        req.setParam("start", str(0))

        #security_roles = page.authentication.get_roles_list();
        #security_query = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        #req.addParam("fq", security_query)

        out = ByteArrayOutputStream()
        Services.indexer.annotateSearch(req, out)
        result = SolrResult(ByteArrayInputStream(out.toByteArray())).getResults()

        # Every annotation for this URI
        if self.type == "http://www.purl.org/anotar/ns/type/0.1#Tag":
            return self.process_tags(result)
        else:
            return self.process_response(result)

    def get_image(self):
        self.type = "http://www.purl.org/anotar/ns/type/0.1#Tag"
        mediaFragType = "http://www.w3.org/TR/2009/WD-media-frags-20091217"
        result = '{"result":' + self.search_solr() + '}'
        if result:
            imageTagList = []
            imageTags = JsonSimple(result).getJsonSimpleList(["result"])
            for imageTag in imageTags:
                imageAno = JsonSimple()
                # We only want tags with locators, not basic tags
                locators = imageTag.getJsonSimpleList(["annotates", "locators"])
                if locators and not locators.isEmpty():
                    locatorValue = locators.get(0).getString(None, ["value"])
                    locatorType = locators.get(0).get(None, ["type"])
                    if locatorValue and locatorValue.find("#xywh=")>-1 and locatorType == mediaFragType:
                        _, locatorValue = locatorValue.split("#xywh=")
                        left, top, width, height = locatorValue.split(",")
                        object = imageAno.getJsonObject()
                        object.put("top", top)
                        object.put("left", left)
                        object.put("width", width)
                        object.put("height", height)
                        object.put("creator", imageTag.getString(None, ["creator", "literal"]))
                        object.put("creatorUri", imageTag.getString(None, ["creator", "uri"]))
                        object.put("id", imageTag.getString(None, ["id"]))
                        #tagCount = imageTag.getString(None, ["tagCount"])
                        object.put("text", imageTag.getString(None, ["content", "literal"]))
                        object.put("editable", "true");
                        imageTagList.append(imageAno.toString())
            result = "[" + ",".join(imageTagList) + "]"
        return result

    def save_image(self):
        jsonTemplate = """
{
  "clientVersionUri": "http://www.purl.org/anotar/client/0.2",
  "type" : "http://www.purl.org/anotar/ns/type/0.1#Tag",
  "title" : {
    "literal" : null,
    "uri" : null
  },  
  "annotates" : {
    "uri" : "%s",
    "rootUri" : "%s",
    "locators" : [ {
      "originalContent": null,
      "type" : "http://www.w3.org/TR/2009/WD-media-frags-20091217",
      "value" : "%s"
    } ]
  },
  "creator" : {
    "literal" : "%s",
    "uri" : "%s",
    "email" : {
      "literal" : null
    }
  },
  "dateCreated" : {
    "literal" : "%s",
    "uri" : null
  },
  "dateModified" : {
    "literal" : null,
    "uri" : null
  },
  "content" : {
    "mimeType" : "text/plain",
    "literal" : "%s",
    "formData" : {
    }
  },
  "isPrivate" : false,
  "lang" : "en"
}
"""
        mediaDimension = "xywh=%s,%s,%s,%s" % (self.fd("left"), self.fd("top"), self.fd("width"), self.fd("height"))
        locatorValue = "%s#%s" % (self.rootUri, mediaDimension)
        dateCreated = time.strftime("%Y-%m-%dT%H:%M:%SZ")
        self.json = jsonTemplate % (self.rootUri, self.rootUri, locatorValue, self.fd("creator"), self.fd("creatorUri"), \
                                    dateCreated, self.fd("text"))
        id = self.fd("id")
        if id == "new":
            id = None
        result = self.put(id)

    def delete_image(self):
        pid = self.fd("id")
        try:
            try:
                self.obj = Services.storage.getObject(self.oid)
            except StorageException, se:
                print "Storage error getting object:", self.oid, ":", se
                if self.obj:
                    self.obj.close()
                return se.getMessage()
            
            self.__delete(self.oid, pid)
        finally:
            if self.obj:
                self.obj.close()
        return ""
