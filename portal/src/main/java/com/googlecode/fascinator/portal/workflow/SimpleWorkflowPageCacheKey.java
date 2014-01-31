package com.googlecode.fascinator.portal.workflow;

public class SimpleWorkflowPageCacheKey implements
		Comparable<SimpleWorkflowPageCacheKey> {

	private String portalId;
	private String workflowId;
	private String workflowStep;

	public SimpleWorkflowPageCacheKey(String portalId, String workflowId,
			String workflowStep) {
		this.portalId = portalId;
		this.workflowId = workflowId;
		this.workflowStep = workflowStep;
	}

	@Override
	public int compareTo(SimpleWorkflowPageCacheKey key) {
		if (this == key) {
			return 0;
		}
		int compareValue = key.getPortalId().compareTo(portalId);
		if (compareValue != 0) {
			return compareValue;
		}
		compareValue = key.getWorkflowId().compareTo(workflowId);
		if (compareValue != 0) {
			return compareValue;
		}
		compareValue = key.getWorkflowStep().compareTo(workflowStep);
		if (compareValue != 0) {
			return compareValue;
		}
		return compareValue;
	}

	@Override
	public int hashCode() {
		return portalId.hashCode() + 23 * workflowId.hashCode() + 31
				* workflowStep.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SimpleWorkflowPageCacheKey key = (SimpleWorkflowPageCacheKey)obj;
		return this.portalId.equals(key.getPortalId()) && this.workflowId.equals(key.getWorkflowId()) && this.workflowStep.equals(key.getWorkflowStep());
	}

	private String getPortalId() {
		return portalId;
	}

	private String getWorkflowId() {
		return workflowId;
	}

	private String getWorkflowStep() {
		return workflowStep;
	}

}
