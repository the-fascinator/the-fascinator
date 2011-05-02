import md5, os, time
from au.edu.usq.fascinator.indexer.rules import AddField, New
from sets import Set
import org.ontoware.rdf2go as rdf2go
import java.io as io


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

def indexList(name, values):
    for value in values:
        rules.add(AddField(name, value))
        
def runQuery(query):
    q = "PREFIX bib: <http://purl.org/net/biblio#>\n \
    PREFIX dc: <http://purl.org/dc/elements/1.1/>\n \
    PREFIX dcterms: <http://purl.org/dc/terms/>\n \
    PREFIX prism: <http://prismstandard.org/namespaces/1.2/basic/>\n \
    PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n \
    PREFIX vcard: <http://nwalsh.com/rdf/vCard#>\n \
    PREFIX link: <http://purl.org/rss/1.0/modules/link/>\n \
    PREFIX z: <http://www.zotero.org/namespaces/export#>\n" \
    + query
    print q
    result = model.sparqlSelect(q)
    return result

def addValue(list, node):
    if node:
        list.add(node.toString())

def getDocumentDetails(id):
    queryString = "SELECT ?id ?title ?type ?subject ?date \
            WHERE { OPTIONAL {" + id + " dc:title ?title } \
            . OPTIONAL {" + id + " dc:identifier ?id} \
            . OPTIONAL {" + id + " dc:subject ?subject} \
            . OPTIONAL {" + id + " dc:date ?date} \
            . OPTIONAL {" + id + " z:itemType ?type " + "}}"

    result = runQuery(queryString)
    for row in result:
        print "Record: " + row.toString()
        addValue(titleList, row.getValue("title"))
        addValue(idList, row.getValue("id"))
        addValue(subjectList, row.getValue("subject"))
        addValue(dateList, row.getValue("date"))
        addValue(typeList, row.getValue("type"))
        
        

def getDocuments():
    result = runQuery("SELECT ?document WHERE { { ?document a bib:Document } \
    UNION { ?document a bib:Article} \
    UNION { ?document a bib:AcademicArticle} \
    UNION { ?document a bib:AudioDocument} \
    UNION { ?document a bib:AudioVisualDocument} \
    UNION { ?document a bib:Film} \
    UNION { ?document a bib:Book} \
    UNION { ?document a bib:Proceedings} \
    UNION { ?document a bib:CollectedDocument} \
    UNION { ?document a bib:EditedBook} \
    UNION { ?document a bib:Issue} \
    UNION { ?document a bib:DocumentPart} \
    UNION { ?document a bib:BookSection} \
    UNION { ?document a bib:Excerpt} \
    UNION { ?document a bib:Slide} \
    UNION { ?document a bib:Image} \
    UNION { ?document a bib:Map} \
    UNION { ?document a bib:LegalDocument} \
    UNION { ?document a bib:LegalCaseDocument} \
    UNION { ?document a bib:Legislation} \
    UNION { ?document a bib:Manual} \
    UNION { ?document a bib:Manuscript} \
    UNION { ?document a bib:Note} \
    UNION { ?document a bib:Patent} \
    UNION { ?document a bib:PersonalCommunicationDocument} \
    UNION { ?document a bib:Email} \
    UNION { ?document a bib:Letter} \
    UNION { ?document a bib:ReferenceSource} \
    UNION { ?document a bib:Report} \
    UNION { ?document a bib:Slideshow} \
    UNION { ?document a bib:Standard} \
    UNION { ?document a bib:Thesis} \
    UNION { ?document a bib:Webpage} \
    }")
    for row in result:
        print "Record: " + row.toString()
        getDocumentDetails(row.getValue("document").toSPARQL())


#start with blank solr document
rules.add(New())

titleList = Set()
idList = Set()
subjectList = Set()
dateList = Set()
typeList = Set()

#common fields
oid = object.getId()
rules.add(AddField("id", oid))
rules.add(AddField("storage_id", oid))
rules.add(AddField("item_type", "object"))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("harvest_config", params.getProperty("jsonConfigOid")))
rules.add(AddField("harvest_rules",  params.getProperty("rulesOid")))

rules.add(AddField("repository_name", params["repository.name"]))
rules.add(AddField("repository_type", params["repository.type"]))

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

model = rdf2go.RDF2Go.getModelFactory().createModel()
model.open()
model.readFrom(io.FileReader(io.File(oid)))
getDocuments()
model.close()

indexList("dc_title", titleList)
indexList("dc_identifier", idList)
indexList("dc_subject", subjectList)
indexList("dc_type", typeList)
#indexList("dc_contributor", contributorList)
#indexList("dc_description", descriptionList)
#indexList("dc_format", formatList)
indexList("dc_date", dateList)



