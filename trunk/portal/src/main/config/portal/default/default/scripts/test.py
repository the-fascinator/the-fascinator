from java.util import ArrayList, HashMap

class TestData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context
        self.trueField = True
        self.falseField = False
    
    def getTrueMethod(self):
        return True
    
    def getFalseMethod(self):
        return False
    
    def getNumberOneTwoThree(self):
        return 123
    
    def getPythonDict(self):
        return { "one": 1, "two": 2, "three": 3 }
    
    def getPythonList(self):
        return [ "one", 2, "three", 4, "five" ]
    
    def getJavaList(self):
        javaList = ArrayList()
        javaList.add("one")
        javaList.add(2)
        javaList.add("three")
        javaList.add(4)
        javaList.add("five")
        return javaList
    
    def getJavaMap(self):
        javaMap = HashMap()
        javaMap.put("one", 1)
        javaMap.put("two", 2)
        javaMap.put("three", 3)
        return javaMap

    def getTextFromFormData(self):
        return self.vc["formData"].get("text")

    def getTextFromRequest(self):
        return self.vc["request"].getParameter("text")
