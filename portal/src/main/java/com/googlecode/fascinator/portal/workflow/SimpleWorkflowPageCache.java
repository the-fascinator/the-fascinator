package com.googlecode.fascinator.portal.workflow;

import java.lang.management.ManagementFactory;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.velocity.Template;

public class SimpleWorkflowPageCache extends ConcurrentHashMap<SimpleWorkflowPageCacheKey, Template> implements SimpleWorkflowPageCacheMXBean {

	private static final long serialVersionUID = 1L;
	
	public SimpleWorkflowPageCache() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		super();
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName mxbeanName = new ObjectName("com.googlecode.fascinator.portal:type=SimpleWorkflowPageCache");
        mbs.registerMBean(this, mxbeanName);
	}

	@Override
	public void clearCache() {
		this.clear();
	}

	@Override
	public void clearCache(String workflowId, String step) {
		this.remove(new AbstractMap.SimpleEntry<String, String>(workflowId,step));		
	}

	

}
