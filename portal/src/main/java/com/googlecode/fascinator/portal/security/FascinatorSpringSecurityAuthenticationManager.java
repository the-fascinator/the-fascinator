package com.googlecode.fascinator.portal.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.googlecode.fascinator.RoleManager;
import com.googlecode.fascinator.common.authentication.GenericUser;
import com.googlecode.fascinator.common.authentication.SpringUser;

/**
 * Spring Security Authentication Manager This class assumes that the user has
 * been pre-authenticated by the existing Fascinator Authentication Manager and
 * simply puts the user on the Spring Security Context
 * 
 * @author andrewbrazzatti
 * 
 */
public class FascinatorSpringSecurityAuthenticationManager implements
        AuthenticationManager {

    private RoleManager roleManager = null;

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public Authentication authenticate(Authentication authToken)
            throws AuthenticationException {
        GenericUser genericUser = (GenericUser) authToken.getDetails();
        SpringUser user = new SpringUser();
        user.setUsername(genericUser.getUsername());
        user.setSource(genericUser.getSource());
        List<GrantedAuthority> userRoles = buildRoleList(user);

        return new PreAuthenticatedAuthenticationToken(user.getUsername(),
                user, userRoles);

    }

    private List<GrantedAuthority> buildRoleList(GenericUser user) {
        List<GrantedAuthority> userRoles = new ArrayList<GrantedAuthority>();
        String[] roles = roleManager.getRoles(user.getUsername());
        for (String role : roles) {
            GrantedAuthority authority = new GrantedAuthorityImpl(role);
            userRoles.add(authority);
        }
        return userRoles;
    }

}
