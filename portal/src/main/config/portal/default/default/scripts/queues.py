from au.edu.usq.fascinator.common import JsonSimpleConfig
from au.edu.usq.fascinator.common import MessagingServices

class QueuesData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.request = context["request"]
        self.response = context["response"]
        self.formData = context["formData"]

        if self.request.isXHR():
            print " **** formData: %s" % self.formData
            queue = self.formData.get("queueName")
            msg = self.formData.get("queueMessage")
            self.queueMessage(queue, msg);
            out = self.response.getPrintWriter("text/plain; charset=UTF-8")
            out.println(self.formData)
            out.close()

        self.config = JsonSimpleConfig()
        self.threads = self.config.getJsonSimpleList(["messaging", "threads"])

    def getDescription(self, queue):
        for thread in self.threads:
            name = thread.getString(None, ["config", "name"])
            if name == queue:
                return thread.getString(None, ["description"])

    def queueMessage(self, queue, msg):
        ms = MessagingServices.getInstance()
        ms.queueMessage(queue, msg);
        ms.release()

