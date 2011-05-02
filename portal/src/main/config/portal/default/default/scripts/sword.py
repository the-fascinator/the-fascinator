#from org.purl.sword.client.Client import *
#import org.apache.commons.httpclient.auth.AuthChallengeProcessor as AuthChallengeProcessor
#import org.apache.commons.httpclient.HttpMethodDirector as HttpMethodDirector

from au.edu.usq.fascinator import QueueStorage
from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.common import JsonConfig
from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.common.storage.impl import GenericDigitalObject
from au.edu.usq.fascinator.common.storage.impl import FilePayload
from au.edu.usq.fascinator.portal import SwordSimpleServer

from java.io import File
from java.io import FileOutputStream
from java.io import FileWriter
from java.lang import Exception
from org.apache.commons.io import IOUtils

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
        depositUrl = "%s/sword/deposit.post" % portalPath
        sword = SwordSimpleServer(depositUrl)
        try:
            p =  self.vc("request").path.split(self.vc("portalId")+"/"+self.vc("pageName")+"/")[1]  # portalPath
        except:
            p = ""
        if p=="post":
            print "\n--- post ---"
            c = sword.getClient()
            c.clearProxy()
            c.clearCredentials()
            postMsg = sword.getPostMessage();
            postMsg.filetype = "application/zip"
            postMsg.filepath = "/home/ward/Desktop/Test.zip"
            depositResponse = c.postFile(postMsg)
            return str(depositResponse)
        elif p=="servicedocument":
            #print "\n--- servicedocument ---"
            sdr = sword.getServiceDocumentRequest()
            sdr.username = self.vc("formData").get("username", "test")
            sdr.password = self.vc("formData").get("password", "test")
            if self.vc("formData").get("test"):
                depositUrl += "?test=1"
            sd = sword.doServiceDocument(sdr)  # get a serviceDocument
            out = self.vc("response").getPrintWriter("text/xml; charset=UTF-8")
            out.println(str(sd))
            out.close()
            self.velocityContext["pageName"] = "-noTemplate-"
            return sd
        elif p=="deposit.post":
            #print "\n--- deposit ---  formData='%s'" % str(formData)
            inputStream = self.vc("formData").getInputStream()
            headers = {}
            for x in self.vc("formData").getHeaders().entrySet():
                headers[x.getKey()] = x.getValue()
            deposit = sword.getDeposit()
            noOp = headers.get("X-No-Op") or "false"
            deposit.noOp = (noOp.lower()=="true") or \
                (formData.get("test") is not None)
            contentDisposition = headers.get("Content-Disposition", "")
            filename = ""
            if contentDisposition!="":
                try:
                    filename = contentDisposition.split("filename=")[1]
                    deposit.filename = filename
                except: pass
            slug = headers.get("Slug")
            if slug is not None and slug!="":
                deposit.slug = slug
            #elif filename!="":
            #    deposit.slug = filename

            deposit.username = "SwordUser"
            deposit.password = deposit.username
            try:
                file = File.createTempFile("tmptf", ".zip")
                file.deleteOnExit()
                fos = FileOutputStream(file.getAbsolutePath())
                IOUtils.copy(inputStream, fos)
                fos.close()
                print "copied posted data to '%s'" % file.getAbsolutePath()
            except Exception, e:
                print "--- Exception - '%s'" % str(e)
            deposit.contentDisposition = file.getAbsolutePath()         #????
            deposit.file = inputStream
            depositResponse = sword.doDeposit(deposit)
            id = str(depositResponse.getEntry().id)
            try:
                print
                #imsPlugin = PluginManager.getTransformer("ims")
                jsonConfig = JsonConfig()
                #imsPlugin.init(jsonConfig.getSystemFile())
                #harvestClient = HarvestClient(jsonConfig.getSystemFile());
                storagePlugin = PluginManager.getStorage(jsonConfig.get("storage/type"))
                #storagePlugin.init(jsonConfig.getSystemFile())

                setConfigUri = self.__getPortal().getClass().getResource("/swordRule.json").toURI()
                configFile = File(setConfigUri)
                harvestConfig = JsonConfigHelper(configFile);
                tFile = File.createTempFile("harvest", ".json")
                tFile.deleteOnExit()
                harvestConfig.set("configDir", configFile.getParent())
                harvestConfig.set("sourceFile", file.getAbsolutePath())
                harvestConfig.store(FileWriter(tFile))

                zipObject = GenericDigitalObject(id)
                zipObject.addPayload(FilePayload(file, id))
                #digitalObject = imsPlugin.transform(zipObject, file)
                qStorage = QueueStorage(storagePlugin, tFile)
                qStorage.init(jsonConfig.getSystemFile())
                qStorage.addObject(zipObject)
                if deposit.noOp:
                    print "-- Testing noOp='true' --"
                else:
                    # deposit the content
                    pass
            except Exception, e:
                print "---"
                print " -- Exception - '%s'" % str(e)
                print "---"
            self.vc("response").setStatus(201)
            self.__mimeType = "text/xml"
            self.velocityContext["pageName"] = "-noTemplate-"
            self.vc("responseOutput").write(str(depositResponse))
            return
        elif p=="test":
            print "\n--- testing ---"
            print "formData='%s'" % str(self.vc("formData"))
        return "Test"
    
    def __getPortal(self):
        return Services.portalManager.get(self.vc("portalId"))
