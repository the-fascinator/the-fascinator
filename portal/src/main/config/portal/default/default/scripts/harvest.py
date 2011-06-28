from com.googlecode.fascinator.api import PluginManager

class HarvestData:
    
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.__harvestManager = Services.getHarvestManager()
        self.__content = self.__harvestManager.getContents().values().iterator().next()
        print self.__content
    
    def getContents(self):
        return self.__harvestManager.getContents()
    
    def getHarvesters(self):
        return PluginManager.getHarvesterPlugins()
    
    def getHarvester(self, type):
        return PluginManager.getHarvester(type)
    
    def getContent(self):
        return self.__content
