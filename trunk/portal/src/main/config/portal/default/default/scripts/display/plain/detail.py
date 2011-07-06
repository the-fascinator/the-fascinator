from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils
from org.apache.commons.lang import StringEscapeUtils

class DetailData:
    def __init__(self):
        pass

    def __activate__(self, context):
        pass

    def getContent(self, oid, metadata):
        # Get the object
        object = Services.getStorage().getObject(oid)
        if object is None:
            return ""

        # Get the preview/source payload
        pid = metadata.getFirst("preview")
        if pid is None:
            pid = object.getSourceId()
        if pid is None:
            return ""
        payload = object.getPayload(object.getSourceId())
        if payload is None:
            return ""

        # Stream the content out to string
        out = ByteArrayOutputStream()
        IOUtils.copy(payload.open(), out)
        payload.close()
        string = out.toString("UTF-8")

        return "<pre>" + StringEscapeUtils.escapeHtml(string) + "</pre>"
