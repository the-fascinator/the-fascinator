from java.awt import Desktop
from java.io import File
from java.lang import Exception

class OpenData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context
        writer = self.vc["response"].getPrintWriter("text/plain; charset=UTF-8")
        jsonResponse = "{}"
        try:
            oid = self.vc["formData"].get("oid")
            object = Services.getStorage().getObject(oid);
            filePath = object.getMetadata().getProperty("file.path")
            object.close()
            print "Opening file '%s'..." % filePath
            Desktop.getDesktop().open(File(filePath))
        except Exception, e:
            jsonResponse = '{ "message": "%s" }' % e.getMessage()
        writer.println(jsonResponse)
        writer.close()
