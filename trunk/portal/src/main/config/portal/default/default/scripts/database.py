import java.lang.Exception as JavaException
import java.util.Date as JavaDate;
import java.sql.Timestamp as JavaTimestamp;

class DatabaseData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        self.__db = Services.database
        self.__error = False
        self.__errorMsg = ""
        self.__dbName = "test-userAgreements"

        # Does the database already exist?
        check = self.check()
        if check is None and not self.__error:
            # No, create it now
            check = self.create()
            # And create our table
            if check is not None and not self.__error:
                self.createTable()

        if not self.__error:
            # Check they have an agreement
            if self.verify("bob", "object"):
                print "1: TRUE"
            else:
                print "1: FALSE"

            # Create an agreement
            if not self.__error:
                self.agree("bob", "object")
                # Check again
                if not self.__error and self.verify("bob", "object"):
                    print "2: TRUE"
                    # Remove their agreement
                    self.remove("bob", "object")
                else:
                    print "2: FALSE"

    def check(self):
        try:
            return self.__db.checkConnection(self.__dbName)
        except JavaException, e:
            msg = self.parseError(e)
            if msg == "Database does not exist":
                # Expected failure
                return None;
            else:
                # Something is wrong
                print "ERROR:", e
                self.__error = True
                self.__errorMsg = msg
                return None;

    def create(self):
        try:
            return self.__db.getConnection(self.__dbName)
        except JavaException, e:
            print "ERROR:", e
            self.__error = True
            self.__errorMsg = self.parseError(e)
            return None;

    def createTable(self):
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
            print "ERROR:", e
            self.__error = True
            self.__errorMsg = self.parseError(e)

    def getError(self):
        return self.__errorMsg

    def hasError(self):
        return self.__error

    def pageContent(self):
        if not page.authentication.is_admin():
            return "Sorry, you do not have sufficient security privileges to view this page.";
        else:
            if self.hasError():
                return "The database test failed, please refer to the log files for further details: ", self.getError()
            else:
                return "The database test has passed."

    def agree(self, username, objectId):
        self.resetErrors()
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
            msg = self.parseError(e)
            if msg == "Duplicate record!":
                # Expected failure
                print "Duplicate record already exists in table! USERNAME='%s' OID='%s'" % (username, objectId)
            else:
                # Something is wrong
                print "ERROR:", e
            self.__error = True
            self.__errorMsg = msg

    def remove(self, username, objectId):
        self.resetErrors()
        index = "userAgreements-DELETE"
        table = "agreements"
        fields = {
            "objectId": objectId,
            "username": username
        }
        try:
            self.__db.delete(self.__dbName, index, table, fields)
            print "Delete successful! USERNAME='%s' OID='%s'" % (username, objectId)
        except JavaException, e:
            # Something is wrong
            print "Delete failed! USERNAME='%s' OID='%s'" % (username, objectId)
            print "ERROR:", e
            self.__error = True
            self.__errorMsg = self.parseError(e)

    def verify(self, username, objectId):
        self.resetErrors()
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
            print "ERROR:", e
            self.__error = True
            self.__errorMsg = self.parseError(e)
            return False

    # Strip out java package names from error strings.
    def parseError(self, error):
        self.has_error = True
        message = error.getMessage()
        i = message.find(":")
        if i != -1:
            return message[i+1:].strip()
        else:
            return message.strip()

    def resetErrors(self):
        self.__error = False
        self.__errorMsg = ""
