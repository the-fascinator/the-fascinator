/* 
 * The Fascinator - Portal - Security
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.portal.security;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

import com.googlecode.fascinator.api.access.AccessControl;
import com.googlecode.fascinator.api.storage.Storage;

/**
 * Spring security methods for Fascinator.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
public class FascinatorWebSecurityExpressionHandler extends
        DefaultWebSecurityExpressionHandler {

    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private ExpressionParser expressionParser = new SpelExpressionParser();
    private RoleHierarchy roleHierarchy;
    private Storage storage;
    private AccessControl accessControl;

    @Override
    public EvaluationContext createEvaluationContext(
            Authentication authentication, FilterInvocation fi) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        SecurityExpressionRoot root;
        root = new FascinatorWebSecurityExpressionRoot(authentication, fi,
                storage, accessControl);
        root.setTrustResolver(trustResolver);
        root.setRoleHierarchy(roleHierarchy);

        ctx.setRootObject(root);

        return ctx;
    }

    @Override
    public ExpressionParser getExpressionParser() {
        return expressionParser;
    }

    @Override
    public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    public void setAccessControl(AccessControl accessControl) {
        this.accessControl = accessControl;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
