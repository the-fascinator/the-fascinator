
class StateData:
    def __activate__(self, context):
        self.velocityContext = context

        #print "formData=%s" % self.vc("formData")
        result = "{}"
        func = self.vc("formData").get("func")
        name = self.vc("formData").get("name")

        auth = context["page"].authentication
        if auth.is_admin():
            if func == "set":
                    value = self.vc("formData").get("value")
                    self.vc("sessionState").set(name, value)
                    result = '{ "name": "%s", "value": "%s" }' % (name, value)
            elif func == "get":
                value = self.vc("sessionState").get(name)
                result = '{ "value": "%s" }' % value
            elif func == "remove":
                value = self.vc("sessionState").get(name)
                self.vc("sessionState").remove(name)
                result = '{ "value": "%s" }' % value
        else:
            result = '{ "error": "Only administrative users have access to this API" }'
        
        writer = self.vc("response").getPrintWriter("application/json; charset=UTF-8")
        writer.println(result)
        writer.close()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None
