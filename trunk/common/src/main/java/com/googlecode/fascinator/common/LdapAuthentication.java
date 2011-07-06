/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.common;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simple LDAP authentication
 * 
 * @author Oliver Lucido
 */
public class LdapAuthentication {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(LdapAuthentication.class);

    /** LDAP environment */
    private Hashtable<String, String> env;

    /** LDAP Base DN */
    private String baseDn;

    /** LDAP identifier attribute */
    private String idAttr;

    /**
     * Creates an LDAP authenticator for the specified server and base DN, using
     * the default identifier attribute "uid"
     * 
     * @param baseUrl LDAP server URL
     * @param baseDn LDAP base DN
     */
    public LdapAuthentication(String baseUrl, String baseDn) {
        this(baseUrl, baseDn, "uid");
    }

    /**
     * Creates an LDAP authenticator for the specified server, base DN and
     * identifier attribute
     * 
     * @param baseUrl LDAP server URL
     * @param baseDn LDAP base DN
     * @param idAttr LDAP user identifier attribute
     */
    public LdapAuthentication(String baseUrl, String baseDn, String idAttr) {
        this.baseDn = baseDn;
        this.idAttr = idAttr;
        env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, baseUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
    }

    /**
     * Attempts to authenticate user credentials with the LDAP server
     * 
     * @param username a username
     * @param password a password
     * @return <code>true</code> if authentication was successful,
     *         <code>false</code> otherwise
     */
    public boolean authenticate(String username, String password) {
        try {
            String principal = String.format("%s=%s,%s", idAttr, username,
                    baseDn);
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, password);
            DirContext ctx = new InitialDirContext(env);
            ctx.lookup(principal);
            ctx.close();
            return true;
        } catch (NamingException ne) {
            log.warn("Failed LDAP lookup", ne);
        }
        return false;
    }
}
