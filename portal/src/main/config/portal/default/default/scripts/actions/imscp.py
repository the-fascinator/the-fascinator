import os.path, urllib

from au.edu.usq.fascinator.common import JsonSimple
from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.ims import FileType, ItemType, ManifestType, \
    MetadataType, ObjectFactory, OrganizationType, OrganizationsType, \
    ResourceType, ResourcesType

from java.io import FileOutputStream, StringWriter
from java.util.zip import ZipEntry, ZipOutputStream
from javax.xml.bind import JAXBContext, Marshaller

from org.apache.commons.io import IOUtils

class ImscpData:
    def __init__(self, outputFile=None):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        oid = self.vc("formData").get("oid")
        print "Creating IMS content package for: %s" % oid
        try:
            # get the package manifest
            object = Services.getStorage().getObject(oid)
            sourceId = object.getSourceId()
            payload = object.getPayload(sourceId)
            self.__manifest = JsonSimple(payload.open())
            payload.close()
            object.close()
            # create the package
            url = self.vc("formData").get("url")
            if outputFile is None and url is None:
                self.__createPackage()
            elif url is not None and outputFile is not None:
                self.__createPackage(outputFile)
        except Exception, e:
            log.error("Failed to create IMS content package: %s" % str(e))

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __createPackage(self, outputFile=None):
        title = self.__manifest.getString(None, "title")
        manifest = self.__createManifest()
        context = JAXBContext.newInstance("au.edu.usq.fascinator.ims")
        m = context.createMarshaller()
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, True)
        writer = StringWriter()
        jaxbElem = ObjectFactory.createManifest(ObjectFactory(), manifest)
        m.marshal(jaxbElem, writer)
        writer.close()

        if outputFile is not None:
            print "writing to %s..." % outputFile
            out = FileOutputStream(outputFile)
        else:
            print "writing to http output stream..."
            filename = urllib.quote(title.replace(" ", "_"))
            response.setHeader("Content-Disposition", "attachment; filename=%s.zip" % filename)
            out = response.getOutputStream("application/zip")

        zipOut = ZipOutputStream(out)

        zipOut.putNextEntry(ZipEntry("imsmanifest.xml"))
        IOUtils.write(writer.toString(), zipOut)
        zipOut.closeEntry()

        oidList = self.__manifest.search("id")
        for oid in oidList:
            obj = Services.getStorage().getObject(oid)
            for pid in obj.getPayloadIdList():
                payload = obj.getPayload(pid)
                if not PayloadType.Annotation.equals(payload.getType()):
                    zipOut.putNextEntry(ZipEntry("resources/%s/%s" % (oid, pid)))
                    IOUtils.copy(payload.open(), zipOut)
                    payload.close()
                    zipOut.closeEntry()
            obj.close()
        zipOut.close()
        out.close()

    def __createManifest(self):
        manifest = ManifestType()
        meta = MetadataType()
        meta.setSchema("IMS Content")
        meta.setSchemaversion("1.1.4")
        manifest.setMetadata(meta)

        jsonManifest = self.__manifest.getJsonSimpleMap("manifest")

        orgs = OrganizationsType()
        org = OrganizationType()
        org.setIdentifier("default")
        org.setTitle(self.__manifest.getString(None, "title"))
        orgs.getOrganization().add(org)
        orgs.setDefault(org)
        manifest.setOrganizations(orgs)

        resources = ResourcesType()
        manifest.setResources(resources)
        self.__createItems(org, resources, jsonManifest)

        return manifest

    def __createItems(self, parent, resources, jsonManifest):
        for key in jsonManifest.keySet():
            jsonRes = jsonManifest.get(key)
            try:
                item, webRes = self.__createItemAndResource(key, jsonRes)
                children = jsonRes.getJsonSimpleMap("children")
                if not children.isEmpty():
                    self.__createItems(item, resources, children)
                parent.getItem().add(item)
                resources.getResource().add(webRes)
            except Exception, e:
                print "Failed to create item for '%s': %s" % (key, str(e))

    def __createItemAndResource(self, key, jsonRes):
        hidden = jsonRes.getBoolean(False, "hidden")
        # organization item
        item = ItemType()
        item.setIdentifier("default-%s" % key)
        item.setIdentifierref(key)
        item.setIsvisible(not hidden)
        item.setTitle(jsonRes.getString(None, "title"))
        # resource
        webRes = ResourceType()
        webRes.setIdentifier(key)
        webRes.setType("webcontent")
        oid = jsonRes.getString(None, "id")
        obj = Services.getStorage().getObject(oid)
        baseName = os.path.splitext(obj.getSourceId())[0]
        webRes.setHref("resources/%s/%s.htm" % (oid, baseName))
        for pid in obj.getPayloadIdList():
            payload = obj.getPayload(pid)
            if not PayloadType.Annotation.equals(payload.getType()):
                file = FileType()
                file.setHref(pid)
                webRes.getFile().add(file)
        obj.close()
        return item, webRes
