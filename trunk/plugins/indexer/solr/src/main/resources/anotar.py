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
        self.indexer = context["indexer"]

        # Common data
        self.__newDoc()

        ##self.__security()

    def __get(self, path, default=None):
        return self.json.getString(default, path)

    def __index(self, field, path, default=None):
        data = self.__get(path, default)
        self.utils.add(self.index, field, data)

    def __newDoc(self):
        # Read the payload json string into
        # a JsonConfigHelper object.
        self.oid = self.object.getId()
        self.pid = self.payload.getId()

        self.json = self.utils.getJsonObject(self.payload.open())
        self.payload.close()

        self.__index("schemaVersion",    ["schemaVersionUri"])
        self.__index("clientVersion",    ["clientVersionUri"])
        self.__index("id",               ["id"])
        self.__index("uri",              ["uri"])
        self.__index("type",             ["type"])
        self.__index("titleLiteral",     ["title", "literal"],            "")
        self.__index("titleUri",         ["title", "uri"],                "")
        self.__index("annotatesLiteral", ["annotates", "literal"],        "")
        self.__index("annotatesUri",     ["annotates", "uri"])
        self.__index("rootUri",          ["annotates", "rootUri"])
        self.__index("creatorLiteral",   ["creator", "literal"])
        self.__index("creatorUri",       ["creator", "uri"])
        self.__index("creatorEmail",     ["creator", "email", "literal"], "")
        self.__index("creatorEmailMd5",  ["creator", "email", "md5hash"], "")
        self.__index("contentType",      ["content", "mimeType"])
        self.__index("contentLiteral",   ["content", "literal"])
        self.__index("isPrivate",        ["isPrivate"])
        self.__index("lang",             ["lang"])

        # Date handling, Solr only accepts UTC
        #http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html
        dateCreated = self.__get(["dateCreated", "literal"])
        if dateCreated is not None:
            self.utils.add(self.index, "dateCreated", dateCreated[:19] + "Z")
            self.utils.add(self.index, "tzCreated", dateCreated[19:])
        dateModified = self.__get(["dateModified", "literal"])
        if dateModified is not None:
            self.utils.add(self.index, "dateModified", dateModified[19:] + "Z")
            self.utils.add(self.index, "tzModified", dateModified[19:])

        # Arrays
        for locator in self.json.getJsonSimpleList(["annotates", "locators"]):
            self.utils.add(self.index, "locators", locator.toString())
            self.utils.add(self.index, "locatorValue", locator.getString(None, ["value"]))
            self.utils.add(self.index, "locatorContent", locator.getString(None, ["originalContent"]))

        # Our full string
        self.utils.add(self.index, "jsonString", self.json.toString())

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
