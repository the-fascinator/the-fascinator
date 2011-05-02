import md5

class TestData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context

        self.writer = self.vc["response"].getPrintWriter("text/html; charset=UTF-8")

        # Did the request have a token?
        token = self.vc["formData"].get("token")
        if token is None:
            self.throwDenial("Access denied!")
        else:
            # Security token check
            key = "JobSecurityToken12345"
            validToken = md5.new(key).hexdigest()
            if validToken != token:
                self.throwDenial("Invalid security token provided!")
            else:
                self.process()

    def process(self):
        # Message is not important
        self.writer.println("Job successfully initiatied")
        self.writer.close()

    def throwDenial(self, message):
        self.vc["response"].setStatus(403)
        self.writer.println(message)
        self.writer.close()

    def throwError(self, message):
        self.vc["response"].setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()
