'''
Created on 27/02/2013

@author: lloyd
'''
from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common.solr import SolrResult
from java.io import ByteArrayInputStream, ByteArrayOutputStream

class ReportsData:
    def __init__(self):
        pass

    def __activate__(self, context):
        #import pydevd;pydevd.settrace()
        self.velocityContext = context
        self.vc("sessionState").remove("fq")
        self.services = self.vc("Services")
        self.log = context["log"]
        self.__latest = None
        self.__search()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            print "ERROR: Requested context entry '" + index + "' doesn't exist"
            return None

    def __search(self):
        indexer = self.services.getIndexer()
        self.log.info("In reports.py search method.......")
        self.log.info("Indexer attributes are : " + str(dir(indexer)))
        portalQuery = self.services.getPortalManager().get(self.vc("portalId")).getQuery()
        portalSearchQuery = self.services.getPortalManager().get(self.vc("portalId")).getSearchQuery()
        
        # Security prep work
        isAdmin = self.vc("page").authentication.is_admin()
        if not isAdmin:
            print "ERROR: User is not an admin '"
            return None

        req = SearchRequest('eventType:"harvestStart"')
        req.setParam("fq", 'item_type:"object"')
        #if portalQuery:
            #req.addParam("fq", portalQuery)
        #if portalSearchQuery:
            #req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "eventTime desc");
        out = ByteArrayOutputStream()
        indexer.searchByIndex(req, out, 'eventLog')
        self.__latest = SolrResult(ByteArrayInputStream(out.toByteArray()))

        
        #self.vc("sessionState").set("fq", 'item_type:"object"')
        #sessionState.set("query", portalQuery.replace("\"", "'"))
    
    def getLatest(self):
        return self.__latest.getResults()

    def getItemCount(self):
        return self.__latest.getNumFound()