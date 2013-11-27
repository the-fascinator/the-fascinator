package com.googlecode.fascinator.access.hibernate;

import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.common.access.GenericSchema;

/**
 * Hibernate based schema
 * 
 * @author Shilo Banihit
 *
 */
public class HibernateAccessSchema extends GenericSchema {

	/** Role name */
    public String role;
    
    /** User name */
    public String user;

    /**
     * Initialization method
     *
     * @param recordId record id
     */
    public void init(String recordId) {
        setRecordId(recordId);
    }
    
    public boolean equals(Object obj) {
    	if (this == obj) return true;
    	boolean sameRec = false;
    	if (obj instanceof AccessControlSchema && !(obj instanceof HibernateAccessSchema)) {
    		AccessControlSchema inst = (AccessControlSchema) obj;
    		sameRec = (inst.getRecordId() == null && getRecordId() == null) ||  (inst.getRecordId() != null && inst.getRecordId().equalsIgnoreCase(this.getRecordId())) ;
    		return sameRec;
    	} else {
	    	if (!(obj instanceof HibernateAccessSchema)) return false;
    	}
    	HibernateAccessSchema inst = (HibernateAccessSchema) obj;    	
    	return ( (inst.getRecordId() == null && getRecordId() == null) ||  (inst.getRecordId() != null && inst.getRecordId().equalsIgnoreCase(this.getRecordId()))
    			&& ((inst.role == null && role == null) ||  (inst.role != null && inst.role.equalsIgnoreCase(role)))
				&& ((inst.user == null && user == null) ||  (inst.user != null && inst.user.equalsIgnoreCase(user)))
    			);
    }
}
