from display.default.result import ResultData as DefaultResultData
from org.apache.commons.lang import StringEscapeUtils

class ResultData(DefaultResultData):
    def __activate__(self, context):
        DefaultResultData.__activate__(self, context)
    
    def getEscapeHtml(self, value):
        return StringEscapeUtils.escapeHtml(value)
