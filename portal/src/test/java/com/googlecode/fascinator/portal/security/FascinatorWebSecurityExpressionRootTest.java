package com.googlecode.fascinator.portal.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.web.FilterInvocation;

import com.googlecode.fascinator.AccessManager;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

public class FascinatorWebSecurityExpressionRootTest {
    FascinatorWebSecurityExpressionRoot fascinatorWebSecurityExpressionRoot;
    Properties properties = new Properties();
    private List<String> allowedRoles = Arrays.asList("reviewer", "admin");
    private List<String> allowedUsers = Arrays.asList("sampleUser", "admin");

    public void initialise() {
        initialise(properties, allowedRoles, allowedUsers, null);
    }

    public void initialise(Properties properties) {
        initialise(properties, allowedRoles, allowedUsers, null);
    }

    public void initialise(JsonSimple workflowconfig) {
        initialise(properties, allowedRoles, allowedUsers, workflowconfig);
    }

    public void initialise(List<String> allowedRoles, List<String> allowedUsers) {
        initialise(properties, allowedRoles, allowedUsers, null);
    }

    public void initialise(Properties properties, List<String> allowedRoles,
            List<String> allowedUsers, JsonSimple workflowconfig) {
        try {
            JsonObject workflowMetadataJson = new JsonObject();
            workflowMetadataJson.put("id", "arms");
            workflowMetadataJson.put("step", "draft");

            if (workflowconfig == null) {
                workflowconfig = new JsonSimple(
                        "{\"stages\": [ { \"name\": \"draft\", \"owner_edit_allowed\": \"true\"} ]}");
            }

            Authentication auth = mock(Authentication.class);
            // Current user: testUser
            when(auth.getPrincipal()).thenReturn("testUser");
            List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
            // Current user's role
            List<GrantedAuthorityImpl> grantedAuthorityImpls = Arrays
                    .asList((new GrantedAuthorityImpl("requester")));
            for (GrantedAuthorityImpl grantedAuthorityImpl : grantedAuthorityImpls) {
                grantedAuthorities.add(grantedAuthorityImpl);
            }
            when(auth.getAuthorities()).thenReturn(grantedAuthorities);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn(
                    "default/sampleUrl/7af2a55aae2a3cae29da4ae2122b1c58");
            when(request.getQueryString()).thenReturn("");

            FilterInvocation fi = mock(FilterInvocation.class);
            when(fi.getRequest()).thenReturn(request);

            Payload workflowMetadataPayload = mock(Payload.class);
            when(workflowMetadataPayload.open()).thenReturn(
                    new ByteArrayInputStream(workflowMetadataJson.toString()
                            .getBytes()));

            DigitalObject digitalObject = mock(DigitalObject.class);
            when(digitalObject.getMetadata()).thenReturn(properties);
            when(digitalObject.getPayload("workflow.metadata")).thenReturn(
                    workflowMetadataPayload);

            Storage storage = mock(Storage.class);
            when(storage.createObject("7af2a55aae2a3cae29da4ae2122b1c58"))
                    .thenReturn(digitalObject);
            when(storage.getObject("7af2a55aae2a3cae29da4ae2122b1c58"))
                    .thenReturn(digitalObject);

            AccessControlManager accessControl = mock(AccessManager.class);
            when(accessControl.getRoles("7af2a55aae2a3cae29da4ae2122b1c58"))
                    .thenReturn(allowedRoles);
            when(accessControl.getUsers("7af2a55aae2a3cae29da4ae2122b1c58"))
                    .thenReturn(allowedUsers);
            fascinatorWebSecurityExpressionRoot = new FascinatorWebSecurityExpressionRoot(
                    auth, fi, storage, accessControl);
            // Reflectively set workflowConfig so that it doesn't try and find
            // one on the disk
            Field workflowConfigJsonField = FascinatorWebSecurityExpressionRoot.class
                    .getDeclaredField("workflowConfigJson");
            workflowConfigJsonField.setAccessible(true);
            workflowConfigJsonField.set(fascinatorWebSecurityExpressionRoot,
                    workflowconfig);
        } catch (Exception e) {
            // We aren't testing anything that throws a checked exception
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testOwnerHasWorkflowAccess() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "testUser");
        initialise(ownerProperties);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasWorkflowAccess());
    }

    @Test
    public void testNoOwnerHasNoWorkflowAccess() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "someoneelse");
        initialise(ownerProperties);
        Assert.assertFalse(fascinatorWebSecurityExpressionRoot
                .hasWorkflowAccess());
    }

    @Test
    public void testGuestHasWorkflowAccess() throws IOException {
        JsonSimple workflowconfig = new JsonSimple(
                "{\"stages\": [ { \"name\": \"draft\", \"owner_edit_allowed\": \"true\", \"guest_owner_edit_allowed\": \"true\"} ]}");
        initialise(workflowconfig);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasWorkflowAccess());
    }

    @Test
    public void testRoleHasWorkflowAccess() {
        List<String> allowedRoles = Arrays.asList("requester");
        initialise(allowedRoles, allowedUsers);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasWorkflowAccess());
    }

    @Test
    public void testHasNoViewAccess() throws StorageException,
            AccessControlException {
        initialise();
        Assert.assertFalse(fascinatorWebSecurityExpressionRoot.hasViewAccess());
    }

    @Test
    public void testOwnerHasViewAccess() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "testUser");
        initialise(ownerProperties);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot.hasViewAccess());
    }

    @Test
    public void testHasRoleViewAccess() {
        List<String> allowedRoles = Arrays.asList("reviewer", "admin",
                "requester");
        initialise(allowedRoles, allowedUsers);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot.hasViewAccess());
    }

    @Test
    public void testHasUserViewAccess() {
        List<String> allowedUsers = Arrays.asList("testUser");
        initialise(allowedRoles, allowedUsers);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot.hasViewAccess());
    }

    @Test
    public void testHasNoDownloadAccess() throws StorageException,
            AccessControlException {
        initialise();
        Assert.assertFalse(fascinatorWebSecurityExpressionRoot
                .hasDownloadAccess());
    }

    @Test
    public void testOwnerHasDownloadAccess() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "testUser");
        initialise(ownerProperties);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasDownloadAccess());
    }

    @Test
    public void testHasRoleDownloadAccess() {
        List<String> allowedRoles = Arrays.asList("reviewer", "admin",
                "requester");
        initialise(allowedRoles, allowedUsers);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasDownloadAccess());
    }

    @Test
    public void testHasUserDownloadAccess() {
        List<String> allowedUsers = Arrays.asList("testUser");
        initialise(allowedRoles, allowedUsers);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .hasDownloadAccess());
    }

    @Test
    /*
     * 1. guest_owner_edit_allowed property test
     * 2. owner_eidt_allowed
     * 3. isInAllowedRoles
     * */
    public void testIsOwner() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "testUser");
        initialise(ownerProperties);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot.isOwner());
    }

    @Test
    public void testIsOwnerEditAllowed() {
        Properties ownerProperties = (Properties) properties.clone();
        ownerProperties.setProperty("owner", "testUser");
        initialise(ownerProperties);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .isOwnerEditAllowed());
    }

    @Test
    public void testIsGuestOwnerEditAllowed() throws IOException {
        JsonSimple workflowconfig = new JsonSimple(
                "{\"stages\": [ { \"name\": \"draft\", \"owner_edit_allowed\": \"true\", \"guest_owner_edit_allowed\": \"true\"} ]}");
        initialise(workflowconfig);
        Assert.assertTrue(fascinatorWebSecurityExpressionRoot
                .isGuestOwnerEditAllowed());
    }
}
