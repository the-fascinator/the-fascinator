from display.default.result import ResultData as DefaultResultData
from org.apache.commons.lang import StringEscapeUtils

from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils

class ResultData(DefaultResultData):
    def __activate__(self, context):
        DefaultResultData.__activate__(self, context)

    def getSourceSample(self, id, limit):
        # Get source payload
        object = self.services.getStorage().getObject(id)
        if object is not None:
            payload = object.getPayload(object.getSourceId())

        # Read to a string
        if payload is not None:
            out = ByteArrayOutputStream()
            IOUtils.copy(payload.open(), out)
            payload.close()
            string = out.toString("UTF-8")

        # Return response
        if string is not None:
            if (len(string)) > limit:
                return  string[0:limit] + "..."
            else:
                return  string
        else:
            return ""
    
    def getEscapeHtml(self, value):
        return StringEscapeUtils.escapeHtml(value)
