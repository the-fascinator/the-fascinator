class UploadData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context
        self.writer = self.vc["response"].getPrintWriter("text/html; charset=UTF-8")

        listener = self.vc["sessionState"].get("upload_listener");

        if listener is not None:
            bytesRead = listener.getBytesRead();
            contentLength = listener.getContentLength();
            if bytesRead == contentLength:
                responseMsg = 100;
            else:
                responseMsg = round(100 * (bytesRead / contentLength));
            self.writer.println(responseMsg)
            self.writer.close()
        else:
            self.throw_error("No upload in progress")

    def throw_error(self, message):
        self.vc["response"].setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()
