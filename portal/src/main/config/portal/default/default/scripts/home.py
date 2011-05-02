from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common.solr import SolrResult
from java.io import ByteArrayInputStream, ByteArrayOutputStream

class HomeData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.vc("sessionState").remove("fq")
        self.services = self.vc("Services")
        self.__latest = None
        self.__mine = None
        self.__workflows = None
        self.__result = None
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
        portalQuery = self.services.getPortalManager().get(self.vc("portalId")).getQuery()
        portalSearchQuery = self.services.getPortalManager().get(self.vc("portalId")).getSearchQuery()
        
        # Security prep work
        current_user = self.vc("page").authentication.get_username()
        security_roles = self.vc("page").authentication.get_roles_list()
        security_filter = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        security_exceptions = 'security_exception:"' + current_user + '"'
        owner_query = 'owner:"' + current_user + '"'
        security_query = "(" + security_filter + ") OR (" + security_exceptions + ") OR (" + owner_query + ")"
        isAdmin = self.vc("page").authentication.is_admin()

        req = SearchRequest("last_modified:[NOW-1MONTH TO *]")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not isAdmin:
            req.addParam("fq", security_query)
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__latest = SolrResult(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest(owner_query)
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not isAdmin:
            req.addParam("fq", security_query)
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__mine = SolrResult(ByteArrayInputStream(out.toByteArray()))

        req = SearchRequest('workflow_security:"' + current_user + '"')
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not isAdmin:
            req.addParam("fq", security_query)
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__workflows = SolrResult(ByteArrayInputStream(out.toByteArray()))

        req = SearchRequest("*:*")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.addParam("fq", "")
        req.setParam("rows", "0")
        if not isAdmin:
            req.addParam("fq", security_query)
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        
        self.vc("sessionState").set("fq", 'item_type:"object"')
        #sessionState.set("query", portalQuery.replace("\"", "'"))
        
        self.__result = SolrResult(ByteArrayInputStream(out.toByteArray()))
    
    def getLatest(self):
        return self.__latest.getResults()
    
    def getMine(self):
        return self.__mine.getResults()

    def getWorkflows(self):
        return self.__workflows.getResults()

    def getItemCount(self):
        return self.__result.getNumFound()
