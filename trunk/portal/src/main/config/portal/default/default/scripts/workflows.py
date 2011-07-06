from com.googlecode.fascinator.common import JsonSimple
from java.io import File
from java.util import LinkedHashMap
from org.apache.commons.lang.text import StrSubstitutor

class StageInfo:
    def __init__(self, stages):
        self.stages = stages
    
    def getFirst(self):
        return self.stages.get(0).get("name")
    
    def getFirstLabel(self):
        return self.stages.get(0).get("label")
    
    def getNext(self):
        if len(self.stages) >= 2:
            return self.stages.get(1).get("name")
        else:
            return self.getFirst()
    
    def getNextLabel(self):
        if len(self.stages) >= 2:
            return self.stages.get(1).get("label")
        else:
            return self.getFirstLabel()

class WorkflowsData:
    def __activate__(self, context):
        self.roles = context["page"].authentication.get_roles_list()
        self.config = context["systemConfig"]
        workflows = JsonSimple.toJavaMap(self.config.getObject(["uploader"]))
        self.uploaders = LinkedHashMap()
        
        for workflow in workflows.keySet():
            if workflows.get(workflow).getString("", ["upload-template"]):
                for role in workflows.get(workflow).getArray(["security"]):
                    if str(role) in self.roles:
                        self.uploaders.put(workflow, workflows.get(workflow))
    
    def getUploaders(self):
        return self.uploaders
    
    def getStageInfo(self, workflowId):
        uploader = self.uploaders.get(workflowId)
        config = JsonSimple(File(StrSubstitutor.replaceSystemProperties(uploader.getString("", ["json-config"]))))
        return StageInfo(config.getArray(["stages"]))
            
