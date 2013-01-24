import md5
from authentication import AuthenticationData
from java.net import URLDecoder
from org.apache.commons.lang import StringEscapeUtils

class LayoutData:
    def __init__(self):
        pass
    
    def __activate__(self, context):
        self.velocityContext = context
        self.log = context["log"]
        self.sessionState = context["sessionState"]
        self.services = context["Services"]
        self.security = context["security"]
        self.request = context["request"]
        self.portalId = context["portalId"]
        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        self.__relPath = "/".join(uri.split("/")[1:])
        self.authentication = AuthenticationData()
        self.authentication.__activate__(context)

        #self.formData = context["formData"]
        #if self.formData is not None:
        #    for field in self.formData.getFormFields():
        #        log.debug("Form Data: '{}' => '{}'", field, self.formData.get(field))
        #if self.sessionState is not None:
        #    for field in self.sessionState.keySet():
        #        log.debug("Session Data: '{}' => '{}'", field, self.sessionState.get(field))
        #log.debug("PATH: '{}'", self.request.getPath())
        #for param in self.request.getParameterNames():
        #    log.debug("PARAM: '{}' : '{}'", param, self.request.getParameter(param));

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getRelativePath(self):
        return self.__relPath
    
    def getPortal(self):
        return self.services.getPortalManager().get(self.portalId)
    
    def getPortals(self):
        return self.services.getPortalManager().portals
    
    def getPortalName(self):
        return self.getPortal().getDescription()

    def getQuery(self):
        query = self.sessionState.get("query")
        if query is None:
            return ""
        else:
            return self.escapeHtml(query)

    def escapeXml(self, text):
        return StringEscapeUtils.escapeXml(text)
    
    def escapeHtml(self, text):
        return StringEscapeUtils.escapeHtml(text)
    
    def unescapeHtml(self, text):
        return StringEscapeUtils.unescapeHtml(text)
    
    def md5Hash(self, data):
        return md5.new(data).hexdigest()
    
    def capitalise(self, text):
        return text[0].upper() + text[1:]
    
    def getTemplate(self, templateName):
        return self.services.velocityService.resourceExists(self.portalId, templateName)
    
    def getQueueStats(self):
        return self.services.getHouseKeepingManager().getQueueStats()
    
    def getSsoProviders(self):
        return self.security.ssoBuildLogonInterface(self.sessionState)

    def getPackageTypes(self):
        
        return ''
    
    def csrfSecurePage(self):
        pageName = self.vc("pageName");
        # Allow only POSTS to CSRF protected pages
        method = self.request.getMethod()
        if method != "POST":
            self.log.error("The secure page '{}' received a '{}' request and it only accepts 'POST'", pageName, method)
            return False
        # Allow only pages refered by use <= NOTE, this can be spoofed
        referer = self.request.getHeader("Referer")
        validReferer = self.vc("portalPath")
        if referer is None or not referer.startswith(validReferer):
            self.log.error("The secure page '{}' requires a valid HTTP Header Referer to use. REFERER: {}", pageName, referer)
            return False
        return True
