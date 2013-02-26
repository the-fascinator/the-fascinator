import sys
from com.googlecode.fascinator.api.storage import StorageException

class AdminData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.writer = self.vc("response").getPrintWriter("text/html; charset=UTF-8")
        self.log = context["log"]

        if self.vc("page").authentication.is_logged_in() and self.vc("page").authentication.is_admin():
            self.process()
        else:
            self.throw_error("Only administrative users can access this feature")

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def add_user(self):
        username = self.vc("formData").get("field")
        rolename = self.vc("formData").get("hidden")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_role_plugin(source)
        self.vc("page").authentication.set_role(username, rolename)

        err = self.vc("page").authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def change_password(self):
        username = self.vc("formData").get("username")
        password = self.vc("formData").get("password")
        password_confirm = self.vc("formData").get("password_confirm")

        if password != password_confirm:
            self.throw_error("The confirm password field does not match the password.")

        else:
            source = self.vc("formData").get("source")
            self.vc("page").authentication.set_auth_plugin(source)
            self.vc("page").authentication.change_password(username, password)

            err = self.vc("page").authentication.get_error()
            if err is None:
                self.writer.println(username)
                self.writer.close()

            else:
                self.throw_error(err)

    def confirm_message(self):
        msgId = self.vc("formData").get("message")
        hk = Services.getHouseKeepingManager()

        if msgId is None:
            self.throw_error("No message ID provided")

        try:
            if msgId == "ALL":
                list = hk.getUserMessages();
                for entry in list:
                    if not entry.block:
                        hk.confirmMessage(str(entry.id));
            else:
                hk.confirmMessage(msgId);
        except:
            error = sys.exc_info()[1]
            self.throw_error(error.getMessage())

        self.writer.println("ok")
        self.writer.close()

    def create_role(self):
        rolename = self.vc("formData").get("field")
        source   = self.vc("formData").get("source")
        self.vc("page").authentication.set_role_plugin(source)
        self.vc("page").authentication.create_role(rolename)

        err = self.vc("page").authentication.get_error()
        if err is None:
            self.writer.println(rolename)
            self.writer.close()

        else:
            self.throw_error(err)

    def create_user(self):
        username = self.vc("formData").get("username")
        password = self.vc("formData").get("password")
        password_confirm = self.vc("formData").get("password_confirm")

        if password != password_confirm:
            self.throw_error("The confirm password field does not match the password.")

        else:
            source = self.vc("formData").get("source")
            self.vc("page").authentication.set_auth_plugin(source)
            self.vc("page").authentication.create_user(username, password)

            err = self.vc("page").authentication.get_error()
            if err is None:
                self.writer.println(username)
                self.writer.close()

            else:
                self.throw_error(err)

    def delete_role(self):
        rolename = self.vc("formData").get("rolename")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_role_plugin(source)
        self.vc("page").authentication.delete_role(rolename)

        err = self.vc("page").authentication.get_error()
        if err is None:
            self.writer.println(rolename)
            self.writer.close()

        else:
            self.throw_error(err)

    def delete_user(self):
        username = self.vc("formData").get("username")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_auth_plugin(source)
        self.vc("page").authentication.delete_user(username)

        err = self.vc("page").authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def get_current_access(self):
        record = self.vc("formData").get("record")

        # Is this object in a workflow?
        hasWorkflow = False
        storage = self.vc("Services").getStorage()
        object = None
        try:
            object = storage.getObject(record)
            wfPayload = object.getPayload("workflow.metadata")
            if wfPayload is not None:
                hasWorkflow = True;
        except StorageException, e:
            # Flag is already set to false
            pass

        # Do we already pay attention to this?
        wfOverride = False
        if hasWorkflow:
            try:
                metadata = object.getMetadata()
                flag = metadata.getProperty("overrideWfSecurity")
                if flag is not None and flag == "true":
                    wfOverride = True
            except StorageException, e:
                # Flag is already set to false
                pass

        # Ready JSON section for workflows
        if hasWorkflow:
            if wfOverride:
                wfString = "'workflow': {'hasWf': true, 'useWf': false}"
            else:
                wfString = "'workflow': {'hasWf': true, 'useWf': true}"
        else:
            wfString = "'workflow': {'hasWf': false, 'useWf': true}"

        roles_list = self.vc("page").authentication.get_access_roles_list(record)

        err = self.vc("page").authentication.get_error()
        if err is None:
            # We need a JSON string for javascript
            plugin_strings = []
            for plugin in roles_list.keys():
                roles = roles_list[plugin]
                if len(roles) > 0:
                    plugin_strings.append("'" + plugin + "' : ['" + "','".join(roles) + "']")
                else:
                    plugin_strings.append("'" + plugin + "' : []")
            pluginString = "'plugins': {" + ",".join(plugin_strings) + "}"
            responseMessage = "{" + wfString + ", " + pluginString + "}"
            self.writer.println(responseMessage)
            self.writer.close()

        else:
            self.throw_error(err)

    def grant_access(self):
        record = self.vc("formData").get("record")
        role   = self.vc("formData").get("role")
        user   = self.vc("formData").get("user")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_access_plugin(source)
        
        if role is not None:
            self.vc("page").authentication.grant_access(record, role)
        
        if user is not None:
            self.vc("page").authentication.grant_user_access(record, user)

        err = self.vc("page").authentication.get_error()
        if err is None:
            if role is not None:
                self.writer.println(role)
            if user is not None:
                self.writer.println(user)
            self.writer.close()
            self.reindex_record(record)

        else:
            self.throw_error(err)

    def list_users(self):
        rolename = self.vc("formData").get("rolename")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_auth_plugin(source)
        user_list = self.vc("page").authentication.list_users(rolename)

        err = self.vc("page").authentication.get_error()
        if err is None:
            # We need a JSON string for javascript
            responseMessage = "{['" + "','".join(user_list) + "']}"
            self.writer.println(responseMessage)
            self.writer.close()

        else:
            self.throw_error(err)

    def process(self):
        valid = self.vc("page").csrfSecurePage()
        if not valid:
            self.throw_error("Invalid request")
            return

        action = self.vc("formData").get("verb")

        switch = {
            "add-user"           : self.add_user,
            "confirm-message"    : self.confirm_message,
            "create-role"        : self.create_role,
            "create-user"        : self.create_user,
            "delete-role"        : self.delete_role,
            "delete-user"        : self.delete_user,
            "change-password"    : self.change_password,
            "get-current-access" : self.get_current_access,
            "grant-access"       : self.grant_access,
            "list-users"         : self.list_users,
            "remove-user"        : self.remove_user,
            "revoke-access"      : self.revoke_access,
            "workflow-override"  : self.workflow_override
        }
        switch.get(action, self.unknown_action)()

    def reindex_record(self, recordId):
        # Re-index the object
        Services.indexer.index(recordId)
        Services.indexer.commit()

    def remove_user(self):
        username = self.vc("formData").get("username")
        rolename = self.vc("formData").get("rolename")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_role_plugin(source)
        self.vc("page").authentication.remove_role(username, rolename)

        err = self.vc("page").authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def revoke_access(self):
        record = self.vc("formData").get("record")
        role   = self.vc("formData").get("role")
        user   = self.vc("formData").get("user")
        source = self.vc("formData").get("source")
        self.vc("page").authentication.set_access_plugin(source)
        
        if role is not None:
            self.vc("page").authentication.revoke_access(record, role)
        
        if user is not None:
            self.vc("page").authentication.revoke_user_access(record, user)

        err = self.vc("page").authentication.get_error()
        if err is None:
            if role is not None:
                self.writer.println(role)
            if user is not None:
                self.writer.println(user)
            self.writer.close()
            self.reindex_record(record)
        else:
            self.throw_error(err)

    def throw_error(self, message):
        self.vc("response").setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()

    def unknown_action(self):
        self.throw_error("Unknown action requested - '" + self.vc("formData").get("verb") + "'")

    def workflow_override(self):
        record = self.vc("formData").get("record")
        newFlag = self.vc("formData").get("wfOverride")

        # Is this object in a workflow?
        hasWorkflow = False
        storage = self.vc("Services").getStorage()
        object = None
        try:
            object = storage.getObject(record)
            wfPayload = object.getPayload("workflow.metadata")
            if wfPayload is not None:
                hasWorkflow = True;
        except StorageException, e:
            self.throw_error("Error accessing object in storage")

        # Do we already pay attention to this?
        wfOverride = False
        if hasWorkflow:
            try:
                metadata = object.getMetadata()
                metadata.setProperty("overrideWfSecurity", newFlag)
                object.close()
                self.writer.println(newFlag)
                self.writer.close()
            except StorageException, e:
                self.throw_error("Error accessing object metadata")
        else:
            self.throw_error("This object is not in a workflow")
