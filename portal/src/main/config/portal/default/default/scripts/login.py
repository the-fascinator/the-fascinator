#This is the python script that backs the login page. For the actual login script see action/login.py
class LoginData:
    def __init__(self):
        pass

    def __activate__(self, context):
        request = context["request"]
        if context["page"].authentication.is_logged_in():
            if request.getParameter("fromUrl") is not None:
                context["response"].sendRedirect(request.getParameter("fromUrl"))
            else:
                context["response"].sendRedirect("home")
        pass