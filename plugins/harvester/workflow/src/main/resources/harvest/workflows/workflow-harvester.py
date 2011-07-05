import time

from com.googlecode.fascinator.api.storage import StorageException
from com.googlecode.fascinator.common import JsonSimple
from com.googlecode.fascinator.common.storage import StorageUtils

from java.io import ByteArrayInputStream
from java.lang import String

class IndexData:
    def __init__(self):
        pass

    def __activate__(self, context):
        # Prepare variables
        self.index = context["fields"]
        self.object = context["object"]
        self.payload = context["payload"]
        self.params = context["params"]
        self.utils = context["pyUtils"]
        self.config = context["jsonConfig"]

        # Common data
        self.__newDoc()

        # Real metadata
        if self.itemType == "object":
            self.__previews()
            self.__basicData()
            self.__metadata()
            # Some of the above steps may request some
            #  messages be sent, particularly workflows.
            self.__messages()
            self.__displayType()

        # Make sure security comes after workflows
        self.__security()

    def __newDoc(self):
        self.oid = self.object.getId()
        self.pid = self.payload.getId()
        metadataPid = self.params.getProperty("metaPid", "DC")

        if self.pid == metadataPid:
            self.itemType = "object"
        else:
            self.oid += "/" + self.pid
            self.itemType = "datastream"
            self.utils.add(self.index, "identifier", self.pid)
            self.utils.add(self.index, "identifier", self.pid)

        self.utils.add(self.index, "id", self.oid)
        self.utils.add(self.index, "storage_id", self.oid)
        self.utils.add(self.index, "item_type", self.itemType)
        self.utils.add(self.index, "last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ"))
        self.utils.add(self.index, "harvest_config", self.params.getProperty("jsonConfigOid"))
        self.utils.add(self.index, "harvest_rules",  self.params.getProperty("rulesOid"))

        self.item_security = []

    def __basicData(self):
        self.utils.add(self.index, "repository_name", self.params["repository.name"])
        self.utils.add(self.index, "repository_type", self.params["repository.type"])

    def __previews(self):
        self.previewPid = None
        for payloadId in self.object.getPayloadIdList():
            try:
                payload = self.object.getPayload(payloadId)
                if str(payload.getType())=="Thumbnail":
                    self.utils.add(self.index, "thumbnail", payload.getId())
                elif str(payload.getType())=="Preview":
                    self.previewPid = payload.getId()
                    self.utils.add(self.index, "preview", self.previewPid)
                elif str(payload.getType())=="AltPreview":
                    self.utils.add(self.index, "altpreview", payload.getId())
            except Exception, e:
                pass

    def __security(self):
        overrideFlag = self.params.getProperty("overrideWfSecurity", "false")
        if overrideFlag == "true":
            overrideFlag = True
        else:
            overrideFlag = False
        # Security
        roles = self.utils.getRolesWithAccess(self.oid)
        if roles is not None:
            # For every role currently with access
            for role in roles:
                # Should show up, but during debugging we got a few
                if role != "":
                    if role in self.item_security or overrideFlag:
                        # They still have access
                        self.utils.add(self.index, "security_filter", role)
                    else:
                        # Their access has been revoked
                        self.__revokeAccess(role)
            # Now for every role that the new step allows access
            for role in self.item_security:
                if role not in roles:
                    # Grant access if new
                    self.__grantAccess(role)
                    self.utils.add(self.index, "security_filter", role)

        # No existing security
        else:
            if self.item_security is None:
                # Guest access if none provided so far
                self.__grantAccess("guest")
                self.utils.add(self.index, "security_filter", role)
            else:
                # Otherwise use workflow security
                for role in self.item_security:
                    # Grant access if new
                    self.__grantAccess(role)
                    self.utils.add(self.index, "security_filter", role)
        # Ownership
        owner = self.params.getProperty("owner", None)
        if owner is None:
            self.utils.add(self.index, "owner", "system")
        else:
            self.utils.add(self.index, "owner", owner)

    def __indexPath(self, name, path, includeLastPart=True):
        parts = path.split("/")
        length = len(parts)
        if includeLastPart:
            length +=1
        for i in range(1, length):
            part = "/".join(parts[:i])
            if part != "":
                if part.startswith("/"):
                    part = part[1:]
                self.utils.add(self.index, name, part)

    def __indexList(self, name, values):
        if values is not None:
            for value in values:
                self.utils.add(self.index, name, value)

    def __getNodeValues(self, doc, xPath):
        nodes = doc.selectNodes(xPath)
        valueList = []
        if nodes:
            for node in nodes:
                #remove duplicates:
                nodeValue = node.getText()
                if nodeValue not in valueList:
                    valueList.append(node.getText())
        return valueList

    def __grantAccess(self, newRole):
        schema = self.utils.getAccessSchema("derby");
        schema.setRecordId(self.oid)
        schema.set("role", newRole)
        self.utils.setAccessSchema(schema, "derby")

    def __revokeAccess(self, oldRole):
        schema = self.utils.getAccessSchema("derby");
        schema.setRecordId(self.oid)
        schema.set("role", oldRole)
        self.utils.removeAccessSchema(schema, "derby")

    def __metadata(self):
        self.title = None
        self.descriptionList = []
        self.creatorList = []
        self.creationDate = []
        self.contributorList = []
        self.approverList = []
        self.formatList = []
        self.fulltext = []
        self.relationDict = {}
        self.customFields = {}

        # Try our data sources, order matters
        self.__dc()
        self.__aperture()
        self.__ffmpeg()
        self.__workflow()
        self.__filePath()

        # Some defaults if the above failed
        if self.title is None:
           self.title = self.object.getSourceId()
        if self.formatList == []:
            source = self.object.getPayload(self.object.getSourceId())
            self.formatList.append(source.getContentType())

        # Index our metadata finally
        self.utils.add(self.index, "dc_title", self.title)
        self.__indexList("dc_creator", self.creatorList)  #no dc_author in schema.xml, need to check
        self.__indexList("dc_contributor", self.contributorList)
        self.__indexList("dc_description", self.descriptionList)
        self.__indexList("dc_format", self.formatList)
        self.__indexList("dc_date", self.creationDate)
        self.__indexList("full_text", self.fulltext)
        for key in self.customFields:
            self.__indexList(key, self.customFields[key])
        for key in self.relationDict:
            self.__indexList(key, self.relationDict[key])

    def __dc(self):
        ### Check if dc.xml returned from ice exists.
        try:
            dcPayload = self.object.getPayload("dc.xml")
            self.utils.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
            dcXml = self.utils.getXmlDocument(dcPayload)
            if dcXml is not None:
                titleList = self.__getNodeValues(dcXml, "//dc:title")
                for dcTitle in titleList:
                    if self.title is None:
                        self.title = dcTitle
                self.descriptionList = self.__getNodeValues(dcXml, "//dc:description")
                self.creatorList = self.__getNodeValues(dcXml, "//dc:creator")
                self.contributorList = self.__getNodeValues(dcXml, "//dc:contributor")
                self.creationDate = self.__getNodeValues(dcXml, "//dc:issued")
                # ice metadata stored in dc:relation as key::value
                relationList = self.__getNodeValues(dcXml, "//dc:relation")
                for relation in relationList:
                    key, value = relation.split("::")
                    value = value.strip()
                    key = key.replace("_5f","") #ICE encoding _ as _5f?
                    if self.relationDict.has_key(key):
                        self.relationDict[key].append(value)
                    else:
                        self.relationDict[key] = [value]
        except StorageException, e:
            #print "Failed to index ICE dublin core data (%s)" % str(e)
            pass

    def __aperture(self):
        # Extract from aperture.rdf if exist
        try:
            from org.semanticdesktop.aperture.vocabulary import NCO;
            from org.semanticdesktop.aperture.vocabulary import NFO;
            from org.semanticdesktop.aperture.vocabulary import NID3;
            from org.semanticdesktop.aperture.vocabulary import NIE;
            from org.semanticdesktop.aperture.rdf.impl import RDFContainerImpl;
            from org.ontoware.rdf2go.model.node.impl import URIImpl;

            rdfPayload = self.object.getPayload("aperture.rdf")
            rdfModel = self.utils.getRdfModel(rdfPayload)

            # Seems like aperture only encode the spaces. Tested against special
            # characters file name and it's working
            safeOid = self.oid.replace(" ", "%20")
            rdfId = "urn:oid:%s" % safeOid.rstrip("/")

            container = RDFContainerImpl(rdfModel, rdfId)

            # 1. get title only if no title returned by ICE
            if self.title is None:
                titleCollection = container.getAll(NIE.title)
                iterator = titleCollection.iterator()
                while iterator.hasNext() and self.title is None:
                    node = iterator.next()
                    self.title = str(node).strip()

                titleCollection = container.getAll(NID3.title)
                iterator = titleCollection.iterator()
                while iterator.hasNext() and self.title is None:
                    node = iterator.next()
                    self.title = str(node).strip()

            # 2. get creator only if no creator returned by ICE
            if self.creatorList == []:
                creatorCollection = container.getAll(NCO.creator);
                iterator = creatorCollection.iterator()
                while iterator.hasNext():
                    node = iterator.next()
                    creatorUri = URIImpl(str(node))
                    creatorContainer = RDFContainerImpl(rdfModel, creatorUri);
                    value = creatorContainer.getString(NCO.fullname);
                    if value and value not in self.creatorList:
                        self.creatorList.append(value)

            # 3. getFullText: only aperture has this information
            fulltextString = container.getString(NIE.plainTextContent)
            if fulltextString:
                self.fulltext.append(fulltextString.strip())
                #4. description/abstract will not be returned by aperture, so if no description found
                # in dc.xml returned by ICE, put first 100 characters
                if self.descriptionList == []:
                    descriptionString = fulltextString
                    if len(fulltextString) > 100:
                        descriptionString = fulltextString[:100] + "..."
                    self.descriptionList.append(descriptionString)

            # 4. album title
            albumTitle = container.getString(NID3.albumTitle)
            if albumTitle:
                self.descriptionList.append("Album: " + albumTitle.strip())

            # 5. mimeType: only aperture has this information
            mimeType = container.getString(NIE.mimeType)
            if mimeType:
                self.formatList.append(mimeType.strip())

            # 6. contentCreated
            if self.creationDate == []:
                contentCreated = container.getString(NIE.contentCreated)
                if contentCreated:
                    self.creationDate.append(contentCreated.strip())
        except StorageException, e:
            #print "Failed to index aperture data (%s)" % str(e)
            pass

    def __ffmpeg(self):
        ### Check if ffmpeg.info exists or not
        try:
            ffmpegPayload = self.object.getPayload("ffmpeg.info")
            ffmpeg = self.utils.getJsonObject(ffmpegPayload.open())
            ffmpegPayload.close()
            if ffmpeg is not None:
                # Dimensions
                width = ffmpeg.getString(None, ["video", "width"])
                height = ffmpeg.getString(None, ["video", "height"])
                if width is not None and height is not None:
                    self.utils.add(self.index, "dc_size", width + " x " + height)

                # Duration
                duration = ffmpeg.getString(None, ["duration"])
                if duration is not None and int(duration) > 0:
                    if int(duration) > 59:
                        secs = int(duration) % 60
                        mins = (int(duration) - secs) / 60
                        self.utils.add(self.index, "dc_duration", "%dm %ds" % (mins, secs))
                    else:
                        self.utils.add(self.index, "dc_duration", duration + " second(s)")

                # Format
                media = ffmpeg.getString(None, ["format", "label"])
                if media is not None:
                    self.utils.add(self.index, "dc_media_format", media)

                # Video
                codec = ffmpeg.getString(None, ["video", "codec", "simple"])
                label = ffmpeg.getString(None, ["video", "codec", "label"])
                if codec is not None and label is not None:
                    self.utils.add(self.index, "video_codec_simple", codec)
                    self.utils.add(self.index, "video_codec_label", label)
                    self.utils.add(self.index, "meta_video_codec", label + " (" + codec + ")")
                else:
                    if codec is not None:
                        self.utils.add(self.index, "video_codec_simple", codec)
                        self.utils.add(self.index, "meta_video_codec", codec)
                    if label is not None:
                        self.utils.add(self.index, "video_codec_label", label)
                        self.utils.add(self.index, "meta_video_codec", label)
                pixel_format = ffmpeg.getString(None, ["video", "pixel_format"])
                if pixel_format is not None:
                    self.utils.add(self.index, "meta_video_pixel_format", pixel_format)

                # Audio
                codec = ffmpeg.getString(None, ["audio", "codec", "simple"])
                label = ffmpeg.getString(None, ["audio", "codec", "label"])
                if codec is not None and label is not None:
                    self.utils.add(self.index, "audio_codec_simple", codec)
                    self.utils.add(self.index, "audio_codec_label", label)
                    self.utils.add(self.index, "meta_audio_codec", label + " (" + codec + ")")
                else:
                    if codec is not None:
                        self.utils.add(self.index, "audio_codec_simple", codec)
                        self.utils.add(self.index, "meta_audio_codec", codec)
                    if label is not None:
                        self.utils.add(self.index, "audio_codec_label", label)
                        self.utils.add(self.index, "meta_audio_codec", label)
                sample_rate = ffmpeg.getString(None, ["audio", "sample_rate"])
                if sample_rate is not None:
                    sample_rate = "%.1f KHz" % (int(sample_rate) / 1000)
                channels = ffmpeg.getString(None, ["audio", "channels"])
                if channels is not None:
                    channels += " Channel(s)"
                if sample_rate is not None and channels is not None:
                    self.utils.add(self.index, "meta_audio_details", sample_rate + ", " + channels)
                else:
                    if sample_rate is not None:
                        self.utils.add(self.index, "meta_audio_details", sample_rate)
                    if channels is not None:
                        self.utils.add(self.index, "meta_audio_details", channels)
        except StorageException, e:
            #print "Failed to index FFmpeg metadata (%s)" % str(e)
            pass

    def __workflow(self):
        # Workflow data
        WORKFLOW_ID = "workflow1"
        wfChanged = False
        workflow_security = []
        self.message_list = None
        try:
            wfPayload = self.object.getPayload("workflow.metadata")
            wfMeta = self.utils.getJsonObject(wfPayload.open())
            wfPayload.close()

            # Are we indexing because of a workflow progression?
            targetStep = wfMeta.getString(None, ["targetStep"])
            if targetStep is not None and targetStep != wfMeta.getString(None, ["step"]):
                wfChanged = True
                # Step change
                wfMeta.getJsonObject().put("step", targetStep)
                wfMeta.getJsonObject().remove("targetStep")

            # This must be a re-index then
            else:
                targetStep = wfMeta.getString(None, ["step"])
            # Security change
            stages = self.config.getJsonSimpleList(["stages"])
            for stage in stages:
                if stage.getString(None, ["name"]) == targetStep:
                    wfMeta.getJsonObject().put("label", stage.getString(None, ["label"]))
                    self.item_security = stage.getStringList(["visibility"])
                    workflow_security = stage.getStringList(["security"])
                    if wfChanged == True:
                        self.message_list = stage.getStringList(["message"])

            # Form processing
            formData = wfMeta.getObject(["formData"])
            if formData is not None:
                formData = JsonSimple(formData)
            else:
                formData = None

            coreFields = ["title", "creator", "contributor", "description", "format", "creationDate"]
            if formData is not None:
                # Core fields
                formTitles = formData.getStringList(["title"])
                if formTitles:
                    for formTitle in formTitles:
                        if self.title is None:
                            self.title = formTitle
                creator = formData.getStringList(["creator"])
                if creator:
                    self.creatorList = creator
                contributor = formData.getStringList(["contributor"])
                if contributor:
                    self.contributorList = contributor
                description = formData.getStringList(["description"])
                if description:
                    self.descriptionList = description
                format = formData.getStringList(["format"])
                if format:
                    self.formatList = format
                creation = formData.getStringList(["creationDate"])
                if creation:
                    self.creationDate = creation
                # Non-core fields
                data = formData.getJsonObject()
                for field in data.keySet():
                    if field not in coreFields:
                        self.customFields[field] = formData.getStringList([field])

        except StorageException, e:
            # No workflow payload, time to create
            wfChanged = True
            wfMeta = JsonSimple()
            wfMetaObj = wfMeta.getJsonObject()
            wfMetaObj.put("id", WORKFLOW_ID)
            wfMetaObj.put("step", "pending")
            wfMetaObj.put("pageTitle", "Uploaded Files - Management")
            stages = self.config.getJsonSimpleList(["stages"])
            for stage in stages:
                if stage.getString(None, ["name"]) == "pending":
                    wfMetaObj.put("label", stage.getString(None, ["label"]))
                    self.item_security = stage.getStringList(["visibility"])
                    workflow_security = stage.getStringList(["security"])
                    self.message_list = stage.getStringList(["message"])

        # Has the workflow metadata changed?
        if wfChanged == True:
            jsonString = String(wfMeta.toString())
            inStream = ByteArrayInputStream(jsonString.getBytes("UTF-8"))
            try:
                StorageUtils.createOrUpdatePayload(self.object, "workflow.metadata", inStream)
            except StorageException, e:
                print " * workflow-harvester.py : Error updating workflow payload"

        self.utils.add(self.index, "workflow_id", wfMeta.getString(None, ["id"]))
        self.utils.add(self.index, "workflow_step", wfMeta.getString(None, ["step"]))
        self.utils.add(self.index, "workflow_step_label", wfMeta.getString(None, ["label"]))
        for group in workflow_security:
            self.utils.add(self.index, "workflow_security", group)

    def __filePath(self):
        baseFilePath = self.params["base.file.path"]
        filePath = self.object.getMetadata().getProperty("file.path")
        if baseFilePath:
            # NOTE: need to change again if the json file accept forward
            #       slash in windows
            # Get the base folder
            baseDir = baseFilePath.rstrip("/")
            baseDir = "/%s/" % baseDir[baseDir.rfind("/")+1:]
            filePath = filePath.replace("\\", "/").replace(baseFilePath, baseDir)
        self.__indexPath("file_path", filePath, False)

    def __messages(self):
        if self.message_list is not None and len(self.message_list) > 0:
            msg = JsonSimple()
            msg.getJsonObject().put("oid", self.oid)
            message = msg.toString()
            for target in self.message_list:
                self.utils.sendMessage(target, message)

    def __displayType(self):
        # check the object metadata for display type set by harvester or transformer
        # otherwise determine the display type by mime type
        displayType = self.params.getProperty("displayType")
        if not displayType:
            displayType = self.formatList[0]
            if displayType is not None:
                self.utils.add(self.index, "display_type", self.utils.basicDisplayType(displayType))
        else:
            self.utils.add(self.index, "display_type", displayType)
        # Some object use a special preview template. eg. word docs with a html preview
        previewType = self.params.getProperty("previewType")
        if not previewType:
            previewType = self.utils.getDisplayMimeType(self.formatList, self.object, self.previewPid)
            if previewType is not None and previewType != displayType:
                self.utils.add(self.index, "preview_type", self.utils.basicDisplayType(previewType))
        else:
            self.utils.add(self.index, "preview_type", previewType)
