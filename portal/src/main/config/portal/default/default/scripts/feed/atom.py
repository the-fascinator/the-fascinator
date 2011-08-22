import os

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common.solr import SolrResult

from java.io import ByteArrayInputStream
from java.io import ByteArrayOutputStream
from java.io import UnsupportedEncodingException
from java.net import URLEncoder

class AtomData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.services = context["Services"]
        self.log = context["log"]
        self.portal = None
        self.__feed()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __feed(self):
        self.portal = self.services.getPortalManager().get(self.vc("portalId"))
        recordsPerPage = self.portal.recordsPerPage
        pageNum = self.vc("sessionState").get("pageNum", 1)

        query = "*:*"
        if self.vc("formData").get("query"):
            query = self.vc("formData").get("query")
            query = self.__escapeQuery(query)

        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", self.portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(self.portal.facetCount))
        req.setParam("sort", "f_dc_title asc")

        portalQuery = self.portal.query
        if portalQuery:
            req.addParam("fq", portalQuery)
        else:
            fq = self.vc("sessionState").get("fq")
            if fq is not None:
                req.setParam("fq", fq)

        req.setParam("start", str((pageNum - 1) * recordsPerPage))

        self.log.debug(" * Query: '{}'", query)
        self.log.debug(" * portalQuery: '{}'", portalQuery)
        self.log.debug(" * feed.py: '{}'", req)

        out = ByteArrayOutputStream()
        self.services.indexer.search(req, out)
        self.__result = SolrResult(ByteArrayInputStream(out.toByteArray()))

    def cleanUp(self, value):
        return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;")

    def hasResults(self):
        return self.__result is not None

    def getResult(self):
        return self.__result 

    def getFileName(self, path):
        return os.path.split(path)[1]

    def getPortal(self):
        return self.portal

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
            self.log.error("Error during UTF8 escape! '{}'", repr(eq), e)
            return eq
