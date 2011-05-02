class BasicUploadData:
    def __activate__(self, context):
        self.formData = context["formData"]

    def getFormData(self, field):
        value = self.formData.get(field)
        if value is None:
            return ""
        else:
            return value
