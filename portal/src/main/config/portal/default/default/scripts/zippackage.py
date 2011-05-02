from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common.solr import SolrResult

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import URLDecoder



class ZippackageData:
    def __init__(self):
        pass

    def __activate__(self, context):
        print "Zippackage.__activate__()"
        self.velocityContext = context
        self.__object = None
        self.__metadata = None
        self.getMetadata()
        print "  id='%s'" % self.getId()


    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None


    def getMetadata(self):
        if self.__metadata is None:
            oid = self.getId()
            if oid is None:
                self.vc("log").error("Error retrieving Solr entry for zip packaging")
                return None
            results = self.__loadSolrData(oid)
            if results is None:
                return None
            if results.getNumFound() == 1:
                self.__metadata = results.getResults().get(0)
        return self.__metadata


    def getId(self):
        object = self.getObject()
        if object is None:
            return ""
        else:
            return object.getId()


    def getObject(self):
        if self.__object is None:
            # Grab the URL
            req = self.vc("request").getAttribute("RequestURI")
            uri = URLDecoder.decode(req)
            # Cut everything down to the OID
            basePath = self.vc("portalId") + "/" + self.vc("pageName")
            oid = uri[len(basePath)+1:]
            # Trim a trailing slash
            if oid.endswith("/"):
                oid = oid[:-1]

            # Now get the object
            if oid is not None:
                try:
                    self.__object = Services.storage.getObject(oid)
                    return self.__object
                except StorageException, e:
                    self.vc("log").error("Failed to retrieve object : " + e.getMessage())
                    return None
        return self.__object


    def __loadSolrData(self, oid):
        portal = self.vc("page").getPortal()
        query = 'id:"%s"' % oid
        if portal.getSearchQuery():
            query += " AND " + portal.getSearchQuery()
        req = SearchRequest(query)
        req.addParam("fq", 'item_type:"object"')
        req.addParam("fq", portal.getQuery())
        out = ByteArrayOutputStream()
        self.vc("Services").getIndexer().search(req, out)
        return SolrResult(ByteArrayInputStream(out.toByteArray()))

