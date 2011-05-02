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
        self.__fixRenderQueues()

        # Real metadata
        if self.itemType == "object":
            self.__previews()
            self.__basicData()
            self.__marcData()

    def __basicData(self):
        self.utils.add(self.index, "repository_name", self.params["repository.name"])
        self.utils.add(self.index, "repository_type", self.params["repository.type"])

    def __fixRenderQueues(self):
        # On the first index after a harvest we need to put the transformer
        #    back into the picture for reharvest actions to work.
        renderer = self.params.getProperty("renderQueue")
        if renderer is not None and renderer == "":
            self.params.setProperty("renderQueue", "solrmarc");
            self.params.setProperty("objectRequiresClose", "true");

    def __marcData(self):
        ### Index the marc metadata extracted from solrmarc
        try:
            marcPayload = self.object.getPayload("metadata.json")
            marc = self.utils.getJsonObject(marcPayload.open())
            marcPayload.close()
            if marc is not None:
                coreFields = {
                    "id" : "storage_id",
                    "recordtype" : "recordtype",
                    "title" : "dc_title",
                    "callnumber" : "callnumber",
                    "format" : "dc_format",
                    "isbn" : "isbn",
                    "issn" : "issn",
                    "author" : "dc_creator",
                    "edition" : "edition",
                    "publishDate" : "dc_date",
                    "url" : "url"
                }

                for k,v in coreFields.iteritems():
                    self.__mapVuFind(v, k, marc)

        except StorageException, e:
            print "Could not find marc data (%s)" % str(e)

    def __mapVuFind(self, ourField, theirField, map):
        for value in map.geStringtList(theirField):
            self.utils.add(self.index, ourField, value)

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
        self.utils.add(self.index, "item_type", self.itemType)
        self.utils.add(self.index, "last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ"))
        self.utils.add(self.index, "harvest_config", self.params.getProperty("jsonConfigOid"))
        self.utils.add(self.index, "harvest_rules",  self.params.getProperty("rulesOid"))
        self.utils.add(self.index, "display_type", "solrmarc")

    def __previews(self):
        for payloadId in self.object.getPayloadIdList():
            try:
                payload = self.object.getPayload(payloadId)
                if str(payload.getType())=="Thumbnail":
                    self.utils.add(self.index, "thumbnail", payload.getId())
                elif str(payload.getType())=="Preview":
                    self.utils.add(self.index, "preview", payload.getId())
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
