from au.edu.usq.fascinator.common import JsonSimple

class AuthtestData:

    def __init__(self):
        pass

    def __activate__(self, context):
        request = context["request"]
        response = context["response"]
        writer = response.getPrintWriter("text/javascript; charset=UTF-8")
        result = JsonSimple()

        ## Look for the JSONP callback to use
        jsonpCallback = request.getParameter("callback")
        if jsonpCallback is None:
            jsonpCallback = request.getParameter("jsonp_callback")
            if jsonpCallback is None:
                response.setStatus(403)
                writer.println("Error: This interface only responds to JSONP")
                writer.close()
                return

        if context["page"].authentication.is_logged_in():
            result.getJsonObject().put("isAuthenticated", "true")
        else:
            result.getJsonObject().put("isAuthenticated", "false")

        writer.println(jsonpCallback + "(" + result.toString() + ")")
        writer.close()
