class UploadData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context

        self.roles = self.vc["page"].authentication.get_roles_list()
        self.uploader = self.vc["toolkit"].getFileUploader(self.roles)

    def render_upload_form(self):
        return self.uploader.renderForm()
