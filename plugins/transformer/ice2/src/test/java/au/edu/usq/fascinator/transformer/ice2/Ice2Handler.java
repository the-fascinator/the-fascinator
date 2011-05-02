/*
 * The Fascinator - Plugin - Transformer - ICE 2
 * Copyright (C) 2009 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package au.edu.usq.fascinator.transformer.ice2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.testing.HttpTester;

/**
 * Simple ICE 2 web handler for unit testing purposes.
 * 
 * @author Linda Octalina
 * @author Oliver Lucido
 */
public class Ice2Handler extends AbstractHandler {

    private HttpTester httpTester;

    public Ice2Handler() {
        httpTester = new HttpTester();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException,
            ServletException {
        InputStream in = null;
        String pathInfo = request.getPathInfo();
        String contentType = "text/html";
        int statusCode = HttpServletResponse.SC_OK;
        if (pathInfo.endsWith("query")) {
            httpTester.parse(getRequest(request));
            String pathext = request.getParameter("pathext");
            String value = "Not supported";
            if ("odt".equals(pathext) || "doc".equals(pathext)) {
                value = "OK";
            }
            in = IOUtils.toInputStream(value);
        } else {
            boolean found = false;
            try {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List items = upload.parseRequest(request);
                Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    FileItem item = (FileItem) iter.next();
                    if (item != null && item.getName() != null &&
                            item.getName().contains("somefile")) {
                        found = true;
                        break;
                    }
                }
            } catch (FileUploadException fue) {
                found = true;
            }
            if (found) {
                statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                in = IOUtils.toInputStream("INTERNAL SERVER ERROR");
            } else {
                contentType = "application/zip";
                in = getClass().getResourceAsStream("/first-post.zip");
            }
        }
        response.setContentType(contentType);
        response.setStatus(statusCode);

        OutputStream out = response.getOutputStream();
        IOUtils.copy(in, out);
        out.close();
        in.close();

        ((Request) request).setHandled(true);
    }

    private String getRequest(HttpServletRequest request) throws IOException {
        ServletInputStream sis = request.getInputStream();
        int contentLen = request.getContentLength();
        if (contentLen == -1) {
            return request.toString();
        }
        byte[] buff = new byte[contentLen];
        sis.read(buff);
        return request.toString() + new String(buff);
    }
}
