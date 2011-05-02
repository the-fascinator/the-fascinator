from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator import HarvestClient
from au.edu.usq.fascinator.common import JsonSimpleConfig

from java.io import File, ByteArrayInputStream, ByteArrayOutputStream

from org.apache.commons.lang import StringUtils

class BatchprocessData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.formData = context["formData"]
        self.response = context["response"]
        self.services = context["Services"]
        self.page = context["page"]
        self.portal = self.page.getPortal()
        self.vc = context["toolkit"]
        self.log = context["log"]
        self.writer = self.response.getPrintWriter("text/html; charset=UTF-8")
        print " *** action batchProcess.py: formData=%s" % self.formData
        auth = context["page"].authentication
        if auth.is_admin():
            self.storage = self.services.getStorage()
            result = self.__process()
        else:
            result = '{ "status": "error", "message": "Only administrative users can access this API" }'
        self.writer.println(result)
        self.writer.close()
    
    def __process(self):
        func = self.formData.get("func")
        result = {}
        
        if func == "batch-update":
            updateScriptFile = self.formData.get("update-script-file")
            self.__checkIfScriptFileIsValid(updateScriptFile)
                
            objectIdList = self.__search("*:*")
            try:
                self.__harvester = HarvestClient()
                for oid in objectIdList:
                    self.__processObject(oid, updateScriptFile, "false")
            except Exception, ex:
                error = "Batch update failed: %s" % str(ex)
                self.log.error(error, ex)
                return '{ status: "failed" }'
        elif func == "batch-export":
            exportScriptFile = self.formData.get("export-script-file")
            self.__checkIfScriptFileIsValid(exportScriptFile)
            
            objectIdList = self.__search("modified:true")
            try:
                self.__harvester = HarvestClient()
                for oid in objectIdList:
                    self.__processObject(oid, exportScriptFile, "true")
            except Exception, ex:
                error = "Batch export failed: %s" % str(ex)
                self.log.error(error, ex)
                return '{ status: "failed" }'
        
        return result
        
    def __checkIfScriptFileIsValid(self, scriptFile):
        if scriptFile == "":
            result = "Invalid script file"
            self.throw_error("Invalid script file")
        script = File(scriptFile)
        if not script.exists():
            result = "Script file is not exist"
            self.throw_error("Script file is not exist")
    
    def __processObject(self, oid, scriptFile, resetModifiedProperty):
        try:
            print "** processObject: ", oid
            # temporarily update the object properties for transforming
            obj = self.storage.getObject(oid)
            props = obj.getMetadata()
            
            self.__setProperty(props, "indexOnHarvest", "false")
            self.__setProperty(props, "harvestQueue", "")
            renderQueue = self.__setProperty(props, "renderQueue", "jython")
            props.setProperty("resetModifiedProperty", resetModifiedProperty)
            self.__setProperty(props, "jythonScript", scriptFile)
            
            obj.close()
            # signal a reharvest
            self.__harvester.reharvest(oid);
        except StorageException, se:
            print se
    
    def __setProperty(self, props, key, newValue=None):
        oldValue = props.get(key)
        if oldValue:
            props.setProperty("copyOf_" + key, oldValue)
        if newValue:
            props.setProperty(key, newValue)
        else:
            props.remove(key)
        return oldValue
    
    def __search(self, searchField):
        indexer = self.services.getIndexer()
        portalQuery = self.services.getPortalManager().get(self.portal.getName()).getQuery()
        portalSearchQuery = self.services.getPortalManager().get(self.portal.getName()).getSearchQuery()
        
        # Security prep work
        current_user = self.page.authentication.get_username()
        security_roles = self.page.authentication.get_roles_list()
        security_filter = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        security_exceptions = 'security_exception:"' + current_user + '"'
        owner_query = 'owner:"' + current_user + '"'
        security_query = "(" + security_filter + ") OR (" + security_exceptions + ") OR (" + owner_query + ")"
        
        startRow = 0
        numPerPage = 25
        numFound = 0
        
        req = SearchRequest(searchField)
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        if not self.page.authentication.is_admin():
            req.addParam("fq", security_query)
        
        objectIdList = []
        while True:
            req.addParam("fq", 'item_type:"object"')
            req.addParam("rows", str(numPerPage))
            req.addParam("start", str(startRow))
            
            out = ByteArrayOutputStream()
            indexer.search(req, out)
            result = JsonSimpleConfig(ByteArrayInputStream(out.toByteArray()))

            docs = result.getJsonList("response", "docs")
            docIds = []
            for doc in docs:
                docId = doc.getString(None, "storage_id")
                if docId is not None:
                    docIds.append(docId)
            objectIdList.extend(docs)

            startRow += numPerPage
            numFound = int(result.getString(None, "response", "numFound"))
            
            if (startRow > numFound):
                break

        return objectIdList
    
    def throw_error(self, message):
        self.response.setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()
        
