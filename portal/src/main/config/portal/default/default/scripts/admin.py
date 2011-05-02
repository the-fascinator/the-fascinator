from au.edu.usq.fascinator.common import JsonSimple

class AdminData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.json = JsonSimple()

    def parse_json(self, json_string):
        self.json = JsonSimple(json_string)
