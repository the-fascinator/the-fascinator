import re

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common.solr import SolrResult
from com.googlecode.fascinator.indexer import SolrSearcher

from java.lang import Exception
from java.net import URI
from java.net import URLDecoder

from org.apache.solr.client.solrj.impl import CommonsHttpSolrServer

class HistoryData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.services = context["Services"]
        self.request = context["request"]
        self.response = context["response"]
        self.formData = context["formData"]
        self.page = context["page"]
        self.log = context["log"]
        self.config = context["systemConfig"]

        self.__oid = None
        self.__pid = None

        self.solrCore = None
        self.solrUrl = None

        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        matches = re.match("^(.*?)/(.*?)/(?:(.*?)/)?(.*)$", uri)
        if matches and matches.group(3):
            self.__oid = matches.group(3)
            self.__pid = matches.group(4)

    def getHistory(self):
        pass

    def getOid(self):
        if self.__oid is not None:
            return self.__oid
        else:
            return ""

    def searchSolr(self, query):
        return self.searchSolrRows(query, "1000")

    def searchSolrRows(self, query, rows):
        if not self.testLogger:
            return None
        else:
            try:
                # Build our query
                req = SearchRequest(query)
                req.addParam("wt", "json")
                req.addParam("sort", "eventTime desc")
                req.addParam("rows", rows)
                # Run our query
                searcher = SolrSearcher(self.solrCore.getBaseURL())
                inStream = searcher.get(req.getQuery(), req.getParamsMap(), False)
                return SolrResult(inStream)

            except Exception, e:
                self.log.error("Error performing Solr search: ", e)
                return None

    def testLogger(self):
        if self.solrCore is not None:
            return True

        try:
            # Find the URL
            self.solrUrl = self.config.getString(None, ["subscriber", "solr-event-log", "uri"])
            if self.solrUrl is None:
                return False
            # Parse and instantiate
            uri = URI(self.solrUrl)
            self.solrCore = CommonsHttpSolrServer(uri.toURL())
            if self.solrCore is None:
                return False
            ### You MUST set solrCore before calling a search or
            ###   an infinite loop will result
            result = self.searchSolrRows("*:*", "1")
            if result is None:
                return False
            return True
        except Exception, e:
            self.log.error("Error on History page: ", e)
            return False
