import java.lang.Exception as JavaException
import java.util.Date as JavaDate;
import java.sql.Timestamp as JavaTimestamp;

from org.apache.commons.lang import StringEscapeUtils

class AgreementData:
    def __init__(self):
        self.__initialised = False

    def __activate__(self, context, metadata):
        if not self.__initialised:
            # Be very careful not to use request specific bindings from the context
            self.__log = context["log"]
            self.__db = context["Services"].database
            self.__dbName = "userAgreements"
            self.__dbError = False
            self.__dbErrorMsg = ""
            self.__dbInit()
            self.__initialised = True

        self.__metadata = metadata
        self.__object = self.__metadata.get("id")

        # Database stuff
        self.__dbError = False
        self.__dbErrorMsg = ""

        # User stuff
        self.__auth = context["page"].authentication
        self.__loggedIn = self.isLoggedIn()
        if self.__loggedIn:
            self.__user = self.getUser()
        else:
            self.__user = None

        # Form processing
        self.processForm(context["formData"])

    def acceptButton(self):
        defaultLabel = "I Accept"
        return self.__metadata.getString(defaultLabel, "user_agreement_accept")

    def cancelButton(self):
        defaultLabel = "No Thanks"
        return self.__metadata.getString(defaultLabel, "user_agreement_cancel")

    def getText(self):
        return StringEscapeUtils.escapeHtml(self.__metadata.get("user_agreement_text"))

    def getTitle(self):
        defaultTitle = "User Agreement"
        return self.__metadata.getString(defaultTitle, "user_agreement_title")

    def getUser(self):
        return self.__auth.get_username()

    def hasAccepted(self):
        if self.__user is None:
            self.__log.error("No username")
            return False
        if self.__object is None:
            self.__log.error("Error on page, no OID provided")
            return False
        return self.__dbVerify(self.__user, self.__object)

    def hasText(self):
        agreement = self.getText()
        if agreement is not None and agreement != "":
            return True
        return False

    def isAccessible(self):
        if self.hasText():
            return self.__loggedIn
        else:
            return True

    def isLoggedIn(self):
        return self.__auth.is_logged_in()

    def isRequired(self):
        # Does the object even have an agreement?
        if self.hasText():
            # And the user is logged in (to accept)
            if self.__loggedIn:
                # And they haven't accepted previously
                if not self.hasAccepted():
                    # Yes an agreement is required
                    return True
        # Default... no agreement required
        return False

    def processForm(self, form):
        #self.__log.debug("===== processForm()")
        if form is not None:
            accept = form.get("accept")
            if accept is None or accept != "true":
                return
            if self.__user is None:
                self.__log.error("User Agreement acceptance recieved and no user logged in")
                return
            if self.__object is None:
                self.__log.error("User Agreement acceptance recieved and no object found")
                return
            # The planets have aligned
            self.__dbAgree(self.__user, self.__object)

###########
# Methods relating to database interaction
###########

    def __dbInit(self):
        #self.__log.debug("===== __dbInit()")
        # Does the database already exist?
        check = self.__dbCheck()
        if check is None and not self.__dbHasError():
            # No, create it now
            check = self.__dbCreate()
            # And create our table
            if check is not None and not self.__dbHasError():
                self.__dbCreateTable()

    def __dbCheck(self):
        #self.__log.debug("===== __dbCheck()")
        try:
            return self.__db.checkConnection(self.__dbName)
        except JavaException, e:
            msg = self.__dbParseError(e)
            if msg == "Database does not exist":
                # Expected failure
                return None;
            else:
                # Something is wrong
                self.__log.error("Error connecting to database:", e)
                self.__dbError = True
                self.__dbErrorMsg = msg
                return None;

    def __dbCreate(self):
        #self.__log.debug("===== __dbCreate()")
        try:
            return self.__db.getConnection(self.__dbName)
        except JavaException, e:
            self.__log.error("Error creating database:", e)
            self.__dbError = True
            self.__dbErrorMsg = self.__dbParseError(e)
            return None;

    def __dbCreateTable(self):
        #self.__log.debug("===== __dbCreateTable()")
        try:
            sql = """
CREATE TABLE agreements
(objectId VARCHAR(255) NOT NULL,
username VARCHAR(255) NOT NULL,
datetime TIMESTAMP NOT NULL,
PRIMARY KEY (objectId, username))
"""
            index = "userAgreements-CREATE"
            self.__db.execute(self.__dbName, index, sql, None)
        except JavaException, e:
            self.__log.error("Error creating table:", e)
            self.__dbError = True
            self.__dbErrorMsg = self.__dbParseError(e)

    def __dbGetError(self):
        return self.__dbErrorMsg

    def __dbHasError(self):
        return self.__dbError

    # Strip out java package names from error strings.
    def __dbParseError(self, error):
        self.has_error = True
        message = error.getMessage()
        i = message.find(":")
        if i != -1:
            return message[i+1:].strip()
        else:
            return message.strip()

    def __dbResetErrors(self):
        self.__dbError = False
        self.__dbErrorMsg = ""

    def __dbAgree(self, username, objectId):
        #self.__log.debug("===== __dbAgree('{}', '{}')", username, objectId)
        self.__dbResetErrors()
        index = "userAgreements-STORE"
        table = "agreements"
        fields = {
            "objectId": objectId,
            "username": username,
            "datetime": JavaTimestamp(JavaDate().getTime())
        }
        try:
            self.__db.insert(self.__dbName, index, table, fields)
        except JavaException, e:
            msg = self.__dbParseError(e)
            if msg == "Duplicate record!":
                # Expected failure
                self.__log.error("Duplicate record already exists in table! USERNAME='{}' OID='{}'", username, objectId)
            else:
                # Something is wrong
                self.__log.error("Error accepting agreement:", e)
            self.__dbError = True
            self.__dbErrorMsg = msg

    def __dbRemove(self, username, objectId):
        #self.__log.debug("===== __dbRemove('{}', '{}')", username, objectId)
        self.__dbResetErrors()
        index = "userAgreements-DELETE"
        table = "agreements"
        fields = {
            "objectId": objectId,
            "username": username
        }
        try:
            self.__db.delete(self.__dbName, index, table, fields)
            self.__log.info("Delete successful! USERNAME='{}' OID='{}'", username, objectId)
        except JavaException, e:
            # Something is wrong
            self.__log.error("Delete failed! USERNAME='{}' OID='{}'", username, objectId)
            self.__log.error("ERROR:", e)
            self.__dbError = True
            self.__dbErrorMsg = self.__dbParseError(e)

    def __dbVerify(self, username, objectId):
        #self.__log.debug("===== __dbVerify('{}', '{}')", username, objectId)
        self.__dbResetErrors()
        index = "userAgreements-CHECK"
        sql = """
SELECT count(*) as total
FROM   agreements
WHERE  objectId = ?
AND    username = ?
"""
        fields = [objectId, username]
        try:
            result = self.__db.select(self.__dbName, index, sql, fields)
            # Make sure we got a response
            if result is None or result.isEmpty():
                return False
            # And that is was as expected
            total = result.get(0).get("TOTAL")
            if total is None or total != "1":
                return False
            # All is good
            return True
        except JavaException, e:
            # Something is wrong
            self.__log.error("Error verifying agreement:", e)
            self.__dbError = True
            self.__dbErrorMsg = self.__dbParseError(e)
            return False
