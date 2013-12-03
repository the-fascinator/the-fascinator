package com.googlecode.fascinator.portal.workflow;

public interface SimpleWorkflowPageCacheMXBean {

	/**
	 * Clear the entire cache
	 */
	public void clearCache();
	
	public void clearCache(String workflowId, String step);
}
