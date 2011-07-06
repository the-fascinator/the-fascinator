from com.googlecode.fascinator.api.storage import StorageException
from com.googlecode.fascinator.common import JsonObject

class ObjectMetaData:
    def __activate__(self, context):
        response = context["response"]
        json = JsonObject()
        auth = context["page"].authentication
        if auth.is_logged_in():
            formData = context["formData"]
            oid = formData.get("oid")
            if oid:
                # TODO check security on object
                json.put("oid", oid)
                try:
                    metaNode = JsonObject()
                    json.put("meta", metaNode)

                    object = context["Services"].storage.getObject(oid)
                    metadata = object.getMetadata()

                    for key in metadata.keySet():
                        metaNode.put(key, metadata.get(key))
                except StorageException:
                    response.setStatus(500)
                    json.put("error", "Object '%s' not found" % oid)
            else:
                response.setStatus(500)
                json.put("error", "An object identifier is required")
        else:
            response.setStatus(500)
            json.put("error", "Only registered users can access this API")
        
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(json.toString())
        writer.close()
    
