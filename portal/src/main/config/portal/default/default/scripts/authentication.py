from com.googlecode.fascinator.api.access import AccessControlException;
from com.googlecode.fascinator.api.authentication import AuthenticationException;
from com.googlecode.fascinator.api.roles import RolesException;

class AuthenticationData:
    active_access_plugin = None
    active_auth_plugin = None
    active_role_plugin = None
    current_user = None
    has_error = False
    error_message = None
    GUEST_ROLE = 'guest'
    GUEST_USER = 'guest'
    roleList = None

    def __activate__(self, context):
        self.security = context["security"]
        self.formData = context["formData"]
        self.sessionState = context["sessionState"]

        self.access = self.security.getAccessControlManager()
        self.auth = self.security.getAuthManager()
        self.role = self.security.getRoleManager()

        self.check_login()

    def change_password(self, username, password):
        try:
            self.auth.changePassword(username, password)
            self.has_error = False
            return "Password changed."
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def check_login(self):
        action = self.formData.get("verb")

        # User is logging in
        if (action == "login"):
            username = self.formData.get("username")
            if username is not None:
                password = self.formData.get("password")
                self.login(username, password)

        # Normal page render, or logout
        else:
            username = self.sessionState.get("username")
            source   = self.sessionState.get("source")
            if username is not None:
                self.current_user = self.get_user(username, source)

        # User is logging out, make sure we ran get_user() first
        if (action == "logout"):
            self.logout()

    def create_role(self, rolename):
        try:
            self.role.createRole(rolename)
            self.has_error = False
            return username # TODO Where does this come from?
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def create_user(self, username, password):
        try:
            self.auth.createUser(username, password)
            self.has_error = False
            return username
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def delete_role(self, rolename):
        try:
            self.role.deleteRole(rolename)
            self.has_error = False
            return rolename
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def delete_user(self, username):
        try:
            self.auth.deleteUser(username)
            self.has_error = False
            return username
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def get_error(self):
        if self.has_error:
            return self.error_message
        else:
            return None

    def get_name(self):
        if self.current_user is not None:
            return self.current_user.realName()
        else:
            return "Guest";

    def get_plugins_access(self):
        return self.access.getPluginList()

    def get_plugins_auth(self):
        return self.auth.getPluginList()

    def get_plugins_roles(self):
        return self.role.getPluginList()

    def get_roles(self):
        my_roles = self.get_roles_list()
        length = len(my_roles)
        if length == 0:
            return ""
        elif length > 0:
            response = my_roles[0]

        if length > 1:
            for role in my_roles[1:]:
                response = response + ", " + role

        return response

    def get_roles_list(self):
        # Use the cache if we can
        if self.roleList is None:
            role_list = []
            try:
                if self.current_user is not None:
                    # Otherwise retrieve from the plugin
                    java_list = self.security.getRolesList(self.sessionState, self.current_user)
                    if java_list is not None:
                        for role in java_list:
                            role_list.append(role)
            except Exception, e:
                self.error_message = self.parse_error(e)

            role_list.append(self.GUEST_ROLE)
            self.roleList = role_list

        return self.roleList

    def get_access_roles_list(self, recordId):
        try:
            roles_list = {}
            plugins = self.access.getPluginList()
            for plugin in plugins:
                self.access.setActivePlugin(plugin.getId())
                result = self.access.getSchemas(recordId)
                roles = []
                for role in result:
                    roles.append(role.get("role"))
                roles_list[plugin.getId()] = roles
            return roles_list
        except AccessControlException, e:
            self.error_message = self.parse_error(e)

    def get_user(self, username, source):
        try:
            self.active_auth_plugin = source
            user = self.security.getUser(self.sessionState, username, source)
            self.has_error = False
            return user
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def get_username(self):
        if self.current_user is None:
            return self.GUEST_USER

        user = self.current_user.getUsername()
        if user is None:
            return self.GUEST_USER
        else:
            return user

    def get_user_source(self):
        if self.current_user is None:
            return self.GUEST_ROLE

        source = self.current_user.getSource()
        if source is None:
            return self.GUEST_ROLE
        else:
            return source

    def grant_access(self, recordId, newRole):
        try:
            newAccess = self.access.getEmptySchema()
            newAccess.init(recordId)
            newAccess.set("role", newRole)
            self.access.applySchema(newAccess)
            self.has_error = False
        except AccessControlException, e:
            self.error_message = self.parse_error(e)

    def grant_user_access(self, recordId, newUser):
        try:
            newAccess = self.access.getEmptySchema()
            newAccess.init(recordId)
            newAccess.set("user", newUser)
            self.access.applySchema(newAccess)
            self.has_error = False
        except AccessControlException, e:
            self.error_message = self.parse_error(e)


    def has_role(self, role):
        if self.current_user is not None:
            my_roles = self.get_roles_list()
            if role in my_roles:
                return True
            else:
                return False
        else:
            return False

    def is_admin(self):
        return self.has_role("admin")

    def is_logged_in(self):
        if self.current_user is not None:
            return True
        else:
            return False

    def list_users(self, rolename):
        try:
            return self.role.getUsersInRole(rolename)
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def login(self, username, password):
        try:
            self.current_user = self.auth.logIn(username, password)
            self.error_message = None
            self.sessionState.set("username", username)
            self.sessionState.set("source",   self.current_user.getSource())
            self.has_error = False
        except AuthenticationException, e:
            self.current_user  = None
            self.error_message = self.parse_error(e)

    def logout(self):
        if self.current_user is not None:
            try:
                self.security.logout(self.sessionState, self.current_user)
                self.current_user = None
                self.error_message = None
                self.has_error = False
            except AuthenticationException, e:
                self.error_message = self.parse_error(e)

    # Strip out java package names from
    # error strings.
    def parse_error(self, error):
        self.has_error = True
        message = error.getMessage()
        i = message.find(":")
        if i != -1:
            return message[i+1:].strip()
        else:
            return message.strip()

    def remove_role(self, username, rolename):
        try:
            self.role.removeRole(username, rolename)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def revoke_access(self, recordId, newRole):
        try:
            oldAccess = self.access.getEmptySchema()
            oldAccess.init(recordId)
            oldAccess.set("role", newRole)
            self.access.removeSchema(oldAccess)
            self.has_error = False
        except AccessControlException, e:
            self.error_message = self.parse_error(e)

    def search_roles(self, query, source):
        try:
            self.active_role_plugin = source
            self.role.setActivePlugin(source)
            roles = self.role.searchRoles(query)
            self.has_error = False
            return roles
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def search_users(self, query, source):
        try:
            self.active_auth_plugin = source
            self.auth.setActivePlugin(source)
            users = self.auth.searchUsers(query)
            self.has_error = False
            return users
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def set_access_plugin(self, plugin_id):
        try:
            self.active_access_plugin = plugin_id
            self.access.setActivePlugin(plugin_id)
            self.has_error = False
        except AccessControlException, e:
            self.error_message = self.parse_error(e)

    def set_auth_plugin(self, plugin_id):
        try:
            self.active_auth_plugin = plugin_id
            self.auth.setActivePlugin(plugin_id)
            self.has_error = False
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def set_role(self, username, rolename):
        try:
            self.role.setRole(username, rolename)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def set_role_plugin(self, plugin_id):
        try:
            self.active_role_plugin = plugin_id
            self.role.setActivePlugin(plugin_id)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)
