import sys
import cPickle
try:
    sys.path.append(parsePath)
    plugin_manifest = __import__(parseLib)
    json = __import__(jsonLib)
except Exception, e:
    print repr(e)

# We want the file path as a python string
pyString = unicode(filePath).encode("utf-8")

# Read the data into memory and unpickle
try:
    FILE = open(pyString, 'rb')
    iceData = cPickle.loads(FILE.read())
    FILE.close()

# Something went awry...
except Exception, e:
    FILE.close()
    iceData = None
    print repr(e)

# Valid ICE data
responseGuid = None
if iceData is not None:
    # We only want the top-level manifest
    if iceData.has_key("manifest") and iceData["manifest"] is not None:
        try:
            responseGuid = iceData["_guid"]
            jsonManifest = iceData["manifest"].asJSON()
            responseJson = json.write(jsonManifest).decode("utf-8")
        except Exception, e:
            print repr(e)
