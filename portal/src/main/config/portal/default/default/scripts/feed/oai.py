import random, time

from datetime import datetime

from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.api.storage import StorageException
from com.googlecode.fascinator.common import JsonSimpleConfig
from com.googlecode.fascinator.common.solr import SolrResult

from java.io import BufferedReader
from java.io import ByteArrayInputStream
from java.io import ByteArrayOutputStream
from java.io import InputStreamReader
from java.lang import Exception
from java.lang import StringBuilder
from java.lang import System
from java.sql import Timestamp

from org.apache.commons.lang import StringEscapeUtils

class ResumptionToken:
    def __init__(self, token=None, metadataPrefix="", nextToken="", resultJson="", sessionExpiry=300000):
        if token is None:
            random.seed()
            token = "%016x" % random.getrandbits(128)
        self.__token = token
        self.__metadataPrefix = metadataPrefix
        self.__nextToken = nextToken
        self.__resultJson = resultJson
        self.__expiry = System.currentTimeMillis() + sessionExpiry

    def getNextToken(self):
        return self.__nextToken
    
    def getResultJson(self):
        return self.__resultJson
    
    def getExpiry(self):
        return self.__expiry

    def getMetadataPrefix(self):
        return self.__metadataPrefix
   
    def getToken(self):
        return self.__token

    def resetExpiry(self, expiry):
        self.__expiry = System.currentTimeMillis() + expiry

    def setExpiry(self, expiry):
        self.__expiry = expiry


class TokensDatabase:
    def __init__(self, context):
        self.db = context["Services"].database
        self.log = context["log"]
        self.dbName = "oaiTokens"
        self.error = False
        self.errorMsg = ""
        
        # Does the database already exist?
        check = self.check()
        if check is None and not self.error:
            # No, create it now
            check = self.create()
            # And create our table
            if check is not None and not self.error:
                self.createTable()

        if self.error:
            self.log.error("Error during database startup: \n", self.errorMsg)
        else:
            self.log.info("Token database online")

    def check(self):
        try:
            return self.db.checkConnection(self.dbName)
        except Exception, e:
            msg = self.parseError(e)
            if msg == "Database does not exist":
                # Expected failure
                return None;
            else:
                # Something is wrong
                self.log.error("ERROR: ", e)
                self.error = True
                self.errorMsg = msg
                return None;

    def create(self):
        try:
            return self.db.getConnection(self.dbName)
        except Exception, e:
            self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = self.parseError(e)
            return None;

    def createTable(self):
        try:
            sql = """
CREATE TABLE resumptionTokens
(token VARCHAR(50) NOT NULL,
metadataPrefix VARCHAR(50) NOT NULL,
expiry TIMESTAMP NOT NULL,
resultJson LONG VARCHAR NOT NULL,
nextToken VARCHAR(50),
PRIMARY KEY (token))
"""
            index = "resumptionTokens-CREATE"
            self.db.execute(self.dbName, index, sql, None)
        except Exception, e:
            self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = self.parseError(e)

    def getError(self):
        return self.errorMsg

    def hasError(self):
        return self.error

    def storeToken(self, tokenObject):
        self.resetErrors()
        index = "resumptionTokens-STORE"
        table = "resumptionTokens"
        fields = {
            "token": tokenObject.getToken(),
            "metadataPrefix": tokenObject.getMetadataPrefix(),
            "expiry": Timestamp(tokenObject.getExpiry()),
            "nextToken": tokenObject.getNextToken(),
            "resultJson": tokenObject.getResultJson()
        }
        #self.log.debug("=== storeToken()")
        #self.log.debug("=== TOKEN: '{}'", tokenObject.getToken())
        #self.log.debug("=== METADATAPREFIX: '{}'", tokenObject.getMetadataPrefix())
        #self.log.debug("=== EXPIRY: '{}'", tokenObject.getExpiry())
        #self.log.debug("=== TOTALFOUND: '{}'", tokenObject.getTotalFound())
        #self.log.debug("=== START: '{}'", tokenObject.getStart())
        try:
            self.db.insert(self.dbName, index, table, fields)
        except Exception, e:
            msg = self.parseError(e)
            if msg == "Duplicate record!":
                # Expected failure
                self.log.error("Duplicate record already exists in table!")
            else:
                # Something is wrong
                self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = msg

    def removeToken(self, tokenObject):
        self.resetErrors()
        index = "resumptionTokens-DELETE"
        table = "resumptionTokens"
        fields = {
            "token": tokenObject.getToken()
        }
        try:
            self.db.delete(self.dbName, index, table, fields)
            self.log.info("Delete successful! TOKEN='{}'", tokenObject.getToken())
            return True
        except Exception, e:
            # Something is wrong
            self.log.error("Delete failed! TOKEN='{}'", tokenObject.getToken())
            self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = self.parseError(e)
            return False

    def updateToken(self, tokenObject):
        self.resetErrors()
        index = "resumptionTokens-UPDATE"
        sql = """
UPDATE resumptionTokens
SET    start = ?, expiry = ?
WHERE  token = ?
"""
        fields = [
            tokenObject.getStart(),
            Timestamp(tokenObject.getExpiry()),
            tokenObject.getToken()
        ]
        try:
            self.db.execute(self.dbName, index, sql, fields)
            #self.log.debug("=== updateToken()")
            #self.log.debug("=== TOKEN: '{}'", tokenObject.getToken())
            #self.log.debug("=== METADATAPREFIX: '{}'", tokenObject.getMetadataPrefix())
            #self.log.debug("=== EXPIRY: '{}'", tokenObject.getExpiry())
            #self.log.debug("=== TOTALFOUND: '{}'", tokenObject.getTotalFound())
            #self.log.debug("=== START: '{}'", tokenObject.getStart())
            return True
        except Exception, e:
            # Something is wrong
            self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = self.parseError(e)
            return False

    def getToken(self, tokenId):
        self.resetErrors()
        index = "resumptionTokens-GET"
        sql = """
SELECT *
FROM   resumptionTokens
WHERE  token = ?
"""
        fields = [tokenId]
        try:
            result = self.db.select(self.dbName, index, sql, fields)
            # Make sure we got a response
            if result is None or result.isEmpty():
                return None
            # Create the new token to return
            metadataPrefix = result.get(0).get("METADATAPREFIX")
            expiryStr = result.get(0).get("EXPIRY")
            # Jython does not support %f microseconds in time parsing, makes
            # this more awkward then it should be in 2.6+ Python
            # 1: split into basic time + micro seconds
            (basicTime, mSecs) = expiryStr.strip().split(".")
            # 2: Parse the basic time
            expiryDt = datetime.strptime(basicTime, "%Y-%m-%d %H:%M:%S")
            # 3: Convert into a 'epoch' long and then to a string (has an extra ".0" on the end)
            epoch = "%s" % time.mktime(expiryDt.timetuple())
            # 4: Remove the extraneous trailing zero and re-attach microseconds
            expiry = "%s%s" % (epoch.replace(".0", ""), mSecs)

            nextToken = result.get(0).get("NEXTTOKEN")
            resultJson = result.get(0).get("RESULTJSON")
            
            token = ResumptionToken(tokenId, metadataPrefix,nextToken,resultJson)
            token.setExpiry(expiry)
            
            return token
        except Exception, e:
            # Something is wrong
            self.log.error("ERROR: ", e)
            self.error = True
            self.errorMsg = self.parseError(e)
            return None

    # Strip out java package names from error strings.
    def parseError(self, error):
        self.has_error = True
        message = error.getMessage()
        i = message.find(":")
        if i != -1:
            return message[i + 1:].strip()
        else:
            return message.strip()

    def resetErrors(self):
        self.error = False
        self.errorMsg = ""

class OaiPmhError:
    def __init__(self, code, message):
        self.__code = code
        self.__message = message

    def getCode(self):
        return self.__code

    def getMessage(self):
        return self.__message

class OaiPmhVerb:
    def __init__(self, context, tokenDB, currentToken):
        self.log = context["log"]
        self.config = JsonSimpleConfig()
        formData = context["formData"]

        self.__error = None
        self.__verb = formData.get("verb")
        self.__metadataFormats = self.__metadataFormatList()
        self.__fromDate = None
        self.__untilDate = None
        self.log.debug(" * OAI Verb = '{}'", self.__verb)

        # No verb provided
        if self.__verb is None:
            self.__error = OaiPmhError("badVerb", "No verb was specified")

        # Some verbs require additional data
        elif self.__verb in ["GetRecord", "ListIdentifiers", "ListRecords"]:
            # What format metadata is requested?
            self.__metadataPrefix = formData.get("metadataPrefix")
            if self.__metadataPrefix is None:
                # No metadata supplied, error...
                #   unless the are resuming an earlier request
                if currentToken is not None:
                    # Make sure the resumption token hasn't expired
                    if currentToken.getExpiry() > System.currentTimeMillis():
                        # And retrieve the metadata prefix from the last request
                        self.__metadataPrefix = currentToken.getMetadataPrefix()

                    # Expired token, make sure it's not in the database anymore
                    else:
                        self.log.error("Using an expired token")
                        self.__error = OaiPmhError("badResumptionToken", "Token has expired")
                        success = tokenDB.removeToken(currentToken)
                        if not success:
                            self.log.error("Error removing expired token!")

                # No prefix and no token. We're done
                else:
                    attemptedToken = context["formData"].get("resumptionToken")
                    # Either they used an invalid token
                    if attemptedToken is not None:
                        self.log.error("Illegal resumption token: '{}'", attemptedToken)
                        self.__error = OaiPmhError("badResumptionToken", "Illegal resumption token")
                    # Or were missing their metadata prefix
                    else:
                        self.log.error("No metadata prefix supplied, and no token")
                        self.__error = OaiPmhError("badArgument", "Metadata prefix required")

            # These verbs require a metadata format... and we must be able to support it
            elif self.__metadataPrefix not in self.__metadataFormats:
                self.log.error("Metadata prefix is not valid for this view")
                self.__error = OaiPmhError("cannotDisseminateFormat",
                                           "Record not available as metadata type: %s" % self.__metadataPrefix)

            # These verbs allow for date limits, validate them if found
            if self.__verb in ["ListIdentifiers", "ListRecords"]:
                # From date
                fromStr = context["formData"].get("from")
                fromTIndex = None
                if fromStr is not None:
                    fromTIndex = fromStr.find("T")
                    # Basic dates
                    try:
                        if fromTIndex == -1:
                            self.__fromDate = datetime.strptime(fromStr, "%Y-%m-%d")
                        # Or datetimes
                        else:
                            self.__fromDate = datetime.strptime(fromStr, "%Y-%m-%dT%H:%M:%SZ")
                    except:
                        self.log.error("Invalid FROM date: '{}'", fromStr)
                        self.__error = OaiPmhError("badArgument", "From date not in valid format!")
                        return

                # Until Date
                untilStr = context["formData"].get("until")
                if untilStr is not None:
                    untilTIndex = untilStr.find("T")
                    # Granularity mismatches
                    if (fromTIndex is not None) and \
                            ((fromTIndex == -1 and untilTIndex != -1) or \
                            (fromTIndex != -1 and untilTIndex == -1)):
                        self.log.error("Date granularity mismatch: '{}' vs '{}'", fromStr, untilStr)
                        self.__error = OaiPmhError("badArgument", "Date granularity mismatch")
                        return
                    # Basic dates
                    try:
                        if untilTIndex == -1:
                            self.__untilDate = datetime.strptime(untilStr, "%Y-%m-%d")
                        # Or datetimes
                        else:
                            self.__untilDate = datetime.strptime(untilStr, "%Y-%m-%dT%H:%M:%SZ")
                    except:
                        self.log.error("Invalid UNTIL date: '{}'", untilStr)
                        self.__error = OaiPmhError("badArgument", "Until date not in valid format!")
                        return

                # Sanity check
                if self.__fromDate is not None and self.__untilDate is not None:
                    if self.__fromDate > self.__untilDate:
                        self.log.error("FROM date > UNTIL date: '{}' > '{}'", fromStr, untilStr)
                        self.__error = OaiPmhError("badArgument", "From date cannot be later then Until date!")
                        return

            # Check for a valid identifier
            if self.__verb == "GetRecord":
                id = context["formData"].get("identifier")
                if id is None or id == "":
                    self.log.error("GetRecord missing an identifier")
                    self.__error = OaiPmhError("badArgument", "Identifier required")
                    return

        # Basic verbs we will respond to easily
        elif self.__verb in ["Identify", "ListMetadataFormats", "ListSets"]:
            pass

        # Invalid verb
        else:
            self.log.error("Invalid verb provided: '{}'", self.__verb)
            self.__error = OaiPmhError("badVerb", "Unknown verb: '%s'" % self.__verb)

    def __metadataFormatList(self):
        metadataFormats = self.config.getObject(["portal", "oai-pmh", "metadataFormats"])
        metadataList = []
        for format in metadataFormats.keySet():
            metadataList.append(str(format))
        return metadataList

    def getError(self):
        return self.__error

    def setError(self, code, message):
        self.__error = OaiPmhError(code, message)

    def getVerb(self):
        return self.__verb

    def getMetadataPrefix(self):
        return self.__metadataPrefix

    def getIdentifier(self):
        return self.__identifier

    def getFromDate(self):
        return self.__fromDate

    def getUntilDate(self):
        return self.__untilDate

class OaiData:
    def __init__(self):
        self.tokensDB = None

    def __activate__(self, context):
        if self.tokensDB is None:
            self.tokensDB = TokensDatabase(context)

        # Set up configuration
        self.systemConfig = JsonSimpleConfig()
        self.oaiConfig = None
        self.getMetadataFormats()

        self.velocityContext = context
        self.services = context["Services"]
        self.log = context["log"]
        self.sessionState = context["sessionState"]
        self.portalDir = context["portalDir"]

        self.__result = None
        self.lastPage = False

        # Check if the OAI request has an overriding portal ('set') to the URL
        paramSet = self.vc("formData").get("set")
        self.__portalName = context["page"].getPortal().getName()
        illegalSet = False
        if paramSet is not None:
            portals = self.vc("page").getPortals().keySet()
            if portals.contains(paramSet):
                self.__portalName = paramSet
            else:
                illegalSet = True

        self.__metadataPrefix = ""
        self.__sessionExpiry = self.systemConfig.getInteger(None, ["portal", "oai-pmh", "sessionExpiry"])

        # Check if there's a resumption token in the formData
        self.__currentToken = None
        resumptionToken = self.vc("formData").get("resumptionToken")
        
        if resumptionToken is not None:
            token = self.tokensDB.getToken(resumptionToken)
            self.__currentToken = token

        # Process/parse the request we've received for validity
        self.vc("request").setAttribute("Content-Type", "text/xml")
        self.__request = OaiPmhVerb(context, self.tokensDB, self.__currentToken)
        if self.getError() is None and illegalSet:
            self.__request.setError("badArgument", "Set '%s' is not valid!" % paramSet)

        # If there are no errors... and the request requires some additional
        #  data (like a search result) do so now. Everything else can be
        #  handled in the templates.
        if self.getError() is None and \
                self.getVerb() in ["GetRecord", "ListIdentifiers", "ListRecords"]:

            # Find the metadata prefix requested
            self.__metadataPrefix = self.vc("formData").get("metadataPrefix")
            if self.__metadataPrefix is None:
                self.__metadataPrefix = self.__currentToken.getMetadataPrefix()

            if resumptionToken is None:
                self.__buildResumptionTokenSets()
            else:
                self.__result = SolrResult(self.__currentToken.getResultJson())
            
                    
            # Only list records if the metadata format is enabled in this view
            if self.isInView(self.__metadataPrefix) == False:
                self.__result = None
            

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            self.log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def isInView(self, format, view=None):
        # Sanity check
        if format is None or format == "":
            return False
        # Default to current poral
        if view is None:
            view = self.__portalName

        # Make sure there is some config for this format
        formatConfig = self.getMetadataFormats().get(format)
        if formatConfig is None:
            return False
        # Is it visible everywhere?
        allViews = formatConfig.getBoolean(False, ["enabledInAllViews"])
        if allViews:
            return True
        # Check if it is visible in this view
        else:
            allowedViews = formatConfig.getStringList(["enabledViews"])
            if view in allowedViews:
                return True
        # Rejection
        return False

    def getID(self, item):
        identifier = item.getFirst("oai_identifier")
        # Fallback to the default
        if identifier is None or identifier == "":
            return "oai:fascinator.usq.edu.au:" + item.getFirst("id")
        # Use the indexed value
        return identifier

    def isDeleted(self, item):
        return bool(item.getFirst("oai_deleted"))

    def getSet(self, item):
        set = item.getFirst("oai_set")
        # Fallback to the portal name
        if set is None or set == "":
            return self.__portalName
        # Use the required set
        return set

    def getVerb(self):
        return self.getRequest().getVerb()

    def getError(self):
        return self.getRequest().getError()

    def getResponseDate(self):
        return time.strftime("%Y-%m-%dT%H:%M:%SZ")

    def getRequest(self):
        return self.__request

    def getResult(self):
        return self.__result

    def getElement(self, elementName, values):
        elementStr = ""
        if values:
            for value in values:
                elementStr += "<%s>%s</%s>" % (elementName, value, elementName)
        return elementStr

    def __buildResumptionTokenSets(self):
        self.__result = SolrResult(None)

        portal = self.services.getPortalManager().get(self.__portalName)
        recordsPerPage = portal.recordsPerPage
        # Resolve our identifier
        id = self.vc("formData").get("identifier")
        query = "*:*"
        if id is not None and id != "":
            # A default TF2 OID
            if id.startswith("oai:fascinator.usq.edu.au:"):
                idString = id.replace("oai:fascinator.usq.edu.au:", "")
                idString = self.__escapeQuery(idString)
                query = "id:" + idString
            # Or a custom OAI ID
            else:
                idString = self.__escapeQuery(id)
                query = "oai_identifier:" + idString

        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", portal.facetFieldList)
        req.setParam("facet.limit", str(portal.facetCount))
        req.setParam("sort", "f_dc_title asc")

        portalQuery = portal.query
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.addParam("fq", "item_type:object")

        # Date data... is supplied
        fromDate = self.__request.getFromDate()
        untilDate = self.__request.getUntilDate()
        if fromDate is not None:
            fromStr = fromDate.isoformat() + "Z"
            self.log.debug("From Date: '{}'", fromStr)
            if untilDate is not None:
                untilStr = untilDate.isoformat() + "Z"
                self.log.debug("Until Date: '{}'", untilStr)
                queryStr = "last_modified:[%s TO %s]" % (fromStr, untilStr)
            else:
                queryStr = "last_modified:[%s TO *]" % (fromStr)
            self.log.debug("Date query: '{}'", queryStr)
            req.addParam("fq", queryStr)
        else:
            if untilDate is not None:
                untilStr = untilDate.isoformat() + "Z"
                self.log.debug("Until Date: '{}'", untilDate.isoformat())
                queryStr = "last_modified:[* TO %s]" % (untilStr)
                self.log.debug("Date query: '{}'", queryStr)
                req.addParam("fq", queryStr)

        # Check if there's resumption token exist in the formData
        start = 0
        

        req.setParam("start", str(start))

        out = ByteArrayOutputStream()
        self.services.indexer.search(req, out)
        self.__result = SolrResult(ByteArrayInputStream(out.toByteArray()))

        totalFound = self.__result.getNumFound()
        
        if totalFound > recordsPerPage:
            
            startRow = recordsPerPage
            random.seed()
            resumptionToken = "%016x" % random.getrandbits(128)
            
            nextResumptionToken = "%016x" % random.getrandbits(128)
            firstLoop = True
            while True:
                
                req.setParam("start", str(startRow)) 
                out = ByteArrayOutputStream()
                self.services.indexer.search(req, out)
                result = SolrResult(ByteArrayInputStream(out.toByteArray()))
                
                tokenObject = ResumptionToken(resumptionToken,self.__metadataPrefix,nextResumptionToken,result.toString())
                
                if firstLoop:
                    self.__currentToken = ResumptionToken(None,self.__metadataPrefix,resumptionToken,None) 
                    firstLoop = False
                    
                startRow = startRow + recordsPerPage
                self.log.debug("Resumption Token: " + nextResumptionToken)
                if startRow > totalFound:
                    self.log.debug(str(startRow) + " " + str(totalFound))
                    tokenObject = ResumptionToken(resumptionToken,self.__metadataPrefix,"",result.toString())
                    self.tokensDB.storeToken(tokenObject)
                    break
                self.tokensDB.storeToken(tokenObject)
                
                
                resumptionToken = nextResumptionToken
                nextResumptionToken = "%016x" % random.getrandbits(128)
                    
    def getToken(self):
        if self.isInView(self.__metadataPrefix) and not self.lastPage:
            return self.__currentToken
        return None

    def getMetadataFormats(self):
        if self.oaiConfig is None:
            self.oaiConfig = self.systemConfig.getJsonSimpleMap(["portal", "oai-pmh", "metadataFormats"])
        return self.oaiConfig

    def encodeXml(self, string):
        return StringEscapeUtils.escapeXml(string);

    def getPayload(self, oid, metadataFileName):
        # First get the Object from storage
        object = None
        try:
            object = self.services.getStorage().getObject(oid)
        except StorageException, e:
            return None

        # Check whether the payload exists
        try:
            return object.getPayload(metadataFileName)
        except StorageException, e:
            return None

    def getPayloadContent(self, payload):
        if payload is None:
            return ""

        try:
            sb = StringBuilder()
            reader = BufferedReader(InputStreamReader(payload.open(), "UTF-8"))
            line = reader.readLine()

            while line is not None:
                sb.append(line).append("\n")
                line = reader.readLine()
            payload.close()

            if sb:
                return sb
            return ""

        except Exception, e:
            return ""

    def __escapeQuery(self, q):
        temp = ""
        chars = "+-&|!(){}[]^\"~*?:\\"
        for c in q:
           if c in chars:
             temp += "\%s" % c
           else:
             temp += c
        return temp
