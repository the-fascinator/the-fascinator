import au.edu.usq.fascinator.common.BasicHttpClient as BasicHttpClient
import org.apache.commons.httpclient.methods.GetMethod as GetMethod

from au.edu.usq.fascinator.common import JsonSimple

class GeonamesData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context

        responseType = "text/html; charset=UTF-8"
        responseMsg = ""
        func = self.vc("formData").get("func")
        if func == "placeName":
            try:
                placeName = self.vc("formData").get("q")
                url = "http://ws.geonames.org/searchJSON?fuzzy=0.7&q=" + placeName
                client = BasicHttpClient(url)
                get = GetMethod(url)
                statusInt = client.executeMethod(get)
                r = str(statusInt)
                json = JsonSimple(get.getResponseBodyAsString().strip())
                for geoName in json.getJsonSimpleList("geonames"):
                    responseMsg += "%s, %s|%s \n" % (geoName.getString(None, "name"), geoName.getString(None, "countryName"), geoName.getString(None, "geonameId"))
            except Exception, e:
                print "exception: ", str(e)
                r = str(e), None
            responseType = "text/plain; charset=UTF-8"
            #responseMsg = "\nToowoomba, Australia\nToowoomba, Africa";
        writer = self.vc("response").getPrintWriter(responseType)
        writer.println(responseMsg)
        writer.close()

#"countryName" : "Australia",
#  "adminCode1" : "04",
#  "fclName" : "city, village,...",
#  "countryCode" : "AU",
#  "lng" : 151.9666667,
#  "fcodeName" : "populated place",
#  "fcl" : "P",
#  "name" : "Toowoomba",
#  "fcode" : "PPL",
#  "geonameId" : 2146268,
#  "lat" : -27.55,
#  "population" : 92800,
#  "adminName1" : "Queensland"
