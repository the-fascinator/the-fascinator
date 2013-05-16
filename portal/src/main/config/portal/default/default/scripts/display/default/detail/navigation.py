from com.googlecode.fascinator.api.storage import StorageException
from com.googlecode.fascinator.common import JsonSimple

class NavigationData:
    def __activate__(self, context):
        self.page = context["page"]
        self.metadata = context["metadata"]
        self.Services = context["Services"]
        self.object = None
        self.errorMsg = None
    
    def hasWorkflow(self):
        #print self.metadata
        self.__workflowStep = self.metadata.getList("workflow_step_label")
        if self.__workflowStep is None or self.__workflowStep.isEmpty():
            return False
        return True
    
    def hasWorkflowAccess(self):
        # role-based security check
        userRoles = self.page.authentication.get_roles_list()
        workflowSecurity = self.metadata.getList("workflow_security")
        if workflowSecurity is not None:
            for userRole in userRoles:
                if userRole in workflowSecurity:
                    return True
        
        # is the record owner allowed to edit their own record (in this workflow stage)?   
        self.errorMsg = None  
        currentStep = self.getWorkflowStepProper()    
        currentStage = self.getStageConfig(currentStep)
            
        if currentStage is not None:  
            if currentStage.getBoolean(False, "owner_edit_allowed"):
                current_user = self.page.authentication.get_username()
                if current_user == "guest":
                    if currentStage.getBoolean(False, "guest_owner_edit_allowed") == False:
                        return False
                owner = self.metadata.getFirst("owner")
                if current_user == owner:
                    return True

        if self.errorMsg is not None:
            print "Could not check user's workflow access: %s" % self.errorMsg
        return False
    
    def getWorkflowStep(self):
        return self.__workflowStep[0]
    
    def getWorkflowStepProper(self):
        stage = self.metadata.getList("workflow_step")
        if stage is None or stage.isEmpty():
            return None
        return stage[0]

    def getOid(self):
        oid = self.metadata.getList("oid")
        if oid is None or oid.isEmpty():
            return None
        return oid[0]

    def getObject(self):
        if self.object is None:
            if self.metadata is not None:
                oid = self.getOid()
                if oid is not None:
                    try:
                        self.object = self.Services.storage.getObject(oid)
                        return self.object
                    except StorageException, e:
                        self.errorMsg = "Failed to retrieve object : " + e.getMessage()
                        pass
                else:
                    self.errorMsg = "Could not find OID"
            else:
                self.errorMsg = "Object metadata not found"

    def getObjectMetadata(self):
        if self.getObject() is not None:
            try:
                return self.object.getMetadata()
            except StorageException, e:
                pass
        return None

    def getWorkflowConfig(self):
        objMeta = self.getObjectMetadata()    
        if objMeta is not None:    
            try:
                jsonObject = self.Services.storage.getObject(objMeta.get("jsonConfigOid"))
                jsonPayload = jsonObject.getPayload(jsonObject.getSourceId())
                config = JsonSimple(jsonPayload.open())
                jsonPayload.close()
                return config
            except Exception, e:
                self.errorMsg = "Error retrieving workflow configuration"
                pass
        return None

    def getStageConfig(self, targetStep):
        if targetStep is None:
            self.errorMsg = "No target step specified"
            return None

        # Retrieve our workflow config
        config = self.getWorkflowConfig()
        if config is None:
            return None

        # Find the current stage's workflow config
        stages = config.getJsonSimpleList(["stages"])
        if stages.size() == 0:
            self.errorMsg = "Invalid workflow configuration"
            return None
        for stage in stages:
            if stage.getString(None, ["name"]) == targetStep:
                return stage

        return None
    
