from com.googlecode.fascinator.api.indexer import SearchRequest
from com.googlecode.fascinator.common.solr import SolrResult

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.lang import Exception

class DeletebyqueryData:
    def __init__(self):
        self.rowsPerQuery = 10
        self.testing = True
        self.deleteQueries = {
            # Cosmetic          # Solr query
            "National Library": "repository_name:\"National Library of Australia\"",
            "Newcastle":        "repository_name:\"The University of Newcastle, Australia\""
        }

    def __activate__(self, context):
        self.config = context["systemConfig"]
        self.log = context["log"]
        self.page = context["page"]
        self.portal = None
        self.portalId = context["portalId"]
        self.response = context["response"]
        self.services = context["Services"]
        self.writer = self.response.getPrintWriter("text/html; charset=UTF-8")

        # Admins Only
        if not self.__isAdmin():
            self.__writeResponse(403, "ERROR: Only administrative users can access this feature")
            return

        success = self.__loopExecute()

        # Errors
        if not success:
            self.__writeResponse(500, "ERROR: Errors occurred during execution! Please see the system logs")
            return

        # Testing or not
        if self.testing:
            self.__writeResponse(200, "TEST COMPLETE: Test execution completed without error. Check system logs for more details, and edit script for real execution if ready.")
        else:
            self.__writeResponse(200, "SUCCESS! Script completed without error. Always check system logs for more details... mass deletion is non-trivial.")

    def __batchExecute(self, search):
        result = self.__searchExecute(search, 0)

        # Stop on any errors
        if result is None:
            return False

        # Loop through each response
        overallSuccess = True
        for document in result.getResults():
            success = self.__processDocument(document)
            if not success:
                overallSuccess = False

        # Solr commit if not testing
        if not self.testing:
            try:
                self.services.indexer.commit()
            except Exception, e:
                self.log.error("Error during Solr commit: ", e)
                overallSuccess = False

        # Report in log
        count = result.getRows()
        total = result.getNumFound()
        remaining = (total - count)
        if self.testing:
            self.log.debug(" * Found {} records.", total)
        else:
            self.log.debug(" * Deleted first {} record, {} remain.", count, remaining)

        # Should we continue?
        if not self.testing and overallSuccess and remaining > 0:
            # Remember, we've deleted and committed, so this just
            # removes the next batch from 0 again.... no need to
            # update 'row' and 'start' etc.
            success = self.__batchExecute(search)
            if not success:
                overallSuccess = False

        return overallSuccess

    def __buildSearch(self, query):
        req = SearchRequest(query)
        req.setParam("rows", str(self.rowsPerQuery))
        req.setParam("fl", "*")
        req.setParam("fq", 'item_type:"object"')
        # The portal filter query
        portal = self.__getPortal()
        if portal.query != "":
            req.setParam("fq", portal.query)

        return req

    def __getPortal(self):
        if self.portal is None:
            portalManager = self.services.portalManager
            return portalManager.get(self.portalId)
        else:
            return self.portal

    def __getQuery(self, newQuery):
        portal = self.__getPortal()

        # Default
        if newQuery is None:
            query = "*:*"
        else:
            query = newQuery

        # Does the portal add extra search criteria?
        if portal.searchQuery != "":
            if query == "*:*":
                query = portal.searchQuery
            else:
                query = query + " AND " + portal.searchQuery
        return query

    def __isAdmin(self):
        # Logged in at all?
        isLoggedIn = self.page.authentication.is_logged_in()
        if not isLoggedIn:
            return False
        # Check if admin
        return self.page.authentication.is_admin()

    def __loopExecute(self):
        overallSuccess = True
        # Foreach query
        for key in self.deleteQueries.keys():
            # Get our search terms
            queryString = self.deleteQueries[key]
            self.log.debug("Query for '{}' => ({})...", key, queryString)
            # Build a new query
            query = self.__getQuery(queryString)
            search = self.__buildSearch(query)
            # Run it
            success = self.__batchExecute(search)
            if not success:
                overallSuccess = False
        return overallSuccess

    def __processDocument(self, doc):
        # Testing only
        if self.testing:
            #self.log.debug("Test...  ID: '{}'", doc.getFirst("storage_id"))
            #self.log.debug("      Title: '{}'", doc.getFirst("dc_title"))
            #self.log.debug("     Origin: '{}'", doc.getFirst("repository_name"))
            return True

        # The real deal
        else:
            oid = doc.getFirst("storage_id")
            self.log.debug("ID: '{}'", oid)
            index = self.services.getIndexer()
            storage = self.services.getStorage()

            errors = False
            # Delete from storage
            try:
                storage.removeObject(oid)
            except Exception, e:
                self.log.error("Error deleting object from storage '{}': ", oid, e)
                errors = True

            # Delete from Solr
            try:
                index.remove(oid)
            except Exception, e:
                self.log.error("Error deleting Solr entry '{}': ", oid, e)
                errors = True

            # Delete annotations
            try:
                index.annotateRemove(oid)
            except Exception, e:
                self.log.error("Error deleting annotations '{}': ", oid, e)
                errors = True

            if errors:
                return False
            return True

    def __searchExecute(self, search, count):
        try:
            search.setParam("start", str(count))
            out = ByteArrayOutputStream()
            self.services.indexer.search(search, out)
            return SolrResult(ByteArrayInputStream(out.toByteArray()))
        except Exception, e:
            self.log.error("Error during search: ", e)
            return None

    def __writeResponse(self, status, message):
        self.response.setStatus(status)
        self.writer.println(message)
        self.writer.close()
