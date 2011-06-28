import array, md5, os

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.api.storage import Payload, PayloadType
from com.googlecode.fascinator.api import PluginManager
from com.googlecode.fascinator.common import JsonConfig, JsonConfigHelper
from com.googlecode.fascinator.common.storage.impl import GenericPayload
from com.googlecode.fascinator.portal import Pagination, Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream, File, StringWriter
from java.lang import Boolean
from java.net import URLDecoder, URLEncoder
from java.util import LinkedHashMap
from java.lang import Object

from org.apache.commons.io import IOUtils
from org.apache.commons.lang import StringEscapeUtils
from org.dom4j.io import OutputFormat, XMLWriter, SAXReader

import traceback

class OrganiseData:
    def __init__(self):
        self.__portal = Services.portalManager.get(portalId)
        self.__result = JsonConfigHelper()
        self.__pageNum = sessionState.get("pageNum", 1)
        self.__selected = []
        
        self.__storage = Services.storage
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        basePath = portalId + "/" + pageName
        self.__oid = uri[len(basePath)+1:]
        slash = self.__oid.rfind("/")
        self.__pid = self.__oid[slash+1:]
        print "uri='%s' oid='%s' pid='%s'" % (uri, self.__oid, self.__pid)
        payload = None
        if (self.__oid is not None and self.__oid != ""):
            payload = self.__storage.getPayload(self.__oid, self.__pid)
        if payload is not None:
            self.__mimeType = payload.contentType
        else:
            self.__mimeType = "application/octet-stream"
        self.__metadata = JsonConfigHelper()
        print " * single.py: uri='%s' oid='%s' pid='%s' mimeType='%s'" % (uri, self.__oid, self.__pid, self.__mimeType)
        self.__search()
    
    def getManifestItem(self):
        hashId = md5.new(self.__oid).hexdigest()
        return self.getPortal().getMap("manifest//node-%s" % hashId)
    
    def getMimeType(self):
        return self.__mimeType
    
    def __search(self):
        req = SearchRequest('id:"%s"' % self.__oid)
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__json = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        self.__metadata = SolrDoc(self.__json)
    
    def getManifest(self):
        return self.getPortal().getJsonMap("manifest")
    
    def getContent(self):
        content = ""
        return content
    
    def getPortal(self):
        return Services.portalManager.get(portalId)
    
    def getPortalName(self):
        return Services.portalManager.get(portalId).description
    
    def __search(self):
        recordsPerPage = self.__portal.recordsPerPage

        query = None
        if query is None or query == "":
            query = formData.get("query")
        if query is None or query == "":
            query = "*:*"
        
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", "1000")
        req.setParam("facet.field", self.__portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(self.__portal.facetCount))
        req.setParam("sort", "f_dc_title asc")
        
        # setup facets
        action = formData.get("verb")
        value = formData.get("value")
        fq = sessionState.get("fq")
        if fq is not None:
            self.__pageNum = 1
            req.setParam("fq", fq)
        if action == "add_fq":
            self.__pageNum = 1
            name = formData.get("name")
            print " * add_fq: %s" % value
            req.addParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "remove_fq":
            self.__pageNum = 1
            req.removeParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "clear_fq":
            self.__pageNum = 1
            req.removeParam("fq")
        elif action == "select-page":
            self.__pageNum = int(value)
        req.addParam("fq", 'item_type:"object"')
        
        portalQuery = self.__portal.query
        print " * portalQuery=%s" % portalQuery
        if portalQuery:
            req.addParam("fq", portalQuery)
        
        self.__selected = req.getParams("fq")
        
        sessionState.set("fq", self.__selected)
        sessionState.set("pageNum", self.__pageNum)
        
        req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        print " * single.py:", req.toString(), self.__pageNum
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        if self.__result is not None:
            self.__paging = Pagination(self.__pageNum,
                                       int(self.__result.get("response/numFound")),
                                       self.__portal.recordsPerPage)
            print " * single.py: updating manifest..."
            portal = self.getPortal()
            manifest = portal.getJsonMap("manifest")
            #add new items from search
            for doc in self.__result.getList("response/docs"):
                hashId = md5.new(doc.get("id")).hexdigest()
                node = portal.get("manifest//node-%s" % hashId)
                if node is None:
                    portal.set("manifest/node-%s/title" % hashId, doc.get("dc_title").get(0))
                    portal.set("manifest/node-%s/id" % hashId, doc.get("id"))
            #remove manifest items missing from search result
            #print manifest
            for key in manifest.keySet():
                item = manifest.get(key)
                id = item.get("id")
                doc = self.__result.getList('response/docs[@id="%s"]' % id)
                if len(doc) == 0:
                    portal.removePath("manifest//%s" % key)
            Services.getPortalManager().save(portal)
    
    def getQueryTime(self):
        return int(self.__result.get("responseHeader/QTime")) / 1000.0;
    
    def getPaging(self):
        return self.__paging
    
    def getResult(self):
        return self.__result
    
    def getFacetField(self, key):
        return self.__portal.facetFields.get(key)
    
    def getFacetName(self, key):
        return self.__portal.facetFields.get(key).get("label")
    
    def getFacetCounts(self, key):
        values = LinkedHashMap()
        valueList = self.__result.getList("facet_counts/facet_fields/%s" % key)
        for i in range(0,len(valueList),2):
            name = valueList[i]
            count = valueList[i+1]
            if count > 0:
                values.put(name, count)
        return values
    
    def hasSelectedFacets(self):
        return (self.__selected is not None and len(self.__selected) > 1) and \
            not (self.__portal.query in self.__selected and len(self.__selected) == 2)
    
    def getSelectedFacets(self):
        return self.__selected
    
    def isPortalQueryFacet(self, fq):
        return fq == self.__portal.query
    
    def isSelected(self, fq):
        return fq in self.__selected
    
    def getSelectedFacetIds(self):
        return [md5.new(fq).hexdigest() for fq in self.__selected]
    
    def getFileName(self, path):
        return os.path.split(path)[1]
    
    def getFacetQuery(self, name, value):
        return '%s:"%s"' % (name, value)
    
    def isImage(self, format):
        return format.startswith("image/")
    
    def getThumbnail(self, oid):
        ext = os.path.splitext(oid)[1]
        url = oid[oid.rfind("/")+1:-len(ext)] + ".thumb.jpg"
        if Services.getStorage().getPayload(oid, url):
            return url
        return None
    
    def getPayloadContent(self):
        mimeType = self.__mimeType
        print " * single.py: payload content mimeType=%s" % mimeType
        contentStr = ""
        if mimeType.startswith("text/"):
            if mimeType == "text/html":
                contentStr = '<iframe class="iframe-preview" src="%s/download/%s"></iframe>' % \
                    (portalPath, self.__oid)
            else:
                pid = self.__oid[self.__oid.rfind("/")+1:]
                payload = self.__storage.getPayload(self.__oid, pid)
                print " * single.py: pid=%s payload=%s" % (pid, payload)
                if payload is not None:
                    sw = StringWriter()
                    sw.write("<pre>")
                    IOUtils.copy(payload.getInputStream(), sw)
                    sw.write("</pre>")
                    sw.flush()
                    contentStr = sw.toString()
        elif mimeType == "application/pdf" or mimeType.find("vnd.ms")>-1 or mimeType.find("vnd.oasis.opendocument.")>-1:
            # get the html version if exist...
            pid = os.path.splitext(self.__pid)[0] + ".htm"
            print " * single.py: pid=%s" % pid
            #contentStr = '<iframe class="iframe-preview" src="%s/download/%s/%s"></iframe>' % \
            #    (portalPath, self.__oid, pid)
            payload = self.__storage.getPayload(self.__oid, pid)
            saxReader = SAXReader(Boolean.parseBoolean("false"))
            try:
                document = saxReader.read(payload.getInputStream())
                slideNode = document.selectSingleNode("//*[local-name()='body']")
                #linkNodes = slideNode.selectNodes("//img")
                #contentStr = slideNode.asXML();
                # encode character entities correctly
                slideNode.setName("div")
                out = ByteArrayOutputStream()
                format = OutputFormat.createPrettyPrint()
                format.setSuppressDeclaration(True)
                format.setExpandEmptyElements(True)
                writer = XMLWriter(out, format)
                writer.write(slideNode)
                writer.close()
                contentStr = out.toString("UTF-8")
            except:
                traceback.print_exc()
                contentStr = "<p class=\"error\">No preview available</p>"
        elif mimeType.startswith("image/"):
            src = "%s/%s" % (self.__oid, self.__pid)
            contentStr = '<a class="image" href="%(src)s"  style="max-width:98%%">' \
                '<img src="%(src)s" style="max-width:100%%" /></a>' % { "src": self.__pid }
        return contentStr
    
    def getOid(self):
        return self.__oid

scriptObject = OrganiseData()
