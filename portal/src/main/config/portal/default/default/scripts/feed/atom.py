import os

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common import JsonSimpleConfig

from java.io import ByteArrayInputStream
from java.io import ByteArrayOutputStream
from java.io import UnsupportedEncodingException
from java.net import URLEncoder

class AtomData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.__feed()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __feed(self):
        portal = Services.getPortalManager().get(self.vc("portalId"))
        recordsPerPage = portal.recordsPerPage
        pageNum = self.vc("sessionState").get("pageNum", 1)

        query = "*:*"
        if self.vc("formData").get("query"):
            query = self.vc("formData").get("query")
            query = self.__escapeQuery(query)

        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(portal.facetCount))
        req.setParam("sort", "f_dc_title asc")

        portalQuery = portal.query
        if portalQuery:
            req.addParam("fq", portalQuery)
        else:
            fq = sessionState.get("fq")
            if fq is not None:
                req.setParam("fq", fq)

        req.setParam("start", str((pageNum - 1) * recordsPerPage))

        print " * query: ", query
        print " * portalQuery='%s'" % portalQuery
        print " * feed.py:", req.toString()

        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonSimpleConfig(ByteArrayInputStream(out.toByteArray()))

    def cleanUp(self, value):
        return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;")

    def hasResults(self): 
        return self.__result is not None

    def getResult(self):
        return self.__result 

    def getFileName(self, path):
        return os.path.split(path)[1]

    def __escapeQuery(self, q):
        eq = q
        # escape all solr/lucene special chars
        # from http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Escaping%20Special%20Characters
        for c in "+-&|!(){}[]^\"~*?:\\":
            eq = eq.replace(c, "\\%s" % c)
        ## Escape UTF8
        try:
            return URLEncoder.encode(eq, "UTF-8")
        except UnsupportedEncodingException, e:
            print "Error during UTF8 escape! ", repr(eq)
            return eq
