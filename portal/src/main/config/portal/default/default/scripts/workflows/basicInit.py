class BasicInitData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

    def getFormData(self, field):
        value = self.velocityContext["formData"].get(field)
        if value is None:
            return ""
        else:
            return value
