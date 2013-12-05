package com.googlecode.fascinator.portal.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.googlecode.fascinator.portal.services.LanguageService;
import com.googlecode.fascinator.portal.services.LanguageServiceMXBean;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public class LanguageServiceMXBeanImpl implements LanguageServiceMXBean {

	@Autowired
	 @Qualifier(value = "scriptingServices")
	private ScriptingServices springScriptingService;
	private ScriptingServices tapestryScriptingService;
	
	
	@Override
	public void reloadLanguageFiles() {
		((LanguageService)this.springScriptingService.getService("languageService")).reloadLanguageFiles();
		((LanguageService)this.tapestryScriptingService.getService("languageService")).reloadLanguageFiles();
	}


	public void setTapestryScriptingService(
			ScriptingServices scriptingServicesImpl) {
		this.tapestryScriptingService = scriptingServicesImpl;
		
	}

	

}
