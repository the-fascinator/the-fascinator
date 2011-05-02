from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonSimple
from au.edu.usq.fascinator.portal import FormData

from java.io import ByteArrayInputStream
from java.lang import String
from java.net import URLDecoder

import locale

class WorkflowData:
    def __init__(self):
        pass

    def __activate__(self, context):
        # prepare variables
        self.setup(context)
        self.getObject()
        self.getWorkflowMetadata()

        if self.formProcess:
            self.processForm()
        #print "workflow.py - UploadedData.__init__() done."

    def setup(self, context):
        self.velocityContext = context
        self.errorMsg = None
        # Test if some actual form data is available
        self.fileName = self.vc("formData").get("upload-file-file")
        self.formProcess = None
        self.localFormData = None
        self.metadata = None
        self.object = None
        self.pageService = self.vc("Services").getPageService()
        self.renderer = self.vc("toolkit").getDisplayComponent(self.pageService)
        self.template = None
        self.uploadRequest = None
        self.fileProcessing = False

        # Normal workflow progressions
        if self.fileName is None:
            self.hasUpload = False
            self.fileDetails = None
            oid = self.vc("formData").get("oid")
            uploadFormData = self.vc("sessionState").get("uploadFormData")
            if uploadFormData:
                self.fileProcessing = uploadFormData.get("fileProcessing")

            if oid is None and uploadFormData:
                oid = uploadFormData.get("oid")

            if oid is [] and self.fileProcessing=="true":
                self.formProcess = True
            elif oid is None:
                #for normal workflow progression
                self.formProcess = False
                self.template = None
            else:
                self.formProcess = True

        # First stage, post-upload
        else:
            # Some browsers won't match what came through dispatch, resolve that
            dispatchFileName = self.vc("sessionState").get("fileName")
            if dispatchFileName != self.fileName and self.fileName.find(dispatchFileName) != -1:
                self.fileName = dispatchFileName
            self.hasUpload = True
            self.fileDetails = self.vc("sessionState").get(self.fileName)
            #print "***** Upload details:", repr(self.fileDetails)
            self.template = self.fileDetails.get("template")
            self.errorMsg = self.fileDetails.get("error")

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getError(self):
        if self.errorMsg is None:
            return ""
        else:
            return self.errorMsg

    def getFileName(self):
        if self.uploadDetails() is None:
            return ""
        else:
            return self.uploadDetails().get("name")

    def getFileSize(self):
        if self.uploadDetails() is None:
            return "0kb"
        else:
            size = float(self.uploadDetails().get("size"))
            if size is not None:
                size = size / 1024.0
            locale.setlocale(locale.LC_ALL, "")
            return locale.format("%.*f", (1, size), True) + " kb"

    def getObjectMetadata(self):
        if self.getObject() is not None:
            try:
                return self.object.getMetadata()
            except StorageException, e:
                pass
        return None

    def getWorkflowMetadata(self):
        if self.metadata is None:
            if self.getObject() is not None:
                try:
                    wfPayload = self.object.getPayload("workflow.metadata")
                    self.metadata = JsonSimple(wfPayload.open())
                    wfPayload.close()
                except StorageException, e:
                    pass
                    #self.errorMsg = "Failed to get workflow metadata: %s" % str(e)
                    #if self.uploadRequest:
                    #    print "create workflow.metadata for upload request"
                    #    formData = self.vc("formData")
                        #{targetStep=[live], upload-file-file=[About Stacks.pdf], title=[About Stacks], submit=[Upload], oid=[], description=[PDF describing the Stacks feature of Mac OS X], upload-file-workflow=[basicUpload]}
        return self.metadata

    def getOid(self):
        if self.getObject() is None:
            return None
        else:
            return self.getObject().getId()

    def getObject(self):
        if self.object is None:
            # Find the OID for the object
            if self.justUploaded():
                # 1) Uploaded files
                oid = self.fileDetails.get("oid")
            else:
                # 2) or POST process from workflow change
                oid = self.vc("formData").get("oid")
                if oid is None:
                    # 3) or GET on page to start the process
                    uri = URLDecoder.decode(self.vc("request").getAttribute("RequestURI"))
                    basePath = self.vc("portalId") + "/" + self.vc("pageName")
                    oid = uri[len(basePath)+1:]

            # Now get the object
            if oid is not None:
                try:
                    self.object = self.vc("Services").storage.getObject(oid)
                    return self.object
                except StorageException, e:
                    self.errorMsg = "Failed to retrieve object : " + e.getMessage()
                    return None
        else:
            return self.object

    def getWorkflow(self):
        return self.fileDetails.get("workflow")

    def hasError(self):
        if self.errorMsg is None:
            return False
        else:
            return True

    def isPending(self):
        metaProps = self.getObject().getMetadata()
        status = metaProps.get("render-pending")
        if status is None or status == "false":
            return False
        else:
            return True

    def justUploaded(self):
        return self.hasUpload

    def prepareTemplate(self):
        # Retrieve our workflow config
        try:
            objMeta = self.getObjectMetadata()
            jsonObject = self.vc("Services").storage.getObject(objMeta.get("jsonConfigOid"))
            jsonPayload = jsonObject.getPayload(jsonObject.getSourceId())
            config = JsonSimple(jsonPayload.open())
            jsonPayload.close()
        except Exception, e:
            self.errorMsg = "Error retrieving workflow configuration"
            return False

        # Current workflow status
        meta = self.getWorkflowMetadata()
        if meta is None:
            self.errorMsg = "Error retrieving workflow metadata"
            return False
        currentStep = meta.getString(None, ["step"]) # Names
        nextStep = ""
        currentStage = None # Objects
        nextStage = None

        # Find next workflow stage
        stages = config.getJsonSimpleList(["stages"])
        if stages.size() == 0:
            self.errorMsg = "Invalid workflow configuration"
            return False

        ##print "--------------"
        ##print "meta='%s'" % meta        # "workflow.metadata"
        ##print "currentStep='%s'" % currentStep
        ##print "stages='%s'" % stages
        nextFlag = False
        for stage in stages:
            # We've found the next stage
            if nextFlag:
                nextFlag = False
                nextStage = stage
            # We've found the current stage
            if stage.getString(None, ["name"]) == currentStep:
                nextFlag = True
                currentStage = stage

        ##print "currentStage='%s'" % currentStage
        ##print "nextStage='%s'" % nextStage
        ##print "--------------"

        if nextStage is None:
            if currentStage is None:
                self.errorMsg = "Error detecting next workflow stage"
                return False
            else:
                nextStage = currentStage
        nextStep = nextStage.getString(None, ["name"])

        # Security check
        workflow_security = currentStage.getStringList(["security"])
        user_roles = self.vc("page").authentication.get_roles_list()
        valid = False
        for role in user_roles:
            if role in workflow_security:
                valid = True
        if not valid:
            self.errorMsg = "Sorry, but your current security permissions don't allow you to administer this item"
            return False

        self.localFormData = FormData()     # localFormData for organiser.vm
#        try:
#            autoComplete = currentStage.get("auto-complete", "")
#            self.localFormData.set("auto-complete", autoComplete)
#        except: pass

        # Check for existing data
        oldJson = meta.getObject(["formData"])
        if oldJson is not None:
            for field in oldJson.keySet():
                self.localFormData.set(field, oldJson.get(field))

        # Get data ready for progression
        self.localFormData.set("oid", self.getOid())
        self.localFormData.set("currentStep", currentStep)
        if currentStep == "pending":
            self.localFormData.set("currentStepLabel", "Pending")
        else:
            self.localFormData.set("currentStepLabel", currentStage.getString(None, ["label"]))
        self.localFormData.set("nextStep", nextStep)
        self.localFormData.set("nextStepLabel", nextStage.getString(None, ["label"]))
        self.template = nextStage.getString(None, ["template"])
        return True

    def processForm(self):
        # Get our metadata payload
        meta = self.getWorkflowMetadata()
        if meta is None:
            self.errorMsg = "Error retrieving workflow metadata"
            return
        # From the payload get any old form data
        oldFormData = meta.getObject(["formData"])
        if oldFormData is not None:
            oldFormData = JsonSimple(oldFormData)
        else:
            oldFormData = JsonSimple()

        # Process all the new fields submitted
        self.processFormData(meta, oldFormData, self.vc("formData"))
        self.processFormData(meta, oldFormData, self.vc("sessionState").get("uploadFormData"))
        self.vc("sessionState").remove("uploadFormData")
        
        # Write the form data back into the workflow metadata
        data = oldFormData.getJsonObject()
        metaObject = meta.writeObject(["formData"])
        for field in data.keySet():
            metaObject.put(field, data.get(field))

        # Write the workflow metadata back into the payload
        response = self.setWorkflowMetadata(meta)
        if not response:
            self.errorMsg = "Error saving workflow metadata"
            return

        # Re-index the object
        self.vc("Services").indexer.index(self.getOid())
        self.vc("Services").indexer.commit()

    def processFormData(self, meta, oldFormData, formData):
        if formData is not None:
            # Quick filter, we may or may not use these fields
            #    below, but they are not metadata
            specialFields = ["oid", "targetStep"]
            newFormFields = formData.getFormFields()
            for field in newFormFields:
                # Special fields - we are expecting them
                if field in specialFields:
                    #print " *** Special Field : '" + field + "' => '" + repr(formData.get(field)) + "'"
                    if field == "targetStep":
                        meta.getJsonObject().put(field, formData.get(field))    
                # Everything else... metadata
                else:
                    #print " *** Metadata Field : '" + field + "' => '" + repr(formData.get(field)) + "'"
                    oldFormData.getJsonObject().put(field, formData.get(field))

    def redirectNeeded(self):
        redirect = self.formProcess
        if redirect:
            if self.fileProcessing == "true":
                redirect = False
        return redirect

    def renderTemplate(self):
        r = self.renderer.renderTemplate(self.vc("portalId"), self.template, self.localFormData, self.vc("sessionState"))
        return r

    def setWorkflowMetadata(self, oldMetadata):
        try:
            jsonString = String(oldMetadata.toString())
            inStream = ByteArrayInputStream(jsonString.getBytes("UTF-8"))
            self.object.updatePayload("workflow.metadata", inStream)
            return True
        except StorageException, e:
            return False

    def uploadDetails(self):
        return self.fileDetails
