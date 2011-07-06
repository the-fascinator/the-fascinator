class LoginData:
    def __init__(self):
        pass

    def __activate__(self, context):
        # Context data
        self.velocityContext = context
        auth = self.velocityContext["page"].authentication
        resp = self.velocityContext["response"]

        # Do login
        if auth.is_logged_in():
            if auth.is_admin():
                responseMsg = auth.get_name() + ":admin"
            else:
                responseMsg = auth.get_name() + ":notadmin"
        else:
            responseMsg = auth.get_error()
            resp.setStatus(500)
        writer = resp.getPrintWriter("text/html; charset=UTF-8")
        writer.println(responseMsg)
        writer.close()
