from com.googlecode.fascinator.api import PluginManager
from java.io import StringWriter
from org.apache.commons.io import IOUtils

class AboutData:
    def __activate__(self, context):
        self.pageService = context["Services"].pageService

    def getAccessControlPlugins(self):
        return PluginManager.getAccessControlPlugins()

    def getAuthenticationPlugins(self):
        return PluginManager.getAuthenticationPlugins()

    def getHarvesterPlugins(self):
        return PluginManager.getHarvesterPlugins()

    def getIndexerPlugins(self):
        return PluginManager.getIndexerPlugins()

    def getStoragePlugins(self):
        return PluginManager.getStoragePlugins()

    def getSubscriberPlugins(self):
        return PluginManager.getSubscriberPlugins()

    def getRolesPlugins(self):
        return PluginManager.getRolesPlugins()

    def getTransformerPlugins(self):
        return PluginManager.getTransformerPlugins()

    def getAboutPage(self, plugin, type):
        if type is None or plugin is None:
            return "<em>This plugin has provided no information about itself.</em>"
        pid = plugin.replace("-", "_")
        resource = "plugin/%s/%s/about.html" % (type, pid)
        stream = self.pageService.getResource(resource)
        if stream:
            writer = StringWriter()
            IOUtils.copy(stream, writer, "UTF-8")
            html = writer.toString()
            return html
        return "<em>This plugin has provided no information about itself.</em>"
