import time
import re

from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.indexer.rules import AddField, New

from java.io import BufferedReader
from java.io import InputStreamReader
from java.lang import StringBuilder

#
# Available objects:
#    indexer    : Indexer instance
#    jsonConfig : JsonConfigHelper of our harvest config file
#    rules      : RuleManager instance
#    object     : DigitalObject to index
#    payload    : Payload to index
#    params     : Metadata Properties object
#    pyUtils    : Utility object for accessing app logic
#

def indexPath(name, path, includeLastPart=True):
    parts = path.split("/")
    length = len(parts)
    if includeLastPart:
        length +=1
    for i in range(1, length):
        part = "/".join(parts[:i])
        if part != "":
            if part.startswith("/"):
                part = part[1:]
            rules.add(AddField(name, part))

def indexList(name, values):
    for value in values:
        rules.add(AddField(name, value))

def getNodeValues (doc, xPath):
    nodes = doc.selectNodes(xPath)
    valueList = []
    if nodes:
        for node in nodes:
            #remove duplicates:
            nodeValue = node.getText()
            if nodeValue not in valueList:
                valueList.append(node.getText())
    return valueList

#start with blank solr document
rules.add(New())

#common fields
oid = object.getId()
pid = payload.getId()
metaPid = params.getProperty("metaPid", "DC")
if pid == metaPid:
    itemType = "object"
else:
    oid += "/" + pid
    itemType = "datastream"
    rules.add(AddField("identifier", pid))

rules.add(AddField("id", oid))
rules.add(AddField("storage_id", oid))
rules.add(AddField("item_type", itemType))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("harvest_config", params.getProperty("jsonConfigOid")))
rules.add(AddField("harvest_rules",  params.getProperty("rulesOid")))

# Security
roles = pyUtils.getRolesWithAccess(oid)
if roles is not None:
    for role in roles:
        rules.add(AddField("security_filter", role))
else:
    # Default to guest access if Null object returned
    schema = pyUtils.getAccessSchema("derby");
    schema.setRecordId(oid)
    schema.set("role", "guest")
    pyUtils.setAccessSchema(schema, "derby")
    rules.add(AddField("security_filter", "guest"))

if pid == metaPid:
    previewPid = None
    for payloadId in object.getPayloadIdList():
        try:
            payload = object.getPayload(payloadId)
            if str(payload.getType())=="Thumbnail":
                rules.add(AddField("thumbnail", payload.getId()))
            elif str(payload.getType())=="Preview":
                previewPid = payload.getId()
                rules.add(AddField("preview", previewPid))
            elif str(payload.getType())=="AltPreview":
                rules.add(AddField("altpreview", payload.getId()))
        except Exception, e:
            pass
    #only need to index metadata for the main object
    rules.add(AddField("repository_name", params["repository.name"]))
    rules.add(AddField("repository_type", params["repository.type"]))

    #Course data
    cYear = params["usq.year"]
    rules.add(AddField("course_year", cYear))
    cSem = params["usq.semester"]
    if cSem == "":
        cSem = params.getProperty("usq-semester")
    rules.add(AddField("course_sem", cSem))
    cCourse = params["usq.course"]
    if cCourse == "":
        cCourse = params.getProperty("usq-course")
    rules.add(AddField("course_code", cCourse))

    itemType = params.getProperty("item-type")
    if itemType is not None:
        rules.add(AddField("dc_type", itemType))

    titleList = []
    descriptionList = []
    creatorList = []
    creationDate = []
    contributorList = []
    approverList = []
    formatList = []
    fulltext = []
    relationDict = {}


    ### Check if dc.xml returned from ice is exist or not. if not... process the dc-rdf
    try:
        dcPayload = object.getPayload("dc.xml")
        # Stream the Payload into a string
        sb = StringBuilder()
        reader = BufferedReader(InputStreamReader(dcPayload.open(), "UTF-8"))
        line = reader.readLine()
        while line is not None:
            sb.append(line).append("\n")
            line = reader.readLine()
        dcPayload.close()
        # Convert the Java String to Python
        pyString = str(sb.toString())
        # Find the escaped characters and replace with bytes
        pattern = "(?:&#(\d{3,3});)"
        replaceFunc = lambda(x): r"%s" % chr(int(x.group(1)))
        pyString = re.sub(pattern, replaceFunc, pyString)
        # Now decode to UTF-8
        utf8String = pyString.decode("utf-8")
        # And parse the string through java
        pyUtils.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
        dcXml = pyUtils.getXmlDocument(utf8String)
        if dcXml is not None:
            #get Title
            titleList = getNodeValues(dcXml, "//dc:title")
            #get abstract/description
            descriptionList = getNodeValues(dcXml, "//dc:description")
            #get creator
            creatorList = getNodeValues(dcXml, "//dc:creator")
            #get contributor list
            contributorList = getNodeValues(dcXml, "//dc:contributor")
            #get creation date
            creationDate = getNodeValues(dcXml, "//dc:issued")
            #ice metadata stored in dc:relation as key::value
            relationList = getNodeValues(dcXml, "//dc:relation")
            for relation in relationList:
                key, value = relation.split("::")
                value = value.strip()
                key = key.replace("_5f","") #ICE encoding _ as _5f?
                if relationDict.has_key(key):
                    relationDict[key].append(value)
                else:
                    relationDict[key] = [value]
    except StorageException, e:
        #print "Failed to index ICE dublin core data (%s)" % str(e)
        pass

    # Extract from aperture.rdf if exist
    try:
        from org.semanticdesktop.aperture.vocabulary import NCO;
        from org.semanticdesktop.aperture.vocabulary import NID3;
        from org.semanticdesktop.aperture.vocabulary import NIE;
        from org.semanticdesktop.aperture.rdf.impl import RDFContainerImpl;
        from org.ontoware.rdf2go.model.node.impl import URIImpl;

        rdfPayload = object.getPayload("aperture.rdf")
        rdfModel = pyUtils.getRdfModel(rdfPayload)
        #rdfModel.dump()

        #Seems like aperture only encode the spaces. Tested against special characters file name
        #and it's working
        safeOid = oid.replace(" ", "%20")
        rdfId = "urn:oid:%s" % safeOid.rstrip("/")

        container = RDFContainerImpl(rdfModel, rdfId)

        #1. get title only if no title returned by ICE
        if titleList == []:
            titleCollection = container.getAll(NIE.title)
            iterator = titleCollection.iterator()
            while iterator.hasNext():
                node = iterator.next()
                result = str(node).strip()
                titleList.append(result)

            titleCollection = container.getAll(NID3.title)
            iterator = titleCollection.iterator()
            while iterator.hasNext():
                node = iterator.next()
                result = str(node).strip()
                titleList.append(result)

        #2. get creator only if no creator returned by ICE
        if creatorList == []:
            creatorCollection = container.getAll(NCO.creator);
            iterator = creatorCollection.iterator()
            while iterator.hasNext():
                node = iterator.next()
                creatorUri = URIImpl(str(node))
                creatorContainer = RDFContainerImpl(rdfModel, creatorUri);
                value = creatorContainer.getString(NCO.fullname);
                if value and value not in creatorList:
                    creatorList.append(value)

        #3. getFullText: only aperture has this information
        fulltextString = container.getString(NIE.plainTextContent)
        if fulltextString:
            fulltext.append(fulltextString.strip())
            #4. description/abstract will not be returned by aperture, so if no description found
            # in dc.xml returned by ICE, put first 100 characters
            if descriptionList == []:
                descriptionString = fulltextString
                if len(fulltextString)>100:
                    descriptionString = fulltextString[:100] + "..."
                descriptionList.append(descriptionString)

        #4 album title
        albumTitle = container.getString(NID3.albumTitle)
        if albumTitle:
            descriptionList.append("Album: " + albumTitle.strip())

        #5. mimeType: only aperture has this information
        mimeType = container.getString(NIE.mimeType)
        if mimeType:
            formatList.append(mimeType.strip())

        #6. contentCreated
        if creationDate == []:
            contentCreated = container.getString(NIE.contentCreated)
            if contentCreated:
                creationDate.append(contentCreated.strip())
    except StorageException, e:
        #print "Failed to index aperture data (%s)" % str(e)
        pass

    ### Check if ffmpeg.info exists or not
    try:
        ffmpegPayload = object.getPayload("ffmpeg.info")
        ffmpeg = pyUtils.getJsonObject(ffmpegPayload.open())
        ffmpegPayload.close()
        if ffmpeg is not None:
            # Dimensions
            width = ffmpeg.get("video/width")
            height = ffmpeg.get("video/height")
            if width is not None and height is not None:
                rules.add(AddField("dc_size", width + " x " + height))

            # Duration
            duration = ffmpeg.get("duration")
            if duration is not None and int(duration) > 0:
                if int(duration) > 59:
                    secs = int(duration) % 60
                    mins = (int(duration) - secs) / 60
                    rules.add(AddField("dc_duration", "%dm %ds" % (mins, secs)))
                else:
                    rules.add(AddField("dc_duration", duration + " second(s)"))

            # Format
            media = ffmpeg.get("format/label")
            if media is not None:
                rules.add(AddField("dc_media_format", media))

            # Video
            codec = ffmpeg.get("video/codec/simple")
            label = ffmpeg.get("video/codec/label")
            if codec is not None and label is not None:
                rules.add(AddField("video_codec_simple", codec))
                rules.add(AddField("video_codec_label", label))
                rules.add(AddField("meta_video_codec", label + " (" + codec + ")"))
            else:
                if codec is not None:
                    rules.add(AddField("video_codec_simple", codec))
                    rules.add(AddField("meta_video_codec", codec))
                if label is not None:
                    rules.add(AddField("video_codec_label", label))
                    rules.add(AddField("meta_video_codec", label))
            pixel_format = ffmpeg.get("video/pixel_format")
            if pixel_format is not None:
                rules.add(AddField("meta_video_pixel_format", pixel_format))

            # Audio
            codec = ffmpeg.get("audio/codec/simple")
            label = ffmpeg.get("audio/codec/label")
            if codec is not None and label is not None:
                rules.add(AddField("audio_codec_simple", codec))
                rules.add(AddField("audio_codec_label", label))
                rules.add(AddField("meta_audio_codec", label + " (" + codec + ")"))
            else:
                if codec is not None:
                    rules.add(AddField("audio_codec_simple", codec))
                    rules.add(AddField("meta_audio_codec", codec))
                if label is not None:
                    rules.add(AddField("audio_codec_label", label))
                    rules.add(AddField("meta_audio_codec", label))
            sample_rate = ffmpeg.get("audio/sample_rate")
            if sample_rate is not None:
                sample_rate = "%.1f KHz" % (int(sample_rate) / 1000)
            channels = ffmpeg.get("audio/channels")
            if channels is not None:
                channels += " Channel(s)"
            if sample_rate is not None and channels is not None:
                rules.add(AddField("meta_audio_details", sample_rate + ", " + channels))
            else:
                if sample_rate is not None:
                    rules.add(AddField("meta_audio_details", sample_rate))
                if channels is not None:
                    rules.add(AddField("meta_audio_details", channels))
    except StorageException, e:
        #print "Failed to index FFmpeg metadata (%s)" % str(e)
        pass

    # some defaults if the above failed
    if titleList == []:
       #use object's source id (i.e. most likely a filename)
       titleList.append(object.getSourceId())

    if formatList == []:
        payload = object.getPayload(object.getSourceId())
        formatList.append(payload.getContentType())

    indexList("dc_title", titleList)
    indexList("dc_creator", creatorList)  #no dc_author in schema.xml, need to check
    indexList("dc_contributor", contributorList)
    indexList("dc_description", descriptionList)
    indexList("dc_format", formatList)
    indexList("dc_date", creationDate)

    for key in relationDict:
        indexList(key, relationDict[key])

    indexList("full_text", fulltext)
    baseFilePath = params["base.file.path"]
    filePath = object.getMetadata().getProperty("file.path")
    if baseFilePath:
        # NOTE: need to change again if the json file accept forward slash
        #       in window get the base folder
        baseFilePath = baseFilePath.replace("\\", "/")
        if not baseFilePath.endswith("/"):
           baseFilePath = "%s/" % baseFilePath
        baseDir = baseFilePath[baseFilePath.rstrip("/").rfind("/")+1:]
        filePath = filePath.replace("\\", "/").replace(baseFilePath, baseDir)
    indexPath("file_path", filePath, includeLastPart=False)

    # check the object metadata for display type set by harvester or transformer
    # otherwise determine the display type by mime type
    displayType = params.getProperty("displayType")
    if not displayType:
        displayType = formatList[0]
        if displayType is not None:
            rules.add(AddField("display_type", pyUtils.basicDisplayType(displayType)))
    else:
        self.utils.add(self.index, "display_type", displayType)
    # Some object use a special preview template. eg. word docs with a html preview
    previewType = params.getProperty("previewType")
    if not previewType:
        previewType = pyUtils.getDisplayMimeType(formatList, object, previewPid)
        if previewType is not None and previewType != displayType:
            rules.add(AddField("preview_type", pyUtils.basicDisplayType(previewType)))
    else:
        self.utils.add(self.index, "preview_type", previewType)
