/*
 * The Fascinator - USQ Single Sign-On
 * Copyright (C) 2010 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.sso;

import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.portal.JsonSessionState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Fascinator and Single Sign-On integration for USQ.
 * @author Greg Pendlebury
 *
 * Most of this work originally comes from other USQ authors:
 * @author Kyaw Htay Aung
 * @author Jonathon Fowler
 *
 */

public class USQSSO implements SSOInterface {
    /** Logging */
    private Logger log = LoggerFactory.getLogger(USQSSO.class);

    /** URL to redirect to for user logins */
    private String remoteLogonURL = null;

    /** ID for a logon attempt ?? */
    private String remoteLogonId;

    /** Request key */
    private String requestKey;

    /** Response key */
    private String responseKey;

    /** Web service WSDL */
    private String WS_DESTINATION =
            "https://usqauth.usq.edu.au/WSRemoteLogon.asmx?WSDL";

    /** Field name for soap action distinction */
    private String WS_SOAP_ACTION = "SOAPAction";

    /** URI to identify logon action */
    private String WS_LOGON_URI =
            "http://tempuri.org/USQ.eInterface2003/WSRemoteLogon/GetLogonURL";

    /** URI to identify user detail action */
    private String WS_USERDETAIL_URI =
            "http://tempuri.org/USQ.eInterface2003/WSRemoteLogon/GetUserDetailsFull";

    /** URI for XML fields */
    private String ENV_URI =
            "http://tempuri.org/USQ.eInterface2003/WSRemoteLogon";

    /** URI Namespace for XML fields */
    private String ENV_NS = "foo";

    /** Connection to SSO service */
    private SOAPConnection connection;

    /** SOAP message */
    private SOAPMessage message;

    /** Message envelope */
    private SOAPEnvelope envelope;

    /** Message body */
    private SOAPBody messageBody;

    /** Message reply */
    private SOAPMessage reply;

    /** HTML Link Text */
    private String linkText;

    /**
     * Return the USQSSO ID. Must match configuration at instantiation.
     *
     * @return String The SSO implementation ID.
     */
    @Override
    public String getId() {
        return "USQSSO";
    }

    /**
     * Return the on-screen label to describing this implementation.
     *
     * @return String The SSO implementation label.
     */
    @Override
    public String getLabel() {
        return "Login via UConnect";
    }

    /**
     * Return the HTML snippet to use in the interface.
     *
     * Implementations can append additional params to URLs.
     * Like so: "?ssoId=OpenID&{customString}"
     * eg: "?ssoId=OpenID&provider=Google"
     *
     * @param ssoUrl The basic ssoUrl for the server.
     * @return String The string to display as link text.
     */
    @Override
    public String getInterface(String ssoUrl) {
        String html = "<a href=\"" + ssoUrl + "\">" +
                "<img title=\"" + getLabel() +
                "\" alt=\"" + getLabel() + "\" src=\"" + linkText + "\"/>" +
                "</a>";
        return html;
    }

    /**
     * Get the current user details in a User object.
     *
     * @return User A user object containing the current user.
     */
    @Override
    public User getUserObject(JsonSessionState session) {
        String username = (String) session.get("usqSsoUsername");
        String fullname = (String) session.get("usqSsoFullName");
        String groups = (String) session.get("usqSsoGroups");

        if (username == null) {
            return null;
        }

        USQUser user = new USQUser();
        user.setUsername(username);
        user.setSource("USQSSO");
        user.set("fullName", fullname);
        user.set("groups", groups);
        return user;
    }

    /**
     * We cannot log the user out of UConnect, but we can clear Fascinator
     * session data regarding this user.
     *
     */
    @Override
    public void logout(JsonSessionState session) {
        session.remove("usqSsoFullName");
        session.remove("usqSsoRemoteLogonId");
        session.remove("usqSsoResponseKey");
        session.remove("usqSsoRequestKey");
        session.remove("usqSsoUsername");
        session.remove("usqSsoGroups");
        session.remove("usqSsoRemoteLogonURL");
    }

    /**
     * Initialize the SSO Service
     *
     * @param session The server session data
     * @param request The incoming servlet request
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoInit(JsonSessionState session, HttpServletRequest request)
            throws Exception {
        // Make sure our link is up-to-date
        String portalUrl = (String) session.get("ssoPortalUrl");
        linkText = portalUrl + "/images/UConnect.jpg";
    }

    /**
     * Prepare the SSO Service to receive a login from the user
     *
     * @param returnAddress The address to come back to after the login
     * @param server The server domain
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoPrepareLogin(JsonSessionState session, String returnAddress,
            String server) throws Exception {
        // Read configuration
        requestKey = this.getRequestKey(session);

        // Let the SSO Service know we are sending a login to them
        createMessage(WS_LOGON_URI);
        SOAPElement bodyElement = messageBody.addChildElement(
                envelope.createName("GetLogonURL" , ENV_NS, ENV_URI));
        bodyElement.addChildElement(envelope.createName("ReturnURL",
                ENV_NS, ENV_URI)).addTextNode(returnAddress);
        bodyElement.addChildElement(envelope.createName("RequestKey",
                ENV_NS, ENV_URI)).addTextNode(requestKey);
        sendMessage();

        // Check the output
        retrieveLogonResult(session, reply.getSOAPBody());
    }

    /**
     * Retrieve the login URL for redirection.
     *
     * @return String The URL used by the SSO Service for logins
     */
    @Override
    public String ssoGetRemoteLogonURL(JsonSessionState session) {
        return remoteLogonURL;
    }

    /**
     * Get user details from the SSO Service and set them in the user session.
     *
     */
    @Override
    public void ssoCheckUserDetails(JsonSessionState session) {
        // Check if already logged in
        String username = (String) session.get("usqSsoUsername");
        if (username != null) {
            return;
        }

        // SSO Service details
        remoteLogonId = (String) session.get("usqSsoRemoteLogonId");
        responseKey = (String) session.get("usqSsoResponseKey");
        requestKey = (String) session.get("usqSsoRequestKey");

        // Make sure we've connected to SSO before
        if (remoteLogonId == null || requestKey == null ||
                responseKey == null) {
            return;
        }

        try {
            // Ask the SSO Service about our user
            createMessage(WS_USERDETAIL_URI);
            SOAPElement bodyElement = messageBody.addChildElement(
                    envelope.createName("GetUserDetailsFull", ENV_NS, ENV_URI));
            bodyElement.addChildElement(envelope.createName("RemoteLogonID",
                    ENV_NS, ENV_URI)).addTextNode(remoteLogonId);
            bodyElement.addChildElement(envelope.createName("RequestKey",
                    ENV_NS, ENV_URI)).addTextNode(requestKey);
            bodyElement.addChildElement(envelope.createName("ResponseKey",
                    ENV_NS, ENV_URI)).addTextNode(responseKey);
            sendMessage();

            // Handle the output
            parseUserDetails(session, reply.getSOAPBody());
        } catch(Exception e) {
            log.error("Error retrieving user details from SSO Servivce", e);

            // Unset our SSO details. The user can try again later
            session.remove("usqSsoRemoteLogonId");
            session.remove("usqSsoResponseKey");
            session.remove("usqSsoRequestKey");
        }
    }

    /**
     * Parse and store the data returned from SSO Service in response to a
     * logon preparation request.
     *
     * @param body The body of the response message.
     */
    private void retrieveLogonResult(JsonSessionState session, Element body) {
        Element response = (Element) body.
                getElementsByTagName("GetLogonURLResponse").item(0);
        Element result = (Element) response.
                getElementsByTagName("GetLogonURLResult").item(0);

        // Read the data
        remoteLogonURL = readValue(result, "URL");
        remoteLogonId = remoteLogonURL.substring(
                remoteLogonURL.indexOf("USQRLID=") + 8);
        responseKey = readValue(result, "ResponseKey");

        // Set it into the session
        session.set("usqSsoRemoteLogonURL", remoteLogonURL);
        session.set("usqSsoRemoteLogonId", remoteLogonId);
        session.set("usqSsoResponseKey", responseKey);
    }

    /**
     * Parse and store the data returned from SSO Service in response to a
     * query of the logged in user's details.
     *
     * @param body The body of the response message.
     */
    private void parseUserDetails(JsonSessionState session, Element body) {
        Element response = (Element) body.
                getElementsByTagName("GetUserDetailsFullResponse").item(0);
        Element result = (Element) response.
                getElementsByTagName("GetUserDetailsFullResult").item(0);

        //debugResponse(result, "{root} > ");

        String username = readValue(result, "UserID");
        String fullname = readValue(result, "FullName");
        String groups = readValue(result, "Groups");
        session.set("usqSsoUsername", username);
        session.set("usqSsoFullName", fullname);
        session.set("usqSsoGroups", groups);
    }

    /**
     * The is method is purely for debugging. It will recursively walk the
     * XML nodes of the response and display node names and values
     * (including nulls).
     *
     * @param node The node to output.
     * @param prefix A string to display before output.
     */
    private void debugResponse(Node node, String prefix) {
        log.debug(prefix + "'{}' : '{}'",
                node.getNodeName(), node.getNodeValue());
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            int count = children.getLength();
            for (int i = 0; i < count; i++) {
                debugResponse(children.item(i),
                        prefix + "'" + node.getNodeName() + "' > ");
            }
        }
    }

    /**
     * Retrieve the value of the first found child of a node matching the
     * tag provided.
     *
     * @param root The top level node.
     * @param tag The node name to retrieve a value from.
     */
    private String readValue(Element root, String tag) {
        if (root == null) {
            return null;
        }

        Node n = root.getElementsByTagName(tag).item(0);
        if (n != null) {
            n = n.getFirstChild();
            if (n != null) {
                return n.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Create a SOAP connection and empty message to the SSO Service for the
     * requested action.
     *
     * @param soapAction The action we are requesting of the service.
     */
    private void createMessage(String soapAction) throws Exception {
        SOAPConnectionFactory soapFactory = SOAPConnectionFactory.newInstance();
        connection = soapFactory.createConnection();

        MessageFactory messageFactory = MessageFactory.newInstance();
        message = messageFactory.createMessage();
        message.getMimeHeaders().addHeader(WS_SOAP_ACTION, soapAction);
        envelope = message.getSOAPPart().getEnvelope();
        messageBody = envelope.getBody();
    }

    /**
     * Save and send the current message, store the reply and close
     * the SOAP connection to the SSO Service.
     *
     */
    private void sendMessage() throws Exception {
        // Save the message
        message.saveChanges();
        // Send the message
        reply = connection.call(message, WS_DESTINATION);
        // Close the connection
        connection.close();
    }

    /**
     * Generate and store a random request key if one does not already exist.
     *
     * @return String The random request key.
     */
    private String getRequestKey(JsonSessionState session) {
        String key = (String) session.get("usqSsoRequestKey");
        if (key == null) {
            key = Long.toHexString(Thread.currentThread().getId()) +
                    Long.toHexString((new Date()).getTime());
            session.set("usqSsoRequestKey", key);
        }
        return key;
    }

    /**
     * Get a list of roles possessed by the current user from the SSO Service.
     *
     * @return List<String> Array of roles.
     */
    @Override
    public List<String> getRolesList(JsonSessionState session) {
        // Retrieve the groups from the session and split
        String groups = (String) session.get("usqSsoGroups");
        String[] groupArr = groups.toUpperCase().split(",");

        // Clean the data
        List<String> cleaned = new ArrayList();
        for (String s : groupArr) {
            s = s.trim();
            if (s.length() > 0) {
                cleaned.add(s);
            }
        }

        return cleaned;
    }
}
