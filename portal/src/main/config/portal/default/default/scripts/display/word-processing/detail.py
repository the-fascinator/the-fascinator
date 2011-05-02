from org.apache.commons.lang import StringEscapeUtils

class DetailData:
    def __init__(self):
        pass

    def __activate__(self, context):
        pass

    def escape(self, text):
        return StringEscapeUtils.escapeHtml(text)
