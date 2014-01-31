package com.googlecode.fascinator.portal.workflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.internal.KeyValue;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
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
import com.googlecode.fascinator.portal.services.PortalManager;
import com.googlecode.fascinator.portal.services.VelocityService;
import com.googlecode.fascinator.portal.workflow.components.HtmlButton;
import com.googlecode.fascinator.portal.workflow.components.HtmlComponent;
import com.googlecode.fascinator.portal.workflow.components.HtmlDiv;
import com.googlecode.fascinator.portal.workflow.components.HtmlFieldElement;
import com.googlecode.fascinator.portal.workflow.components.HtmlForm;
import com.googlecode.fascinator.portal.workflow.components.HtmlFormLayout;
import com.googlecode.fascinator.portal.workflow.components.HtmlValidationFunction;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

public class SimpleWorkflowHelper {

    private Storage storage = null;
    private VelocityService velocityService = null;
    private JsonSimple systemConfiguration = null;
    private String portalId = null;
    private VelocityContext parentVelocityContext = null;
    private VelocityContext baseVelocityContext = new VelocityContext();
    private MessagingServices messagingServices = null;
    private SimpleWorkflowPageCache cachedPages;
    

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SimpleWorkflowHelper() throws MessagingException {
        messagingServices = MessagingServices.getInstance();
        cachedPages = (SimpleWorkflowPageCache) ApplicationContextProvider
                .getApplicationContext().getBean("simpleWorkflowPageCache");
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

    public void updateObjectMetadata(String oid, List<KeyValue> data)
            throws StorageException, IOException {

        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);
        Properties tfObjMeta = digitalObject.getMetadata();
        for (KeyValue keyValue : data) {
            tfObjMeta.setProperty(keyValue.getKey(), keyValue.getValue());
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tfObjMeta.store(output, null);
        ByteArrayInputStream input = new ByteArrayInputStream(
                output.toByteArray());
        StorageUtils.createOrUpdatePayload(digitalObject, "TF-OBJ-META", input);

    }

    public void updateTFPackage(String oid, FormData formData)
            throws StorageException, IOException {
        // Get all the required objects
        String pid = getTFPackagePid(oid);
        JsonSimple tfPackage = getTFPackage(oid, pid);
        DigitalObject digitalObject = StorageUtils.getDigitalObject(storage,
                oid);

        Map<String, Integer> repeatableFormFields = new HashMap<String, Integer>();
        Pattern p = Pattern
                .compile("([A-Za-z0-9\\.\\:]+\\.)([0-9]+)(\\.[A-Za-z0-9\\.\\:]*)");

        Set<String> fields = formData.getFormFields();
        for (String field : fields) {
            String fieldValue = formData.get(field);
            if (fieldValue != null) {
                tfPackage.getJsonObject().put(field, fieldValue);
            }

            Matcher m = p.matcher(field);
            m.find();
            if (m.matches()) {
                String fieldId = m.group(1) + "0" + m.group(3);
                int fieldIndex = new Integer(m.group(2));
                Integer count = repeatableFormFields.get(fieldId);
                if (count == null || count < fieldIndex) {
                    repeatableFormFields.put(fieldId, fieldIndex);
                }
            }
        }

        // search through the tfPackage and remove anything greater than the
        // highest index in the form
        for (String repeatableFormField : repeatableFormFields.keySet()) {
            int count = repeatableFormFields.get(repeatableFormField);
            count++;
            Matcher m = p.matcher(repeatableFormField);
            m.find();
            while (existsInTFPackage(tfPackage, m.group(1) + count + m.group(3))) {
                tfPackage.getJsonObject().remove(
                        m.group(1) + count + m.group(3));
                count++;
            }
        }

        digitalObject.updatePayload(pid, new ByteArrayInputStream(tfPackage
                .toString().getBytes()));

    }

    private boolean existsInTFPackage(JsonSimple tfPackage, String path) {
        return tfPackage.getJsonObject().get(path) != null;
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

        String step = workflowMetadata.getString(null, "step");
        SimpleWorkflowPageCacheKey key = new SimpleWorkflowPageCacheKey(portalId,
                workflowId, step);
        
        buildBaseVelocityContext();
        HtmlForm form = new HtmlForm();
        JsonObject stepObject = formConfiguration.getObject("stages", step);
        JSONArray formJsonArray;
        if (stepObject.get("config-file") != null) {
            stepObject = getJsonObject((String) stepObject
                    .get("config-file"));
            formJsonArray = (JSONArray) stepObject.get("divs");
        } else {
            formJsonArray = (JSONArray) stepObject.get("divs");
        }
        String htmlFooter = (String) stepObject.get("form-footer");
        if (htmlFooter != null) {
            form.setHtmlFooter(htmlFooter);
        }
        if (cachedPages.containsKey(key)) {
            return renderPageTemplate(cachedPages.get(key),form);
        } else {
            
         
            
            for (int i = 0; i < formJsonArray.size(); i++) {
                JsonObject htmlDivJson = (JsonObject) formJsonArray.get(i);
                if (htmlDivJson.get("config-file") != null) {
                    htmlDivJson = getJsonObject((String) htmlDivJson
                            .get("config-file"));
                }
                form.addHtmlDiv(getHtmlDiv(htmlDivJson));
            }

            JSONArray buttonJsonArray = (JSONArray) stepObject.get("buttons");
            for (int i = 0; i < buttonJsonArray.size(); i++) {
                form.addHtmlButton(getHtmlButton((JsonObject) buttonJsonArray
                        .get(i)));
            }

            

            JsonObject validationFunction = (JsonObject) stepObject
                    .get("validation-function");

            if (validationFunction != null) {
                form.setValidationFunction(getValidationFunction(validationFunction));
            }

            JSONArray customValidators = (JSONArray) stepObject
                    .get("validators");

            if (customValidators != null) {
                form.setCustomValidators(getValidatorList(customValidators));
            }

            JsonObject formLayout = (JsonObject) stepObject.get("form-layout");

            if (formLayout != null) {
                form.setHtmlFormLayout(getFormLayout(formLayout));
            }
            String output = renderFormHtml(form);
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();            
            StringReader reader = new StringReader(output);
            SimpleNode node = runtimeServices.parse(reader, "template");
            Template template = new Template();
            template.setRuntimeServices(runtimeServices);
            template.setData(node);
            template.initDocument();            
            cachedPages.put(key, template);
            
            return renderPageTemplate(template,form);
        }
    }

    private void buildBaseVelocityContext() {
		for(Object key : parentVelocityContext.getKeys()) {
			if(!key.equals("formData") && !key.equals("velocityContext")) {
				baseVelocityContext.put((String)key, parentVelocityContext.get((String)key));
			}
		}
		
	}

	private String renderPageTemplate(Template template, HtmlForm form) throws Exception {
		VelocityContext vc = new VelocityContext(baseVelocityContext);
		FormData formData = (FormData)parentVelocityContext.get("formData");
		for (String field : formData.getFormFields()) {
			vc.put(field,formData.get(field));
		}
		String formFooterHtml = renderFormFooterHtml(form.getHtmlFooter());
        vc.put("formFooterHtml", formFooterHtml);
    	StringWriter writer = new StringWriter();
		template.merge(vc,writer);
		return writer.toString();
	}

	private JsonObject getJsonObject(String jsonObjectPath) throws IOException {
        File workflowConfigFile = FascinatorHome.getPathFile(jsonObjectPath);
        return new JsonSimple(workflowConfigFile).getJsonObject();
    }

    private List<String> getValidatorList(JSONArray customValidators) {
        List<String> validatorList = new ArrayList<String>();
        for (int i = 0; i < customValidators.size(); i++) {
            validatorList.add((String) customValidators.get(i));
        }
        return validatorList;
    }

    private HtmlValidationFunction getValidationFunction(JsonObject jsonObject) {
        HtmlValidationFunction htmlValidationFunction = new HtmlValidationFunction();
        htmlValidationFunction.setComponentTemplateName((String) jsonObject
                .get("component-type"));

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Set<Object> keys = jsonObject.keySet();
        for (Object key : keys) {
            parameterMap.put((String) key, jsonObject.get(key));
        }

        htmlValidationFunction.setParameterMap(parameterMap);
        return htmlValidationFunction;
    }

    private HtmlFormLayout getFormLayout(JsonObject jsonObject) {
        HtmlFormLayout htmlFormLayout = new HtmlFormLayout();
        htmlFormLayout.setComponentTemplateName((String) jsonObject
                .get("component-type"));

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Set<Object> keys = jsonObject.keySet();
        for (Object key : keys) {
            parameterMap.put((String) key, jsonObject.get(key));
        }

        htmlFormLayout.setParameterMap(parameterMap);

        return htmlFormLayout;
    }

    private String renderFormHtml(HtmlForm form) throws Exception {

        String divElementsHtml = renderDivElementsHtml(form);

        String fieldElementsHtml = renderFieldElementsHtml(form
                .getHtmlFieldElements());

        String buttonElementsHtml = renderButtonElementsHtml(form);

        String validationFunction = renderValidationFunction(form
                .getValidationFunction());
        
        String formLayout = StringUtils.EMPTY;
        if (form.getHtmlFormLayout() != null) {
            formLayout = renderFormLayoutCode(form.getHtmlFormLayout());
        }

        String validatorFunctions = renderValidatorFunctions(form
                .getCustomValidators());

        

        // Now that we have generated the elements we need for the html form.
        // Wrap it in the general form template
        VelocityContext vc = new VelocityContext(baseVelocityContext);
        vc.put("fieldElementsHtml", fieldElementsHtml);
        vc.put("buttonElementsHtml", buttonElementsHtml);
        vc.put("divElementsHtml", divElementsHtml);
        vc.put("formLayoutCode", formLayout);
        vc.put("validationFunction", validationFunction);
        vc.put("validatorFunctions", validatorFunctions);

        StringWriter pageContentWriter = new StringWriter();
        velocityService.renderTemplate(portalId,
                "form-components/form-template", vc, pageContentWriter);

        return pageContentWriter.toString();
    }

    private String renderValidatorFunctions(List<String> customValidators)
            throws Exception {
        VelocityContext vc = new VelocityContext(baseVelocityContext);

        String portalHome = systemConfiguration.getString(
                PortalManager.DEFAULT_PORTAL_HOME, "portal", "home");

        File coreValidatorDir = new File(portalHome
                + "/default/default/form-components/validators");
        if (coreValidatorDir.exists()) {
            String[] defaultValidators = coreValidatorDir
                    .list(new SuffixFileFilter(".vm"));
            for (String defaultValidator : defaultValidators) {
                customValidators.add(defaultValidator.substring(0,
                        defaultValidator.length() - 3));
            }
        }
        String validatorFunctions = "";
        for (String validator : customValidators) {
            if (velocityService.resourceExists(portalId,
                    "form-components/validators/" + validator + ".vm") != null) {

                StringWriter pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId,
                        "form-components/validators/" + validator, vc,
                        pageContentWriter);
                validatorFunctions += pageContentWriter.toString();

            }

        }

        return validatorFunctions;
    }

    private String renderFormLayoutCode(HtmlFormLayout htmlFormLayout)
            throws Exception {
        VelocityContext vc = new VelocityContext(baseVelocityContext);

        if (velocityService.resourceExists(
                portalId,
                "form-components/form-layouts/"
                        + htmlFormLayout.getComponentTemplateName() + ".vm") != null) {
            Map<String, Object> parameterMap = htmlFormLayout.getParameterMap();
            Set<String> keySet = parameterMap.keySet();
            for (String key : keySet) {
                vc.put(key, parameterMap.get(key));
            }
            // Render the component's velocity template as a String
            StringWriter pageContentWriter = new StringWriter();
            velocityService.renderTemplate(
                    portalId,
                    "form-components/form-layouts/"
                            + htmlFormLayout.getComponentTemplateName(), vc,
                    pageContentWriter);
            return pageContentWriter.toString();
        }
        // no layout
        String formLayoutCode = "";
        return formLayoutCode;
    }

    private String renderValidationFunction(
            HtmlValidationFunction validationFunction) throws Exception {
        VelocityContext vc = new VelocityContext(baseVelocityContext);
        String validationFunctionTemplate = validationFunction
                .getComponentTemplateName();
        if (velocityService.resourceExists(portalId,
                "form-components/validation-functions/"
                        + validationFunctionTemplate + ".vm") != null) {

            Map<String, Object> parameterMap = validationFunction
                    .getParameterMap();
            Set<String> keySet = parameterMap.keySet();
            for (String key : keySet) {
                vc.put(key, parameterMap.get(key));
            }
            // Render the component's velocity template as a String
            StringWriter pageContentWriter = new StringWriter();
            velocityService
                    .renderTemplate(portalId,
                            "form-components/validation-functions/"
                                    + validationFunctionTemplate, vc,
                            pageContentWriter);
            return pageContentWriter.toString();
        }
        // no validation
        String validationFunctionString = "function doValidation() { return true;}";
        return validationFunctionString;
    }

    private String renderFormFooterHtml(String htmlFooterTemplate)
            throws Exception {
        VelocityContext vc = new VelocityContext(baseVelocityContext);
        if (velocityService.resourceExists(portalId, "form-components/"
                + htmlFooterTemplate + ".vm") != null) {
            // Render the component's velocity template as a String
            StringWriter pageContentWriter = new StringWriter();
            velocityService.renderTemplate(portalId, "form-components/"
                    + htmlFooterTemplate, vc, pageContentWriter);
            return pageContentWriter.toString();
        }
        return "";
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

                VelocityContext vc = new VelocityContext(baseVelocityContext);

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
                VelocityContext vc = new VelocityContext(baseVelocityContext);

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
        VelocityContext vc = new VelocityContext(baseVelocityContext);
        

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
            if ("group".equals(htmlFieldElement.getComponentTemplateName())) {
                fieldElementsHtml += renderGroupElement(htmlFieldElement);
            } else {
                String fieldElementName = "form-components/field-elements/"
                        + htmlFieldElement.getComponentTemplateName();
                String buttonElementName = "form-components/button-elements/"
                        + htmlFieldElement.getComponentTemplateName();
                String fieldElementHtml = "";
                if (velocityService.resourceExists(portalId, fieldElementName
                        + ".vm") != null) {
                    fieldElementHtml = renderHtmlFieldElement(htmlFieldElement,
                            fieldElementName);
                } else if (velocityService.resourceExists(portalId,
                        buttonElementName + ".vm") != null) {
                    fieldElementHtml = renderHtmlFieldElement(htmlFieldElement,
                            buttonElementName);
                }

                fieldElementsHtml += fieldElementHtml;
            }
        }
        return fieldElementsHtml;
    }

    private String renderHtmlFieldElement(HtmlFieldElement htmlFieldElement,
            String fieldElementName) throws Exception {
        VelocityContext vc = new VelocityContext(baseVelocityContext);

        Map<String, Object> parameterMap = htmlFieldElement.getParameterMap();
        Set<String> keySet = parameterMap.keySet();
        for (String key : keySet) {
            vc.put(key, parameterMap.get(key));
        }

        // Render the component's velocity template as a String
        StringWriter pageContentWriter = new StringWriter();
        velocityService.renderTemplate(portalId, fieldElementName, vc,
                pageContentWriter);
        String componentHtml = pageContentWriter.toString();

        if (htmlFieldElement.hasValidation()) {
            vc.put("validation", htmlFieldElement.getValidation());
            vc.put("elementHtml", componentHtml);
            pageContentWriter = new StringWriter();
            velocityService
                    .renderTemplate(portalId,
                            "form-components/validation-wrapper", vc,
                            pageContentWriter);
            componentHtml = pageContentWriter.toString();
        }

        // Inject this components html for use in the
        // component-wrapper
        // template
        vc.put("elementHtml", componentHtml);

        pageContentWriter = new StringWriter();
        velocityService.renderTemplate(portalId,
                "form-components/component-wrapper", vc, pageContentWriter);
        String fieldElementHtml = pageContentWriter.toString();
        return fieldElementHtml;
    }

    private String renderGroupElement(HtmlFieldElement htmlFieldElement)
            throws Exception {
        JSONArray fieldsArray = (JSONArray) htmlFieldElement.getParameterMap()
                .get("fields");
        List<HtmlFieldElement> fieldElements = new ArrayList<HtmlFieldElement>();
        for (int i = 0; i < fieldsArray.size(); i++) {
            fieldElements
                    .add(getHtmlComponent((JsonObject) fieldsArray.get(i)));
        }
        String fieldElementsHtml = renderFieldElementsHtml(fieldElements);

        String groupTemplate = (String) htmlFieldElement.getParameterMap().get(
                "template");
        StringWriter pageContentWriter = new StringWriter();
        if (velocityService.resourceExists(portalId,
                "form-components/field-elements/" + groupTemplate + ".vm") != null) {
            VelocityContext vc = new VelocityContext(baseVelocityContext);
            vc.put("fieldElementsHtml", fieldElementsHtml);
            Set<String> keys = htmlFieldElement.getParameterMap().keySet();
            for (String key : keys) {
                vc.put(key, htmlFieldElement.getParameterMap().get(key));
            }
            velocityService.renderTemplate(portalId,
                    "form-components/field-elements/" + groupTemplate, vc,
                    pageContentWriter);
        }

        return pageContentWriter.toString();
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

        if (jsonObject.get("validation") != null) {
            htmlComponent.setValidation(((JsonObject) jsonObject
                    .get("validation")));
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
