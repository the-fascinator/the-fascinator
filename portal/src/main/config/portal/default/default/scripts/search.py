import md5
import os.path

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common import Manifest
from com.googlecode.fascinator.common.solr import SolrResult
from com.googlecode.fascinator.portal import Pagination

from java.io import ByteArrayInputStream
from java.io import ByteArrayOutputStream
from java.io import UnsupportedEncodingException
from java.lang import Boolean
from java.net import URLDecoder
from java.net import URLEncoder
from java.util import ArrayList, LinkedHashMap, HashSet

class SearchData:
    def __activate__(self, context):
        self.services = context["Services"]
        self.page = context["page"]
        self.formData = context["formData"]
        self.portalId = context["portalId"]
        self.sessionState = context["sessionState"]
        self.request = context["request"]
        self.pageName = context["pageName"]
        self.log = context["log"]

        self.__portal = context["page"].getPortal()
        self.__useSessionNavigation = self.__portal.getBoolean(True, ["portal", "use-session-navigation"])
        self.__result = None
        if self.__useSessionNavigation:
            self.__pageNum = self.sessionState.get("pageNum", 1)
        else:
            self.__pageNum = 1
        self.__selected = ArrayList()
        self.__fqParts = []
        self.__searchField = self.formData.get("searchField", "full_text")
        
        self.__sortField = self.formData.get("sort-field")
        self.__sortOrder = self.formData.get("sort-order")
        if not (self.__sortField or self.__sortOrder):
            # use form data not specified, check session
            sortField = self.__portal.sortFieldDefault or "score"
            sortOrder = self.__portal.sortFieldDefaultOrder or "desc"
            #print "f:%s,o:%s" % (sortField, sortOrder)
            self.__sortField = self.sessionState.get("sortField", sortField)
            self.__sortOrder = self.sessionState.get("sortOrder", sortOrder)
        self.sessionState.set("sortField", self.__sortField)
        self.sessionState.set("sortOrder", self.__sortOrder)
        self.__sortBy = "%s %s" % (self.__sortField, self.__sortOrder)
        
        # reset the query and facet selections when changing views
        lastPortalId = self.sessionState.get("lastPortalId")
        if lastPortalId != self.portalId:
            self.log.info("Portal changed. Resetting saved search values in session.")
            self.sessionState.remove("fq")
            self.sessionState.remove("pageNum")
            self.sessionState.remove("sortField")
            self.sessionState.remove("sortOrder")
            self.__pageNum = 1
            self.sessionState.set("lastPortalId", self.portalId)
        
        self.__search()

    def usingSessionNavigation(self):
        return self.__restful
    
    def getPortalName(self):
        return self.__portal.getDescription()
    
    def getSearchField(self):
        return self.__searchField
    
    def __search(self):
        requireEscape = False
        recordsPerPage = self.__portal.recordsPerPage
        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        query = None
        pagePath = self.__portal.getName() + "/" + self.pageName
        if query is None or query == "":
            query = self.formData.get("query")
            requireEscape = True
        if query is None or query == "":
            query = "*:*"
        
        if query == "*:*":
            self.__query = ""
        else:
            self.__query = query
            if requireEscape:
                query = self.__escapeQuery(query)
            query = "%s:%s" % (self.__searchField, query)
        self.sessionState.set("query", self.__query)
        
        # find objects with annotations matching the query
        if query != "*:*":
            anotarQuery = self.__query
            if requireEscape:
                anotarQuery = self.__escapeQuery(anotarQuery)
            annoReq = SearchRequest(anotarQuery)
            annoReq.setParam("facet", "false")
            annoReq.setParam("rows", str(99999))
            annoReq.setParam("sort", "dateCreated asc")
            annoReq.setParam("start", str(0))
            anotarOut = ByteArrayOutputStream()
            self.services.indexer.annotateSearch(annoReq, anotarOut)
            resultForAnotar = SolrResult(ByteArrayInputStream(anotarOut.toByteArray()))
            resultForAnotar = resultForAnotar.getResults()
            ids = HashSet()
            for annoDoc in resultForAnotar:
                annotatesUri = annoDoc.getFirst("annotatesUri")
                ids.add(annotatesUri)
                self.log.debug("Found annotation for %s" % annotatesUri)
            # add annotation ids to query
            query += ' OR id:("' + '" OR "'.join(ids) + '")'
        
        portalSearchQuery = self.__portal.searchQuery
        if portalSearchQuery == "":
            portalSearchQuery = query
        else:
            if query != "*:*":
                query += " AND " + portalSearchQuery
            else:
                query = portalSearchQuery
        
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", self.__portal.facetFieldList)
        req.setParam("facet.sort", Boolean.toString(self.__portal.getFacetSort()))
        req.setParam("facet.limit", str(self.__portal.facetCount))
        req.setParam("sort", self.__sortBy)
        
        # setup facets
        if self.__useSessionNavigation:
            action = self.formData.get("verb")
            value = self.formData.get("value")
            fq = self.sessionState.get("fq")
            if fq is not None:
                self.__pageNum = 1
                req.setParam("fq", fq)
            if action == "add_fq":
                self.__pageNum = 1
                req.addParam("fq", URLDecoder.decode(value, "UTF-8"))
            elif action == "remove_fq":
                self.__pageNum = 1
                req.removeParam("fq", URLDecoder.decode(value, "UTF-8"))
            elif action == "clear_fq":
                self.__pageNum = 1
                req.removeParam("fq")
            elif action == "select-page":
                self.__pageNum = int(value)
        else:
            navUri = uri[len(pagePath):]
            self.__pageNum, fq, self.__fqParts = self.__parseUri(navUri)
            savedfq = self.sessionState.get("savedfq")
            limits = []
            if savedfq:
                limits.extend(savedfq)
            if fq:
                limits.extend(fq)
                self.sessionState.set("savedfq", limits)
                for q in fq:
                    req.addParam("fq", URLDecoder.decode(q, "UTF-8"))
        
        portalQuery = self.__portal.query
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.addParam("fq", 'item_type:"object"')
        if req.getParams("fq"):
            self.__selected = ArrayList(req.getParams("fq"))
        
        if self.__useSessionNavigation:
            self.sessionState.set("fq", self.__selected)
            self.sessionState.set("searchQuery", portalSearchQuery)
            self.sessionState.set("pageNum", self.__pageNum)
        
        # Make sure 'fq' has already been set in the session
        if not self.page.authentication.is_admin():
            current_user = self.page.authentication.get_username()
            security_roles = self.page.authentication.get_roles_list()
            security_filter = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
            security_exceptions = 'security_exception:"' + current_user + '"'
            owner_query = 'owner:"' + current_user + '"'
            security_query = "(" + security_filter + ") OR (" + security_exceptions + ") OR (" + owner_query + ")"
            req.addParam("fq", security_query)
        
        req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        #print " * search.py:", req.toString(), self.__pageNum
        
        out = ByteArrayOutputStream()
        self.services.indexer.search(req, out)
        self.__result = SolrResult(ByteArrayInputStream(out.toByteArray()))
        if self.__result is not None:
            self.__paging = Pagination(self.__pageNum,
                                       self.__result.getNumFound(),
                                       self.__portal.recordsPerPage)
    
    def __escapeQuery(self, q):
        temp = ""
        chars = "+-&|!(){}[]^\"~*?:\\"
        for c in q:
           if c in chars:
             temp += "\%s" % c
           else:
             temp += c
        
        return temp 
        
#        eq = q
#        # escape all solr/lucene special chars
#        # from http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Escaping%20Special%20Characters
#        for c in "+-&|!(){}[]^\"~*?:\\":
#            eq = eq.replace(c, "\\%s" % c)
#        ## Escape UTF8
#        try:
#            return URLEncoder.encode(eq, "UTF-8")
#        except UnsupportedEncodingException, e:
#            print "Error during UTF8 escape! ", repr(eq)
#            return eq
    
    def getQueryTime(self):
        return int(self.__result.getQueryTime()) / 1000.0;
    
    def getPaging(self):
        return self.__paging
    
    def getResult(self):
        return self.__result
    
    def getFacetField(self, key):
        return self.__portal.facetFields.get(key)
    
    def getFacetName(self, key):
        return self.__portal.facetFields.get(key).getString(None, ["label"])
    
    def getFacetCounts(self, key):
        if self.__useSessionNavigation:
            facetData = self.__result.getFacets()
            if facetData is None:
                return LinkedHashMap()
            if not facetData.containsKey(key):
                return LinkedHashMap()
            return facetData.get(key).values()
        else:
            return LinkedHashMap()
        # TODO : What were these doing? Hiding file path facets unless some facets are selected?
        #if name.find("/") == -1 or self.hasSelectedFacets():
        #    values.put(name, count)
    
    def getFacetDisplay(self):
        return self.__portal.facetDisplay
    
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
        return os.path.splitext(os.path.basename(path))[0]
    
    def getFacetQuery(self, name, value):
        return '%s:"%s"' % (name, value)
    
    # Packaging support
    def getActiveManifestTitle(self):
        return self.__getActiveManifest().getTitle()
    
    def getActiveManifestId(self):
        return self.sessionState.get("package/active/id")
    
    def getSelectedItemsCount(self):
        return self.__getActiveManifest().size()
    
    def isSelectedForPackage(self, oid):
        result = self.__getActiveManifest().getNode("node-%s" % oid)
        return (result is not None)
    
    def getManifestItemTitle(self, oid, defaultValue):
        result = self.__getActiveManifest().getNode("node-%s" % oid)
        if result is None:
            return defaultValue
        return result.getTitle()
    
    def __getActiveManifest(self):
        activeManifest = self.sessionState.get("package/active")
        if not activeManifest:
            activeManifest = Manifest(None)
            activeManifest.setTitle("New package")
            activeManifest.setViewId(self.__portal.getName())
            self.sessionState.set("package/active", activeManifest)
        return activeManifest

    def isSelectableForPackage(self, oid):
        return oid != self.getActiveManifestId()
    
    def getSortFields(self):
        return self.__portal.sortFields
    
    def getSortField(self):
        return self.__sortField
    
    def getSortOrder(self):
        return self.__sortOrder
    
    # RESTful style URL support methods
    def getPageQuery(self, page):
        prefix = ""
        if self.__fqParts:
            prefix = "/" + "/".join(self.__fqParts)
        suffix = ""
        if page > 1:
            suffix = "/page/%s" % page
        return prefix + suffix
    
    def getFacetQueryUri(self, name, value):
        return "%s/%s" % (name, value)
    
    def getFacetValue(self, facetValue):
        return facetValue.split("/")[-1]
    
    def getLimitQueryWith(self, fq):
        limits = ArrayList(self.__fqParts)
        limits.add("category/" + fq)
        return "/".join(limits)
    
    def getFacetIndent(self, facetValue):
        return len(facetValue.split("/"))
    
    def getLimitQueryWithout(self, fq):
        limits = ArrayList(self.__fqParts)
        limits.remove("category/" + fq)
        if limits.isEmpty():
            return ""
        return "/".join(limits)
    
    def __parseUri(self, uri):
        page = 1
        fq = []
        fqParts = []
        if uri != "":
            parts = uri.split("/")
            partType = None
            facetKey = None
            facetValues = None
            for part in parts:
                if partType == "page":
                    facetKey = None
                    page = int(part)
                elif partType == "category":
                    partType = "category-value"
                    facetValues = None
                    facetKey = part
                elif partType == "category-value":
                    if facetValues is None:
                        facetValues = []
                    if part in ["page", "category"]:
                        partType = part
                        facetQuery = '%s:"%s"' % (facetKey, "/".join(facetValues))
                        fq.append(facetQuery)
                        fqParts.append("category/%s/%s" % (facetKey, "/".join(facetValues)))
                        facetKey = None
                        facetValues = None
                    else:
                        facetValues.append(URLDecoder.decode(part))
                else:
                    partType = part
            if partType == "category-value":
                facetQuery = '%s:"%s"' % (facetKey, "/".join(facetValues))
                fq.append(facetQuery)
                fqParts.append("category/%s/%s" % (facetKey, "/".join(facetValues)))
        return page, fq, fqParts

