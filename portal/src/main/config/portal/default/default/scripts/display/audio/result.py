from display.default.result import ResultData as DefaultResultData
from org.apache.commons.lang import StringEscapeUtils

class ResultData(DefaultResultData):
    def __activate__(self, context):
        DefaultResultData.__activate__(self, context)
        self.md = context["metadata"]

    def getAudioSummary(self):
        result = self.append("", "dc_format", "Format")
        result = self.append(result, "dc_duration", "Duration")
        result = self.append(result, "meta_audio_details", "Audio Details")
        return result

    def append(self, string, field, label):
        value = self.get(field)
        if value is None:
            return string
        else:
            return string + "<strong>%s</strong>: %s<br/>" % (label, value)

    def get(self, name):
        return self.md.get(name)

    def getFirst(self, name):
        return self.md.getFirst(name)

    def getList(self, name):
        return self.md.getList(name)
    
    def getEscapeHtml(self, value):
        return StringEscapeUtils.escapeHtml(value)
