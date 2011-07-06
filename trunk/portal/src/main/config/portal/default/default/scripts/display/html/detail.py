from com.googlecode.fascinator.api.storage import StorageException
from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils

class DetailData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.req = context["request"]
        self.serverPort = context["serverPort"]
        self.contextPath = context["contextPath"]
        self.portalId = context["portalId"]
        self.metadata = context["metadata"]
        self.config = context["systemConfig"]

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def frameContent(self, objectPath):
        objectLink = '<a class="iframe-link-alt" href="%s">View outside the frame</a>' % objectPath
        objectFrame = '<iframe class="iframe-preview" src="%s"></iframe>' % objectPath
        return objectLink + "<br/>" + objectFrame

    def pageContent(self):
        # Object ID
        oid = self.metadata.getFirst("id")
        # Determine MIME Type
        mimeType = "Unknown"
        mimeList = self.metadata.getList("dc_format")
        if mimeList is not None and not mimeList.isEmpty():
            mimeType = mimeList.get(0)

        # The HTML payload is the real object, display in a frame because we
        #  have no idea what kind of object it is.
        if mimeType == "text/html":
            urlBase = self.config.getString(None, ["urlBase"])
            if urlBase is None:
                "http://%s:%s%s/" % (self.req.serverName, self.serverPort, self.contextPath)
            objectPath = "%s%s/download/%s/" % (urlBase, self.portalId, oid)
            return self.frameContent(objectPath)

        # We are rendering a HTML preview...
        else:
            preview = self.metadata.getFirst("preview")

            # ... of an IMS package or zipped website. Treat as per html above.
            if mimeType == "application/zip":
                urlBase = self.config.getString(None, ["urlBase"])
                if urlBase is None:
                    "http://%s:%s%s/" % (self.req.serverName, self.serverPort, self.contextPath)
                objectPath = "%s%s/download/%s/%s" % (urlBase, self.portalId, oid, preview)
                return self.frameContent(objectPath)

            # ... of an HTML excerpt, such as an ICE rendition. Render in page.
            else:
                try:
                    object = Services.getStorage().getObject(oid)
                    payload = object.getPayload(preview)
                    out = ByteArrayOutputStream()
                    IOUtils.copy(payload.open(), out)
                    payload.close()
                    return out.toString("UTF-8")

                except StorageException, e:
                    return
"""
<h4 class="error">No preview available</h4>
<p>You can always <a class="open-this" href="#">access the original source file</a>.</p>
<p>Administrators can attempt re-rendering by using the Re-Harvest action.</p>
"""
