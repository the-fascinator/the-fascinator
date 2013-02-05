package com.googlecode.fascinator.portal.api;

import org.apache.tapestry5.services.Request;

import com.googlecode.fascinator.portal.services.ScriptingServices;

public interface APICallHandler {

    public void setScriptingServices(ScriptingServices scriptingServices);

    public String handleRequest(Request request) throws Exception;

}
