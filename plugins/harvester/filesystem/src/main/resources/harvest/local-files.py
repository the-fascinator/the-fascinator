import time

from au.edu.usq.fascinator.api.storage import StorageException

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

        # Common data
        self.__newDoc()
        self.__security()

        # Real metadata
        if self.itemType == "object":
            self.__previews()
            self.__basicData()
            self.__metadata()
            self.__displayType()

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

        self.utils.add(self.index, "id", self.oid)
        self.utils.add(self.index, "storage_id", self.oid)
        self.utils.add(self.index, "item_type", self.itemType)
        self.utils.add(self.index, "last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ"))
        self.utils.add(self.index, "harvest_config", self.params.getProperty("jsonConfigOid"))
        self.utils.add(self.index, "harvest_rules",  self.params.getProperty("rulesOid"))

    def __basicData(self):
        self.utils.add(self.index, "repository_name", self.params["repository.name"])
        self.utils.add(self.index, "repository_type", self.params["repository.type"])

    def __previews(self):
        self.previewPid = None
        for payloadId in self.object.getPayloadIdList():
            try:
                payload = self.object.getPayload(payloadId)
                #print "TYPE:", payload.getType()
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
        roles = self.utils.getRolesWithAccess(self.oid)
        if roles is not None:
            for role in roles:
                self.utils.add(self.index, "security_filter", role)
        else:
            # Default to guest access if Null object returned
            schema = self.utils.getAccessSchema("derby");
            schema.setRecordId(self.oid)
            schema.set("role", "guest")
            self.utils.setAccessSchema(schema, "derby")
            self.utils.add(self.index, "security_filter", "guest")

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
        self.titleList = []
        self.descriptionList = []
        self.creatorList = []
        self.creationDate = []
        self.contributorList = []
        self.approverList = []
        self.formatList = []
        self.fulltext = []
        self.relationDict = {}

        # Try our data sources, order matters
        self.__dc()
        self.__aperture()
        self.__ffmpeg()
        self.__filePath()

        # Some defaults if the above failed
        if self.titleList == []:
           self.titleList.append(self.object.getSourceId())
        if self.formatList == []:
            source = self.object.getPayload(self.object.getSourceId())
            self.formatList.append(source.getContentType())

        # Index our metadata finally
        self.__indexList("dc_title", self.titleList)
        self.__indexList("dc_creator", self.creatorList)  #no dc_author in schema.xml, need to check
        self.__indexList("dc_contributor", self.contributorList)
        self.__indexList("dc_description", self.descriptionList)
        self.__indexList("dc_format", self.formatList)
        self.__indexList("dc_date", self.creationDate)
        self.__indexList("full_text", self.fulltext)
        for key in self.relationDict:
            self.__indexList(key, self.relationDict[key])

    def __dc(self):
        ### Check if dc.xml returned from ice exists.
        try:
            dcPayload = self.object.getPayload("dc.xml")
            self.utils.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
            dcXml = self.utils.getXmlDocument(dcPayload)
            if dcXml is not None:
                self.titleList = self.__getNodeValues(dcXml, "//dc:title")
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
            if self.titleList == []:
                titleCollection = container.getAll(NIE.title)
                iterator = titleCollection.iterator()
                while iterator.hasNext():
                    node = iterator.next()
                    result = str(node).strip()
                    self.titleList.append(result)

                titleCollection = container.getAll(NID3.title)
                iterator = titleCollection.iterator()
                while iterator.hasNext():
                    node = iterator.next()
                    result = str(node).strip()
                    self.titleList.append(result)

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
