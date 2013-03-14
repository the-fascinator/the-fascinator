'''
Created on 14/03/2013

@author: lloyd
'''
from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common.solr import SolrResult
from java.io import ByteArrayInputStream, ByteArrayOutputStream

import traceback, sys, re, os

from java.net import URLDecoder

class HarvestreportData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.vc("sessionState").remove("fq")
        self.services = self.vc("Services")
        self.request = self.vc("request")
        self.log = context["log"]
        self.__records = None
        
        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        print "--- URI is %s ---" % uri
        self.__harvestId = os.path.basename(uri)
        print "--- harvest id is %s ---" % os.path.basename(uri)
        
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
        
        # Security prep work
        isAdmin = self.vc("page").authentication.is_admin()
        if not isAdmin:
            print "ERROR: User is not an admin '"
            return None

        req = SearchRequest('harvestId:"' + self.__harvestId + '"')
        req.setParam("fq", 'eventType:modify')
        out = ByteArrayOutputStream()
        try:
            indexer.searchByIndex(req, out, "eventLog")
        except:
            print traceback.format_exc();
            print repr(traceback.print_exc())
            traceback.print_stack(file=sys.stdout)
        self.__records = SolrResult(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest('harvestId:"' + self.__harvestId + '"')
        req.setParam("fq", 'eventType:modify')
        req.setParam("fq", 'isNew:true')
        out = ByteArrayOutputStream()
        indexer.searchByIndex(req, out, "eventLog")
        self.__latest = SolrResult(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest('harvestId:"' + self.__harvestId + '"')
        req.setParam("fq", 'eventType:modify')
        req.setParam("fq", 'isNew:false')
        req.setParam("fq", 'isModified:true')
        out = ByteArrayOutputStream()
        indexer.searchByIndex(req, out, "eventLog")
        self.__modified = SolrResult(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest('harvestId:"' + self.__harvestId + '"')
        req.setParam("fq", 'eventType:modify')
        req.setParam("fq", 'isNew:false')
        req.setParam("fq", 'isModified:false')
        out = ByteArrayOutputStream()
        indexer.searchByIndex(req, out, "eventLog")
        self.__unmodified = SolrResult(ByteArrayInputStream(out.toByteArray()))


    def getNewcount(self):
        return self.__latest.getNumFound()
    
    def getModifiedcount(self):
        return self.__modified.getNumFound()
    
    def getUnmodifiedcount(self):
        return self.__unmodified.getNumFound()
    
    def getRecords(self):
        return self.__records.getResults()

    def getItemcount(self):
        return self.__records.getNumFound()

    