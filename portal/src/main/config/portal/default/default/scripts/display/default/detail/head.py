from au.edu.usq.fascinator.common import JsonSimple
from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils
from org.apache.commons.lang import StringEscapeUtils

class HeadData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.__metadata = self.vc("metadata")
        self.__urlBase = None
        self.__ffmpegRaw = None
        self.__ffmpegData = None
        self.__ffmpegOutputs = None
        
        self.__codecList = None

    def escape(self, text):
        return StringEscapeUtils.escapeHtml(text)

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getBasicFFmpegData(self, index):
        if self.__ffmpegData is not None:
            output = self.__ffmpegData.getString(None, [index])
            if output is not None:
                return output
        return ""

    def getFFmpegData(self, pid, index):
        if self.__ffmpegOutputs is not None:
            output = self.__ffmpegOutputs.get(pid).getString(None, [index])
            if output is not None:
                return output
        return ""

    # Get The MIME Type of a payload
    def getMimeType(self, pid, parent):
        if parent is not None:
            object = parent.getObject()
            if object is not None:
                try:
                    payload = object.getPayload(pid)
                    return payload.getContentType()
                except:
                    pass
        return "unknown"

    def getTranscodings(self):
        pass

    def hasMp4(self):
        for key in self.__ffmpegOutputs.keySet():
            list = self.__ffmpegOutputs.get(key)
            codec = list.getString("", ["codec"])
            if codec == "h264":
                return True

    def getRawFFmpeg(self):
        return self.__ffmpegRaw

    def getSplashScreen(self, metadata, preference):
        #TODO - Proper checking that the prefered payload actually exists
        if preference is not None and preference != "":
            return preference

        # Fall back to the thumbnail if no preference was given
        thumbnail = metadata.getFirst("thumbnail")
        if thumbnail is not None:
            return thumbnail
        return ""

    def isAudio(self, mime):
        return mime.startswith("audio/")

    def isVideo(self, mime):
        return mime.startswith("video/")

    # Turn a Python boolean into a javascript boolean
    def jsBool(self, pBool):
        if pBool:
            return "true"
        else:
            return "false"

    def parseFFmpeg(self, parent):
        if parent is not None:
            payload = None
            object = parent.getObject()
            if object is not None:
                try:
                    payload = object.getPayload("ffmpeg.info")
                    # Stream the content out to string
                    out = ByteArrayOutputStream()
                    IOUtils.copy(payload.open(), out)
                    payload.close()
                    self.__ffmpegRaw = out.toString("UTF-8")
                    out.close()
                    payload.close()
                    # And parse it
                    self.__ffmpegData = JsonSimple(self.__ffmpegRaw)
                    if self.__ffmpegData is None:
                        return False
                    else:
                        self.__ffmpegOutputs = self.__ffmpegData.getJsonSimpleMap(["outputs"])
                        return True
                except:
                    if payload is not None:
                        payload.close()
        return False

    def renderUrl(self, payload):
        if self.__urlBase is None:
            portal = self.vc("portalPath")
            page = self.vc("pageName")
            id = self.__metadata.getFirst("id")
            self.__urlBase = portal + "/" + page + "/" + id + "/"
        return self.__urlBase + payload
