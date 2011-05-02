from au.edu.usq.fascinator.portal import SwordSimpleServer
from java.io import File
from java.lang import Exception
from org.apache.commons.io import FileUtils
from org.purl.sword.client import Client, PostMessage

class SwordData(object):
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.__processRequest()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __processRequest(self):
        func = self.vc("formData").get("func")
        url = self.vc("formData").get("url")
        client = Client()
        client.setCredentials(self.vc("formData").get("username"), self.vc("formData").get("password"))
        if func == "collections":
            responseType = "application/json; charset=UTF-8"
            try:
                serviceDoc = client.getServiceDocument(url)
                data = '{"collections":['
                for w in serviceDoc.service.workspaces:
                    for c in w.collections:
                        data += '{"title":"%s","location":"%s"}' % (c.title, c.location)
                data += ']}'
                responseData = data
            except Exception, e:
                print str(e)
                responseData = '{"error":"%s"}' % str(e)
        elif func == "post":
            tmpFile = File.createTempFile("ims-", ".zip")
            from actions.imscp import ImsPackage
            imscp = ImsPackage(tmpFile)
            postMsg = PostMessage()
            postMsg.setFiletype("application/zip")
            postMsg.setFilepath(tmpFile.getAbsolutePath())
            postMsg.setDestination(url)
            depositResponse = client.postFile(postMsg)
            FileUtils.deleteQuietly(tmpFile)
            responseType = "text/xml; charset=UTF-8"
            responseData = str(depositResponse)
        else:
            responseType = "text/html; charset=UTF-8"
            responseData = ""
        out = self.vc("response").getPrintWriter(responseType)
        out.println(responseData)
        out.close()
