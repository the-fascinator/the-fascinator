from com.googlecode.fascinator.common import JsonObject, JsonSimple, JsonSimpleConfig

##### Debugging
#from java.awt import FlowLayout
#from javax.swing import JButton, JFrame, JOptionPane
#from javax.swing import JTextPane
#frame = JFrame("Title", size=(400, 300))
#frame.getContentPane().setLayout(FlowLayout())
#but = JButton("text", actionPerformed=func)   #def func(event):
#frame.add(but)
#text = JTextPane();  text.getText(); frame.add(text)
#frame.visible = True
#def popupDebugMessage(self, msg):
#    try:
#        JOptionPane.showMessageDialog(None, msg)
#    except Exception, e:
#        print "popupDebugMessage Error - '%s'\n'message was '%s'" % (str(e), msg)
#####

class PackageData(object):
    def __init__(self):
        pass

    def __activate__(self, context):
        self.sysConfig = JsonSimpleConfig()
        self.velocityContext = context
        self.__meta = {}
        formData = self.vc("formData")

        self.isAjax = self.vc("formData").get("ajax") != None
        if self.isAjax:
            ok = JsonObject()
            ok.put("ok", "OK")
            self.json = ok.toString()
        else:
            self.json = ""

        self.__selectedPackageType = formData.get("packageType", "default")
        #print "formData=%s" % self.vc("formData")
        #print "selectedPackageType='%s'" % self.__selectedPackageType
        self.__meta["packageType"] = formData.get("packageType", "default")
        self.__meta["description"] = formData.get("description", "")

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getIsAjax(self):
        return self.isAjax

    def popupDebugMessage(self, msg):
        try:
            JOptionPane.showMessageDialog(None, msg)
        except Exception, e:
            print "popupDebugMessage Error - '%s'\n'message was '%s'" % (str(e), msg)

    def getFormData(self, field):
        return self.__encoded(self.vc("formData").get(field, ""))

    def getMeta(self, metaName):
        return self.__encoded(self.__meta.get(metaName, ""))

    def getPackageTitle(self):
        title = self.getMeta("title")
        if title == "":
            title = "New package"
        return title

    def getOid(self):
        return self.getFormData("oid")

    def getPackageTypes(self):
        pt = self.__getPackageTypes().keySet()
        return pt

    def getSelectedPackageType(self):
        return self.__selectedPackageType

    def __getPackageTypes(self):
        object = self.sysConfig.getObject(["portal", "packageTypes"])
        packageTypes = JsonSimple.toJavaMap(object)
        if packageTypes.isEmpty():
            defaultPackage = JsonObject()
            defaultPackage.put("jsonconfig", "packaging-config.json")
            packageTypes.put("default", JsonSimple(defaultPackage))
        return packageTypes

    def __encoded(self, text):
        text = text.replace("&", "&amp;")
        text = text.replace("<", "&lt;").replace(">", "&gt;")
        return text.replace("'", "&#x27;").replace('"', "&#x22;")

    def test(self):
        s = "data "
        try:
            l = self.sysConfig.getStringList(["test"])
            s += str(l.size())
            s += " '%s' " % json.getString(None, ["test1"])
        except Exception, e:
            s += "Error '%s'" % str(e)
        return s
