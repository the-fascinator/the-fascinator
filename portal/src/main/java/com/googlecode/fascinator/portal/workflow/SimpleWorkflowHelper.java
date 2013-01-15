package com.googlecode.fascinator.portal.workflow;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.json.simple.JSONArray;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.messaging.TransactionManagerQueueConsumer;
import com.googlecode.fascinator.portal.FormData;
import com.googlecode.fascinator.portal.services.VelocityService;
import com.googlecode.fascinator.portal.workflow.components.HtmlButton;
import com.googlecode.fascinator.portal.workflow.components.HtmlComponent;
import com.googlecode.fascinator.portal.workflow.components.HtmlDiv;
import com.googlecode.fascinator.portal.workflow.components.HtmlFieldElement;
import com.googlecode.fascinator.portal.workflow.components.HtmlForm;

public class SimpleWorkflowHelper {

    private Storage storage = null;
    private VelocityService velocityService = null;
    private JsonSimple systemConfiguration = null;
    private String portalId = null;
    private VelocityContext parentVelocityContext = null;
    private MessagingServices messagingServices = null;

    public SimpleWorkflowHelper() throws MessagingException {
        messagingServices = MessagingServices.getInstance();
    }

    public String updateWorkflowMetadata(String oid, String action)
            throws StorageException, IOException {
        // Get all the required objects
        String targetStep = null;
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        JsonSimple workflowMetadata = getWorkflowMetadata(digitalObject);
        String workflowId = workflowMetadata.getString(null, "id");
        JsonSimple workflowConfiguration = getWorkflowConfiguration(workflowId);

        JSONArray workflowStages = workflowConfiguration.getArray("stages");
        JsonObject workflowStageConfiguration = null;
        for (int i = 0; i < workflowStages.size(); i++) {
            JsonObject workflowStage = (JsonObject) workflowStages.get(i);
            if (workflowMetadata.getJsonObject().get("step")
                    .equals(workflowStage.get("name"))) {
                workflowStageConfiguration = workflowStage;
                break;
            }
        }

        JSONArray workflowStepActions = (JSONArray) workflowStageConfiguration
                .get("actions");
        for (int i = 0; i < workflowStepActions.size(); i++) {
            JsonObject workflowStepAction = (JsonObject) workflowStepActions
                    .get(i);
            if (action.equals(workflowStepAction.get("action-name"))) {
                targetStep = (String) workflowStepAction.get("target-step");
                progressActionToNextStep(digitalObject, workflowMetadata,
                        targetStep);
            }
        }

        return targetStep;
    }

    private void progressActionToNextStep(DigitalObject digitalObject,
            JsonSimple workflowMetadata, String targetStep)
            throws StorageException {

        workflowMetadata.getJsonObject().put("targetStep", targetStep);
        digitalObject
                .updatePayload("workflow.metadata", new ByteArrayInputStream(
                        workflowMetadata.toString().getBytes()));
    }

    public void updateTFPackage(String oid, FormData formData)
            throws StorageException, IOException {
        // Get all the required objects
        String pid = getTFPackagePid(oid);
        JsonSimple tfPackage = getTFPackage(oid, pid);
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        JsonSimple workflowMetadata = getWorkflowMetadata(digitalObject);
        String workflowId = workflowMetadata.getString(null, "id");
        JsonSimple workflowConfiguration = getWorkflowConfiguration(workflowId);
        JsonSimple formConfiguration = getFormConfiguration(workflowConfiguration);

        // Find out what fields were actually present on this form. We don't
        // want users to be able to poke additional values on the request
        List<String> fieldList = new ArrayList<String>();
        JSONArray formJsonArray = formConfiguration.getArray("stages",
                workflowMetadata.getString(null, "step"), "fields");
        for (int i = 0; i < formJsonArray.size(); i++) {
            fieldList.add(new JsonSimple((JsonObject) formJsonArray.get(i))
                    .getString("", "field-name"));
        }

        for (String field : fieldList) {
            tfPackage.getJsonObject().put(field, formData.get(field));
        }

        digitalObject.updatePayload(pid, new ByteArrayInputStream(tfPackage
                .toString().getBytes()));

    }

    public void reindex(String oid, String step, String username)
            throws StorageException, IOException, MessagingException {
        JsonObject message = new JsonObject();
        message.put("oid", oid);
        if (step == null) {
            message.put("eventType", "ReIndex");
        } else {
            message.put("eventType", "NewStep : " + step);
            message.put("newStep", step);
        }

        message.put("quickIndex", true);
        message.put("username", username);
        message.put("context", "Workflow");
        message.put("task", "workflow");

        messagingServices
                .queueMessage(TransactionManagerQueueConsumer.LISTENER_ID,
                        message.toString());

    }

    private JsonSimple getTFPackage(String oid, String pid)
            throws StorageException, IOException {
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        InputStream tfPackageInputStream = digitalObject.getPayload(pid).open();

        return new JsonSimple(tfPackageInputStream);
    }

    public JsonSimple getTFPackage(String oid) throws StorageException,
            IOException {
        return getTFPackage(oid, getTFPackagePid(oid));
    }

    private String getTFPackagePid(String oid) throws StorageException {
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        for (String pid : digitalObject.getPayloadIdList()) {
            if (pid.endsWith("tfpackage")) {
                return pid;
            }
        }
        return null;
    }

    public String getFormHtml(String oid) throws Exception {

        // Load object from storage and get the necessary configuration and
        // metadata files
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        JsonSimple workflowMetadata = getWorkflowMetadata(digitalObject);
        String workflowId = workflowMetadata.getString(null, "id");
        JsonSimple workflowConfiguration = getWorkflowConfiguration(workflowId);
        JsonSimple formConfiguration = getFormConfiguration(workflowConfiguration);

        HtmlForm form = new HtmlForm();

        JSONArray formJsonArray = formConfiguration.getArray("stages",
                workflowMetadata.getString(null, "step"), "divs");
        for (int i = 0; i < formJsonArray.size(); i++) {
            form.addHtmlDiv(getHtmlDiv((JsonObject) formJsonArray.get(i)));
        }

        JSONArray buttonJsonArray = formConfiguration.getArray("stages",
                workflowMetadata.getString(null, "step"), "buttons");
        for (int i = 0; i < buttonJsonArray.size(); i++) {
            form.addHtmlButton(getHtmlButton((JsonObject) buttonJsonArray
                    .get(i)));
        }

        String output = renderFormHtml(form);

        return output;
    }

    private String renderFormHtml(HtmlForm form) throws Exception {

        String divElementsHtml = renderDivElementsHtml(form);

        String fieldElementsHtml = renderFieldElementsHtml(form
                .getHtmlFieldElements());

        String buttonElementsHtml = renderButtonElementsHtml(form);

        // Now that we have generated the elements we need for the html form.
        // Wrap it in the general form template
        VelocityContext vc = new VelocityContext();
        vc.put("fieldElementsHtml", fieldElementsHtml);
        vc.put("buttonElementsHtml", buttonElementsHtml);
        vc.put("divElementsHtml", divElementsHtml);
        StringWriter pageContentWriter = new StringWriter();
        velocityService.renderTemplate(portalId,
                "form-components/form-template", vc, pageContentWriter);

        return pageContentWriter.toString();
    }

    private String renderDivElementsHtml(HtmlForm form) throws Exception {
        String divElementsHtml = "";
        List<HtmlDiv> htmlDivs = form.getHtmlDivs();
        int divorder = 1;
        for (HtmlDiv htmlDiv : htmlDivs) {
            String htmlDivTemplate = "form-components/"
                    + htmlDiv.getComponentTemplateName();
            if (velocityService.resourceExists(portalId, htmlDivTemplate
                    + ".vm") != null) {

                String fieldElementsHtml = renderFieldElementsHtml(htmlDiv
                        .getHtmlFieldElements());

                VelocityContext vc = new VelocityContext();

                Object[] parentKeys = parentVelocityContext.getKeys();
                for (Object key : parentKeys) {
                    vc.put((String) key,
                            parentVelocityContext.get((String) key));
                }
                vc.put("divorder", divorder++);
                vc.put("fieldElementsHtml", fieldElementsHtml);

                Map<String, Object> parameterMap = htmlDiv.getParameterMap();
                Set<String> keySet = parameterMap.keySet();
                for (String key : keySet) {
                    vc.put(key, parameterMap.get(key));
                }

                // Render the component's velocity template as a String
                StringWriter pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId, htmlDivTemplate, vc,
                        pageContentWriter);

                divElementsHtml += pageContentWriter.toString();
            }
        }
        return divElementsHtml;
    }

    private String renderButtonElementsHtml(HtmlForm form) throws Exception {
        String buttonElementsHtml = "";
        List<HtmlButton> htmlButtons = form.getHtmlButtons();
        for (HtmlComponent htmlButton : htmlButtons) {
            String pageName = "form-components/button-elements/"
                    + htmlButton.getComponentTemplateName();
            if (velocityService.resourceExists(portalId, pageName + ".vm") != null) {
                VelocityContext vc = new VelocityContext();

                Object[] parentKeys = parentVelocityContext.getKeys();
                for (Object key : parentKeys) {
                    vc.put((String) key,
                            parentVelocityContext.get((String) key));
                }

                Map<String, Object> parameterMap = htmlButton.getParameterMap();
                Set<String> keySet = parameterMap.keySet();
                for (String key : keySet) {
                    vc.put(key, parameterMap.get(key));
                }

                // Render the component's velocity template as a String
                StringWriter pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId, pageName, vc,
                        pageContentWriter);

                buttonElementsHtml += pageContentWriter.toString();
            }
        }
        VelocityContext vc = new VelocityContext();
        Object[] parentKeys = parentVelocityContext.getKeys();
        for (Object key : parentKeys) {
            vc.put((String) key, parentVelocityContext.get((String) key));
        }

        // Inject this buttonELementsHtml for use in the button-wrapper
        // template
        vc.put("buttonHtml", buttonElementsHtml);

        StringWriter pageContentWriter = new StringWriter();
        velocityService.renderTemplate(portalId,
                "form-components/button-wrapper", vc, pageContentWriter);

        buttonElementsHtml = pageContentWriter.toString();

        return buttonElementsHtml;
    }

    private String renderFieldElementsHtml(
            List<HtmlFieldElement> htmlfieldElements) throws Exception {
        String fieldElementsHtml = "";

        for (HtmlFieldElement htmlFieldElement : htmlfieldElements) {
            String pageName = "form-components/field-elements/"
                    + htmlFieldElement.getComponentTemplateName();
            if (velocityService.resourceExists(portalId, pageName + ".vm") != null) {
                VelocityContext vc = new VelocityContext();

                Object[] parentKeys = parentVelocityContext.getKeys();
                for (Object key : parentKeys) {
                    vc.put((String) key,
                            parentVelocityContext.get((String) key));
                }

                Map<String, Object> parameterMap = htmlFieldElement
                        .getParameterMap();
                Set<String> keySet = parameterMap.keySet();
                for (String key : keySet) {
                    vc.put(key, parameterMap.get(key));
                }

                // Render the component's velocity template as a String
                StringWriter pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId, pageName, vc,
                        pageContentWriter);
                String componentHtml = pageContentWriter.toString();

                // Inject this components html for use in the component-wrapper
                // template
                vc.put("elementHtml", componentHtml);

                pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId,
                        "form-components/component-wrapper", vc,
                        pageContentWriter);

                fieldElementsHtml += pageContentWriter.toString();
            }
        }
        return fieldElementsHtml;
    }

    private HtmlFieldElement getHtmlComponent(JsonObject jsonObject) {
        HtmlFieldElement htmlComponent = new HtmlFieldElement();
        htmlComponent.setComponentTemplateName((String) jsonObject
                .get("component-type"));

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Set<Object> keys = jsonObject.keySet();
        for (Object key : keys) {
            parameterMap.put((String) key, jsonObject.get(key));
        }

        htmlComponent.setParameterMap(parameterMap);
        return htmlComponent;
    }

    private HtmlDiv getHtmlDiv(JsonObject jsonObject) {
        HtmlDiv htmlDiv = new HtmlDiv();
        String componentTemplateName = (String) jsonObject
                .get("component-type");
        if (componentTemplateName != null) {
            htmlDiv.setComponentTemplateName(componentTemplateName);
        }

        JSONArray formJsonArray = (JSONArray) jsonObject.get("fields");

        for (int i = 0; i < formJsonArray.size(); i++) {
            htmlDiv.addHtmlFieldElement(getHtmlComponent((JsonObject) formJsonArray
                    .get(i)));
        }

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Set<Object> keys = jsonObject.keySet();
        for (Object key : keys) {
            parameterMap.put((String) key, jsonObject.get(key));
        }

        htmlDiv.setParameterMap(parameterMap);
        return htmlDiv;
    }

    private HtmlButton getHtmlButton(JsonObject jsonObject) {
        HtmlButton htmlButton = new HtmlButton();
        htmlButton.setComponentTemplateName((String) jsonObject
                .get("component-type"));

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Set<Object> keys = jsonObject.keySet();
        for (Object key : keys) {
            parameterMap.put((String) key, jsonObject.get(key));
        }

        htmlButton.setParameterMap(parameterMap);

        return htmlButton;
    }

    private JsonSimple getFormConfiguration(JsonSimple workflowConfiguration)
            throws IOException {
        String formConfigFileLocation = workflowConfiguration.getString(null,
                "form-configuration");
        File formConfigFile = FascinatorHome
                .getPathFile(formConfigFileLocation);
        return new JsonSimple(formConfigFile);
    }

    private JsonSimple getWorkflowConfiguration(String workflowId)
            throws IOException {
        String workflowConfigFileLocation = (String) systemConfiguration
                .getObject(
                        new Object[] { "portal", "packageTypes", workflowId })
                .get("jsonconfig");
        File workflowConfigFile = FascinatorHome
                .getPathFile("harvest/workflows/" + workflowConfigFileLocation);
        return new JsonSimple(workflowConfigFile);
    }

    private JsonSimple getWorkflowMetadata(DigitalObject digitalObject)
            throws StorageException, IOException {
        return new JsonSimple(digitalObject.getPayload("workflow.metadata")
                .open());
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public void setSystemConfiguration(JsonSimple systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }

    public void setVelocityService(VelocityService velocityService) {
        this.velocityService = velocityService;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public void setParentVelocityContext(VelocityContext parentVelocityContext) {
        this.parentVelocityContext = parentVelocityContext;
    }

}
