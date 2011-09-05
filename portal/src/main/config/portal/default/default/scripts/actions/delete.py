from java.lang import Exception

class DeleteData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context

        self.writer = self.vc["response"].getPrintWriter("text/html; charset=UTF-8")

        if self.vc["page"].authentication.is_logged_in() or self.vc["page"].authentication.is_admin():
            self.process()
        else:
            self.throw_error("Only administrative users can access this feature")

    def process(self):
        record = self.vc["formData"].get("record")
        if record is None:
            self.throw_error("Record ID required")

        errors = False

        # Delete from storage
        try:
            Services.storage.removeObject(record)
        except Exception, e:
            self.vc["log"].error("Error deleting object from storage: ", e)
            errors = True

        # Delete from Solr
        try:
            Services.indexer.remove(record)
        except Exception, e:
            self.vc["log"].error("Error deleting Solr entry: ", e)
            errors = True

        # Delete annotations
        try:
            Services.indexer.annotateRemove(record)
        except Exception, e:
            self.vc["log"].error("Error deleting annotations: ", e)
            errors = True

        if errors:
            self.throw_error("Error deleting object!")
        else:
            self.writer.println(record)
            self.writer.close()

    def throw_error(self, message):
        self.vc["response"].setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()
