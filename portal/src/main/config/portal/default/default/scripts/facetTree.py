import md5
from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonSimple, JsonObject
from au.edu.usq.fascinator.common.solr import SolrResult
from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import URLEncoder
from java.util import ArrayList, HashMap
from org.json.simple import JSONArray

class Facet:
    def __init__(self, key, value, count):
        self.__name = value[value.rfind("/") + 1:]
        fq = '%s:"%s"' % (key, value)
        self.__facetQuery = URLEncoder.encode(fq, "UTF-8")
        self.__id = md5.new(fq).hexdigest()
        self.__count = count
        self.__subFacets = ArrayList()

    def getId(self):
        return self.__id

    def getName(self):
        return self.__name

    def getCount(self):
        return self.__count

    def getFacetQuery(self):
        return self.__facetQuery

    def addSubFacet(self, facet):
        self.__subFacets.add(facet)

    def getSubFacets(self):
        return self.__subFacets

    def getJson(self, state = "open"):
        title = "%s (%s)" % (self.getName(), self.getCount())
        json = JsonSimple()
        jsonObj = json.getJsonObject()
        attributes = JsonObject()
        attributes.put("id", self.getId())
        attributes.put("fq", self.getFacetQuery())
        attributes.put("title", title)
        jsonObj.put("data", title)
        jsonObj.put("attributes", attributes)
        hasSubFacets = not self.getSubFacets().isEmpty()
        if hasSubFacets:
            jsonObj.put("state", state)
            subFacetList = ArrayList()
            for subFacet in self.getSubFacets():
                subFacetList.add(subFacet.getJson("closed"))
            children = JSONArray()
            children.addAll(subFacetList)
            jsonObj.put("children", children)
        return json

class FacetList:
    def __init__(self, name, results):
        self.__facetMap = HashMap()
        self.__facetList = ArrayList()
        facets = results.getFacets()
        if facets is None:
            return
        facet = facets.get(name)
        if facet is None:
            return
        facetData = facet.values()
        for value in facetData.keySet():
            count = facetData.get(value)
            facet = Facet(name, value, count)
            self.__facetMap.put(value, facet)
            slash = value.rfind("/")
            if slash == -1:
                self.__facetList.add(facet)
            else:
                parent = self.__getFacet(value[:slash])
                if parent is not None:
                    parent.addSubFacet(facet)

    def __getFacet(self, name):
        return self.__facetMap.get(name)

    def getJsonList(self):
        jsonList = ArrayList()
        for facets in self.__facetList:
            jsonList.add(facets.getJson())
        return jsonList

class FacetTreeData:
    def __activate__(self, context):
        self.services = context["Services"]
        self.formData = context["formData"]
        self.sessionState = context["sessionState"]
        self.auth = context["page"].authentication

        result = "{}"
        try:
            facetList = self.__search()
            result = facetList.getJsonList().toString()
        except Exception, e:
            print " ERROR:: ", str(e)

        response = context["response"]
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()

    def __search(self):
        query = self.formData.get("query")
        searchQuery = self.sessionState.get("searchQuery")
        if query is None or query == "":
            query = "*:*"
        if searchQuery and query == "*:*":
            query = searchQuery
        elif searchQuery:
            query += " AND " + searchQuery
        facetField = self.formData.get("facet.field")

        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("fl", "id")
        req.setParam("rows", "0")
        req.setParam("facet.limit", "-1")
        req.setParam("facet.field", facetField)

        fq = self.sessionState.get("fq")
        if fq is not None:
            req.setParam("fq", fq)
        req.addParam("fq", 'item_type:"object"')

        # Make sure 'fq' has already been set in the session
        security_roles = self.auth.get_roles_list();
        security_query = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        req.addParam("fq", security_query)

        out = ByteArrayOutputStream()
        indexer = self.services.indexer
        indexer.search(req, out)
        result = SolrResult(ByteArrayInputStream(out.toByteArray()))

        return FacetList(facetField, result)

"""
#macro(createFacetNode $facet $state $isLast)
#set($data = "$facet.name ($facet.count)")
{
    "attributes": {
        "id": "$facet.id",
        "fq": "$facet.facetQuery",
        "title": "$data"
    },
    "data": "$data"
    #set($size = $facet.subFacets.size())
    #if($size > 0)
        ,
        "state": "$state",
        "children": [
        #foreach($subFacet in $facet.subFacets)
            #set($isSubLast = $velocityCount == $size)
            #createFacetNode($subFacet "closed" $isSubLast)
        #end
        ]
    #end
}
#if(!$isLast),#end
#end
"""    
