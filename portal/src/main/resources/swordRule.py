import md5, os, time
from com.googlecode.fascinator.indexer.rules import AddField, New
from org.dom4j.io import SAXReader

from com.googlecode.fascinator.common.nco import Contact
from com.googlecode.fascinator.common.nfo import PaginatedTextDocument
from com.googlecode.fascinator.common.nid3 import ID3Audio
from com.googlecode.fascinator.common.nie import InformationElement

from com.googlecode.fascinator.common.ctag import Tag, TaggedContent

#
# Available objects:
#    indexer   : Indexer instance
#    rules     : RuleManager instance
#    object    : DigitalObject to index
#    payloadId : Payload identifier
#    storageId : Storage layer identifier
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
solrId = object.getId();
if isMetadata:
    itemType = "object"
else:
    solrId += "/" + payloadId
    itemType = "datastream"
    rules.add(AddField("identifier", payloadId))

rules.add(AddField("id", solrId))
rules.add(AddField("storage_id", storageId))
rules.add(AddField("item_type", itemType))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("group_access", "guest"))

print "isMetadata='%s'" % isMetadata

if isMetadata:
    print 
    #only need to index metadata for the main object
    rules.add(AddField("repository_name", params["repository.name"]))
    rules.add(AddField("repository_type", params["repository.type"]))
    
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
    dcPayload = object.getPayload("dc.xml")
    if dcPayload is not None:
        pyUtils.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
        dcXml = pyUtils.getXmlDocument(dcPayload)
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

    metaPayload = object.getMetadata()
    if metaPayload.getId()=="imsmanifest.xml":
        #pyUtils.registerNamespace("ims", "http://www.imsglobal.org/xsd/imscp_v1p1")
        imsXml = pyUtils.getXmlDocument(metaPayload)
        if imsXml is not None:
            #get Title
            titleList = getNodeValues(imsXml, "//*[local-name()='title']")
            if len(titleList)>1:
                titleList = titleList[:1]
    else:
        rdfPayload = object.getMetadata()
        if rdfPayload is not None:
            rdfModel = pyUtils.getRdfModel(rdfPayload)

            #Seems like aperture only encode the spaces. Tested against special characters file name
            #and it's working
            oid = object.getId().replace(" ", "%20")
            #under windows we need to add a slash
            if not oid.startswith("/"):
                oid = "/" + oid
            rdfId = "file:%s" % oid

            #Set write to False so it won't write to the model
            paginationTextDocument = PaginatedTextDocument(rdfModel, rdfId, False)
            informationElement = InformationElement(rdfModel, rdfId, False)
            id3Audio = ID3Audio(rdfModel, rdfId, False)

            #1. get title only if no title returned by ICE
            if titleList == []:
                allTitles = informationElement.getAllTitle();
                while (allTitles.hasNext()):
                    title = allTitles.next().strip()
                    if title != "":
                        titleList.append(title)
                allTitles = id3Audio.getAllTitle()
                while (allTitles.hasNext()):
                    title = allTitles.next().strip()
                    if title != "":
                        titleList.append(title)

            #use id/filename if no title
            if titleList == []:
               title = os.path.split(object.getId())[1]
               titleList.append(title)

            #2. get creator only if no creator returned by ICE
            if creatorList == []:
                allCreators = paginationTextDocument.getAllCreator();
                while (allCreators.hasNext()):
                    thing = allCreators.next()
                    contacts = Contact(rdfModel, thing.getResource(), False)
                    allFullnames = contacts.getAllFullname()
                    while (allFullnames.hasNext()):
                         creatorList.append(allFullnames.next())

            #3. getFullText: only aperture has this information
            if informationElement.hasPlainTextContent():
                allPlainTextContents = informationElement.getAllPlainTextContent()
                while(allPlainTextContents.hasNext()):
                    fulltextString = allPlainTextContents.next()
                    fulltext.append(fulltextString)

                    #4. description/abstract will not be returned by aperture, so if no description found
                    # in dc.xml returned by ICE, put first 100 characters
                    if descriptionList == []:
                        descriptionString = fulltextString
                        if len(fulltextString)>100:
                            descriptionString = fulltextString[:100] + "..."
                        descriptionList.append(descriptionString)

            if id3Audio.hasAlbumTitle():
                albumTitle = id3Audio.getAllAlbumTitle().next().strip()
                descriptionList.append("Album: " + albumTitle)

            #5. mimeType: only aperture has this information
            if informationElement.hasMimeType():
                allMimeTypes = informationElement.getAllMimeType()
                while(allMimeTypes.hasNext()):
                    formatList.append(allMimeTypes.next())

            #6. contentCreated
            if creationDate == []:
                if informationElement.hasContentCreated():
                    creationDate.append(informationElement.getContentCreated().getTime().toString())

    indexList("dc_title", titleList)
    indexList("dc_creator", creatorList)  #no dc_author in schema.xml, need to check
    indexList("dc_contributor", contributorList)
    indexList("dc_description", descriptionList)
    indexList("dc_format", formatList)
    indexList("dc_date", creationDate)
    
    for key in relationDict:
        indexList(key, relationDict[key])
    
    indexList("full_text", fulltext)
    indexPath("file_path", object.getId().replace("\\", "/"), includeLastPart=False)
    
    tagsRdf = object.getPayload("tags.rdf")
    if tagsRdf is not None:
        tagsModel = pyUtils.getRdfModel(tagsRdf)
        hashId = md5.new(object.getId()).hexdigest()
        content = TaggedContent(tagsModel, "urn:" + hashId, False);
        tags = content.getAllTagged_as().asArray()
        for tag in tags:
            labels = tag.getAllTaglabel_asNode_().asArray()
            for label in labels:
                rules.add(AddField("tag", label.toString()))
