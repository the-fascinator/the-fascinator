from com.googlecode.fascinator.portal.workflow import SimpleWorkflowHelper
from org.apache.commons.lang import StringEscapeUtils
from org.apache.velocity import VelocityContext
from org.apache.commons.lang import StringEscapeUtils
from java.util import ArrayList
from org.apache.tapestry5.internal import KeyValue

class SimpleworkflowData:
    def __init__(self):
        self.simpleWorkflowHelper = SimpleWorkflowHelper()
    
    def __activate__(self, context):
       self.velocityContext = context
       self.Services = self.vc("Services") 
       self.simpleWorkflowHelper.setStorage(self.Services.storage)
       self.simpleWorkflowHelper.setVelocityService(self.Services.velocityService);
       self.simpleWorkflowHelper.setSystemConfiguration(self.vc("systemConfig"))
       self.simpleWorkflowHelper.setPortalId(self.vc("portalId"))
       self.simpleWorkflowHelper.setParentVelocityContext(self.__convertToVelocityContext())
       
       self.log = self.vc("log")
       
       request = self.vc("request")
       response = self.vc("response")
       formData = self.vc("formData")
       self.log.debug("parameters:     " +formData.toString())
       
       func = self.vc("formData").get("func", "")
       
       # Allow for URL GET paramaters
       if self.vc("request").method == "GET" and func != "":
            func = ""
       if func == "" and request.getParameter("func"):
            func = request.getParameter("func")
            
       if func == "get-tfpackage":
            oid = request.getParameter("oid")
            tfPackage = self.simpleWorkflowHelper.getTFPackage(oid)
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println(tfPackage.toString())
            writer.close()
            return None
        
       if func == "action":
            oid = request.getParameter("oid")
            action = request.getParameter("action")
            objMeta = request.getParameter("objMeta")
            
            self.simpleWorkflowHelper.updateTFPackage(oid,self.vc("formData"))
            targetStep = None
            if action != "save":
                targetStep = self.simpleWorkflowHelper.updateWorkflowMetadata(oid,action)
            
            if objMeta is not None:
                
                paramList = ArrayList()
                objMetaArray = objMeta.split(":")
                
                for objMetaParam in objMetaArray:
                    objMetaParamArray = objMetaParam.split(";")
                    if len(objMetaParamArray) == 2:
                        paramList.add(KeyValue(objMetaParamArray[0],objMetaParamArray[1]))
                self.simpleWorkflowHelper.updateObjectMetadata(oid,paramList)
                
            
            self.simpleWorkflowHelper.reindex(oid,targetStep,self.vc("page").authentication.get_username())  
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println("{\"success\":\"true\"}")
            writer.close()
            return None
        
       
    def getFormHtml(self, oid): 
        return self.simpleWorkflowHelper.getFormHtml(oid)
    
    def getFormData(self, field):
        formData = self.vc("formData")
        return StringEscapeUtils.escapeHtml(formData.get(field, ""))
    
    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            self.log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __convertToVelocityContext(self):
        vc = VelocityContext()
        for key in self.velocityContext.keySet():
            vc.put(key, self.vc(key));
        vc.put("velocityContext", vc);
        return vc
    

        
       