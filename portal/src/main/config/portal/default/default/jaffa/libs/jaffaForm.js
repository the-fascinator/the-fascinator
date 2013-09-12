function jaffaForm(jaffaObject) { 
    var jaffa = jaffaObject;
    var form = {};
    var ignoredInputs = ["submit", "reset", "image", "file", "button"];

    // Field holders
    form.fields    = {};
    form.checkbox  = {};
    form.radio     = {};
    form.selects   = {};
    form.textual   = {};
    form.unmanaged = {};
    // Widgets + Field Mappings inside them
    form.widgets      = {};
    form.widgetFields = {};

    // Used to add a field after init() time. They don't need to be marked by
    //  the standard CSS selector, just called by ID (or name for radio inputs)
    //  with this method.
    form.addField = function(fieldId, widgetId) {
        if (fieldId in form.fields) {
            jaffa.logWarning("Field '"+fieldId+"' is already in use.");
            return false;
        }

        // Find by ID
        var valid;
        var element = jaffa.util.getById(fieldId);
        if (element.size() == 1) {
            processField(element);
            if (fieldId in form.fields) {
                // If it came from a widget, log this
                if (widgetId != null) {
                    form.widgetFields[fieldId] = widgetId;
                }
                // Check if there is server data waiting for it
                valid = form.synch(false, fieldId);
                if (valid) {
                    return true;
                }
                // Error condition. We have loaded a control that has restricted
                //  data allowances (ie. a drop-down or something) and the server
                //  data is not valid for this control... back out and error.
                this.ignoreField(fieldId, true);
                jaffa.logError("Data mismatch in field '"+fieldId+"'. Allowed values do not match server data.");
                return false;
            } else {
                jaffa.logError("Unable to add field '"+fieldId+"'.");
                return false;
            }
        }
        if (element.size() > 1) {
            jaffa.logError("There are multiple ID '"+fieldId+"' elements in document. Please validate document before using.");
            return false;
        }

        // If we are still here, we'll try finding by name for a radio group
        var elements = $("input:radio[name=\""+fieldId+"\"]");
        if (elements.size() > 0) {
            elements.each(function(_, elem) {
                processField($(elem));
            });

            if (fieldId in form.fields) {
                // If it came from a widget, log this
                if (widgetId != null) {
                    form.widgetFields[fieldId] = widgetId;
                }
                // Check if there is server data waiting for it
                valid = form.synch(false, fieldId);
                if (valid) {
                    return true;
                }
                // Error condition. We have loaded a control that has restricted
                //  data allowances (ie. a drop-down or something) and the server
                //  data is not valid for this control... back out and error.
                this.ignoreField(fieldId, true);
                jaffa.logError("Data mismatch in field '"+fieldId+"'. Allowed values do not match server data.");
                return false;
            } else {
                jaffa.logError("Unable to add field '"+fieldId+"'.");
                return false;
            }
        }

        jaffa.logWarning("Field '"+fieldId+"' could not be found by name or ID.");
        return false;
    }

    // Add a new widget to the form
    form.addWidget = function(newWidget) {
        form.widgets[newWidget.id()] = newWidget;
        newWidget.startLoading();
    }

    // Can be used to a 'check' a checkbox currently monitored by Jaffa. Also
    //  the optional second 'value' parameter can be used to check a specific
    //  option from a radio input.
    form.check = function(fieldId, value) {
        var element = null;

        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        // Checkboxes
        if (fieldId in form.checkbox) {
            element = form.checkbox[fieldId];
            element.attr("checked", "checked");
            element.trigger("change");
            return form.value(fieldId);
        }
        // Radio Group: 
        if (fieldId in form.radio) {
            if (value == null) {
                jaffa.logWarning("Cannot 'check' a radio group without providing a value. Ignoring request for field '"+fieldId+"'.");
                return null;
            } else {
                element = form.radio[fieldId];
                // Radio group uses name - so should be an array
            	for(var i = 0; i < element.length; i++) {
            		element[i].checked = false;
            		if(element[i].value == value) {
            			element[i].checked = true;
                        element.trigger('change');
            		}
            	}
            	return value; // a none null value
            }
        }
        return null;
    }

    // Get the jQuery element related to a specific field
    form.field = function(fieldId) {
        if (fieldId in form.fields) {
            return form.fields[fieldId];
        }
        return null;
    }

    form.focus = function(fieldId) {
        var field = form.field(fieldId);
        if (field == null) {
            jaffa.logWarning("Recieved focus() request for unknown field '"+fieldId+"'");
            return;
        }

        // If the field is in a tab (could be nested), toggle them
        var tabs = field.parents(".ui-tabs-panel");
        if (tabs != null) {
            tabs.each(function(i, tab) {
                var tabId = $(tab).attr("id");
                $("a[href=#"+tabId+"]").click();
            });
        }
        // Or an accordian... this is difficult as we are going bother with
        var accordians = field.parents(".ui-accordion-content");
        if (accordians != null) {
            accordians.each(function(i, accordian) {
                var header = $(accordian).prev(".ui-accordion-header");
                header.click();
            });
        }
        // Before 
        field.focus();
    }

    // Similar to addField() above, except now we can tell Jaffa to start
    //  ignoring a field it was previously managing.
    form.ignoreField = function(fieldId, preserveServerData) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Cannot remove a field we don't have: '"+fieldId+"'.");
            return false;
        }
        delete form.fields[fieldId];
        if (fieldId in form.checkbox) {
            delete form.checkbox[fieldId];
        }
        if (fieldId in form.radio) {
            delete form.radio[fieldId];
        }
        if (fieldId in form.selects) {
            delete form.selects[fieldId];
        }
        if (fieldId in form.textual) {
            delete form.textual[fieldId];
        }
        if (fieldId in form.widgets) {
            delete form.widgets[fieldId];
        }
        if (fieldId in jaffa.serverData && !preserveServerData) {
            delete jaffa.serverData[fieldId];
        }
        return true;
    }

    form.isChecked = function(fieldId) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        if (fieldId in form.checkbox) {
            return form.checkbox[fieldId].is(":checked");
        }
        if (fieldId in form.radio) {
            // You can just do an .is() test on the radio group
            var checked = form.radio[fieldId].filter(":checked");
            return checked.size() == 1;
        }
        return false;
    }

    form.isEmpty = function(fieldId) {
        var result = form.value(fieldId);
        if (result != null && result != "") {
            return false;
        }
        return true;
    }

    form.isSelected = function(fieldId) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        if (fieldId in form.selects) {
            var selected = form.selected(fieldId);
            if (selected != null) {
                return true;
            } else {
                return false;
            }
        }
        if (fieldId in form.checkbox || fieldId in form.radio) {
            // Use isChecked() for checkboxes and radio groups
            return form.isChecked(fieldId);
        }
        return false;
    }

    form.select = function(fieldId, value) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        if (fieldId in form.selects) {
            var element = form.selects[fieldId];
            var option = element.find("option[value=\""+value+"\"]");
            if (option.size() == 0) {
                jaffa.logWarning("Cannot find value '"+value+"' in select control '"+fieldId+"'.");
                return null;
            } else {
                option.attr("selected", "selected");
                element.trigger("change");
                return option.val();
            }
        }
        if (fieldId in form.checkbox || fieldId in form.radio) {
            // Use check() for checkboxes and radio groups
            return form.check(fieldId, value);
        }
        return null;
    }

    form.selected = function(fieldId) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        if (fieldId in form.selects) {
            return form.selects[fieldId].find(":selected").val();
        }
        if (fieldId in form.radio) {
            return form.value(fieldId);
        }
        return null;
    }

    form.synch = function(dataLoad, fieldName) {
        if (dataLoad == null) dataLoad = false;
        var field;

        // Jaffa's 'serverData' has been loaded/modifed,
        //     update our managed fields
        if (dataLoad) {
            // TODO: Testing
            for (field in jaffa.serverData) {
                if (field in form.fields) {
                    var result = form.value(field, jaffa.serverData[field]);
                    if (result == null) {
                        jaffa.logError("Error loading form field '" + field + "'. Unable to assign value to GUI.");
                    } else {
                        jaffa.logDebug("Data loaded and synched: '" + field + "'")
                    }
                } else {
                    jaffa.logWarning("Unmanaged field '" + field + "' in server data!")
                    form.unmanaged[field] = true;
                    jaffa.cb("UnmanagedServerData", field);
                }
            }

        // We are synching our managed fields back into
        //    the data in preparation for submission
        } else {
            // A specific field has been updated via the API(s)
            if (fieldName) {
                // Was it newly added?
                if (fieldName in form.fields && fieldName in form.unmanaged) {
                    jaffa.logInfo("Field '" + fieldName + "' is now managed!")
                    // TODO: Testing
                    var value = form.value(fieldName, jaffa.serverData[fieldName]);
                    if (value == null) {
                        return false;
                    }
                    delete form.unmanaged[fieldName];
                    jaffa.cb("NewManagedServerData", fieldName);

                // Or a direct synch request (why? TODO)
                } else {
                    // TODO: Testing
                    if (fieldName in form.fields) {
                        jaffa.serverData[fieldName] = form.value(fieldName);
                    }
                }

            // All fields
            } else {
                jaffa.logDebug("Performing full field synch()")
                for (field in form.fields) {
                    // TODO: Testing
                    jaffa.serverData[field] = form.value(field);
                }
            }
        }

        return true;
    }

    // Typically used during unit tests/debugging. Test whether or not the
    //  'serverData' value for a field matches the managed fields GUI value.
    form.synchTest = function(fieldName) {
        return (form.value(fieldName) == jaffa.serverData[fieldName]);
    }

    form.uncheck = function(fieldId) {
        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }
        if (fieldId in form.checkbox) {
            var element = form.checkbox[fieldId];
            element.removeAttr("checked");
            element.trigger("change");
            return false;
        }
        if (fieldId in form.radio) {
            return form.check(fieldId, false);
        }
        return null;
    }

    form.value = function(fieldId, value) {
        var element = null;

        if (!(fieldId in form.fields)) {
            jaffa.logWarning("Field '"+fieldId+"' does not exist.");
            return null;
        }

        // TEXT
        if (fieldId in form.textual) {
            // Basic text value
            element = form.textual[fieldId];
            if(element.parent().length == 0 ) {
 				element = $("[id='"+element.attr('id')+"']");
 				form.textual[fieldId] = element;
			}
            if (value != null) {
                element.val(value);
                element.trigger("change");
                return element.val();
            } else {
                return element.val();
            }
        }

        // RADIO
        if (fieldId in form.radio) {
            if (value != null) {
                return form.check(fieldId, value);
            }
            // The value of the checked radio option
            return form.radio[fieldId].filter(":checked").val();
        }

        // CHECKBOX
        if (fieldId in form.checkbox) {
            // Setting value of checkbox
            if (value != null) {
                // Checking
                if (value && value != 'null') {
                    if (form.isChecked(fieldId)) {
                        // No change required
                        return true;
                    } else {
                        return form.check(fieldId);
                    }
                }
                // Unckecking
                if (value === false || value == 'null') {
                    if (form.isChecked(fieldId)) {
                        return form.uncheck(fieldId);
                    } else {
                        // No change required
                        return false;
                    }
                }
                // Invalid
                return null;
            }

            // No write, just read
            if (form.isChecked(fieldId)) {
                return form.checkbox[fieldId].val();
            } else {
                return "null";
            }
        }

        // SELECT
        if (fieldId in form.selects) {
        	element = form.selects[fieldId];
            if(element.parent().length == 0 ) {
 				element = $("[id='"+element.attr('id')+"']");
 				form.selects[fieldId] = element;
			}
            if (value != null) {
                return form.select(fieldId, value);
            }
            return form.selected(fieldId);
        }

        return null;
    }

    // Adds a callback for change() events and makes
    //  sure the field name is passed on as well.
    function addChangeEvent(fieldName, element) {
        element.on("blur",   {field: fieldName}, managedOnBlur);
        element.on("change", {field: fieldName}, managedOnChanged);
    }

    // TODO: Testing - particularly radio groups
    // TODO: Per field triggers/callbacks

    // Decide what sort of validation (if any) should be run on changes
    var validateOnChange = jaffa.config("validateOnChange");
    if (validateOnChange !== false) {
        // Default to true unless explicitly set to false
        validateOnChange = true;
    }
    // Should we validat on blur() too? We'll use
    //  the same validation type as change() though.
    var validateOnBlur = jaffa.config("validateOnBlur");
    if (validateOnBlur !== false) {
        // Default to true unless explicitly set to false
        validateOnBlur = true;
    }
    // If we are validating... what sort?
    if (validateOnChange === true || validateOnBlur === true) {
        var validateType = jaffa.config("validateOnChangeType");
        var validateFunction = null;
        if (validateType == "save") {
            validateFunction = "save";
        }
        // Default to submit() validation
        if (validateType == "submit" || validateFunction == null) {
            validateFunction = "submit";
        }
    }
    function managedOnChanged(event) {
        var fieldName = event.data.field;
        if (validateOnChange) {
            var isValid = jaffa.valid.test(validateFunction, fieldName);
        }

        // Is there a user callback?
        jaffa.cb("OnDataChanged", fieldName, isValid);
        // Was the field in a widget?
        var widgetId = form.widgetFields[fieldName];
        if (widgetId != null) {
            form.widgets[widgetId].change(fieldName, isValid);
        }
        // TODO: This should tie into the value
        //  returned from OnDataChanged() callback?
        return true;
    }
    function managedOnBlur(event) {
        var fieldName = event.data.field;
        if (validateOnBlur) {
            jaffa.valid.test(validateFunction, fieldName);
        }
    }

    function processField(element) {
        /**
         *  Things we want:
         *    <input type="text" id="..."/>
         *    <input type="hidden" id="..."/>
         *    <input type="password" id="..."/>
         *    <input type="checkbox" id="..."/>
         *    <input type="radio" name="..."/>   <== Note we look for 'name'
         *    <select id="...">...</select>
         *    <textarea id="...">...</select>
         *  
         *  Things we don't want:
         *    <input type="submit" id="..."/>
         *    <input type="reset" id="..."/>
         *    <input type="image" id="..."/>
         *    <input type="file" id="..."/>   <== Maybe later
         *    <input type="button" id="..."/>
         */

        var type = element.attr("type");
        var tag = element[0].tagName;
        var id = element.attr("id");

        // Inputs have a lot of different types
        if (tag == "INPUT") {
            // Ignore things we don't want
            if ($.inArray(type, ignoredInputs) != -1) {
                jaffa.logWarning("Ignoring invalid form field. Input tags of type '"+type+"' are not used! ID: '"+id+"'");

            } else {
                if (type == "radio" || type == "checkbox") {
                    // Special, checkboxes
                    if (type == "checkbox") {
                        if (id != null) {
                            form.fields[id] = element;
                            form.checkbox[id] = element;
                            addChangeEvent(id, element);
                            jaffa.logDebug("Form field '"+id+"' found. Type = Checkbox");
                        } else {
                            jaffa.logError("Invalid form field. A checkbox requested for use in the form has no ID!");
                        }
                    }

                    // Special, radio - we actually want to create a jQuery
                    //   reference to the radio group as a whole, referenced
                    //   by name rather than ID.
                    if (type == "radio") {
                        var name = element.attr("name");
                        // We want to avoid all the duplication, just
                        //  use the first radio value we find
                        if (!(name in form.radio)) {
                            var radioElements = $("input[name=\""+name+"\"]:radio");
                            form.fields[name] = radioElements;
                            form.radio[name] = radioElements;
                            addChangeEvent(name, radioElements);
                            jaffa.logDebug("Form field '"+name+"' found. Type = Radio");
                        }
                    }

                // Everything else is text in some form
                } else {
                    form.fields[id] = element;
                    form.textual[id] = element;
                    addChangeEvent(id, element);
                    jaffa.logDebug("Form field '"+id+"' found. Type = "+tag+":"+type);
                }
            }

        // Other than INPUTs...
        } else {
            // Drop-downs
            if (tag == "SELECT") {
                form.fields[id] = element;
                form.selects[id] = element;
                addChangeEvent(id, element);
                jaffa.logDebug("Form field '"+id+"' found. Type = "+tag);

            // Textareas
            } else if (tag == "TEXTAREA") {
                form.fields[id] = element;
                form.textual[id] = element;
                addChangeEvent(id, element);
                jaffa.logDebug("Form field '"+id+"' found. Type = "+tag);

            } else {
                jaffa.logWarning("Ignoring invalid form field. '"+tag+"' tags are not used!");
            }
        }
    }

    // Basic wrapper for checking if any unmanaged data is present
    form.hasUnmanagedData = function() {
        for (var key in form.unmanaged) {
            if (form.unmanaged.hasOwnProperty(key)) {
                return true;
            }
        }
        return false;
    }

    // Send data out via whatever context required. Logic is very similar to both
    function sendData(isValid, preCb, fn, postCb) {
        var unmanagedData = form.hasUnmanagedData();
        var proceed = jaffa.cb(preCb, unmanagedData, isValid, jaffa.serverData);

        // In the absense of a callback (or decision)
        //    only proceed with valid data
        if (proceed == null) {
            proceed = !unmanagedData && isValid;
        }

        // We are sending
        if (proceed === true) {
            // Synch form values first
            form.synch();
            // Send
            jaffa.fn(fn, unmanagedData, isValid, jaffa.serverData);
            // and post-process (if requested)
            jaffa.cb(postCb, unmanagedData, isValid, jaffa.serverData);
            return true;
        } 

        return false;
    }

    form.save = function(hasValidated) {
    	var result;
    	if (hasValidated == true) {
    		jaffa.logInfo("Trying to send data with 'save' context with trusted data");
    		result = sendData(true, "PreSave", "SaveData", "PostSave");
    	} else {
            jaffa.logInfo("Trying to send data with 'save' context");
            result = sendData(jaffa.valid.okToSave(), "PreSave", "SaveData", "PostSave");
    	}
        // TODO: Global validation handling
        return result;
    }

    // Start the submission process
    form.submit = function() {
        jaffa.logInfo("Sending with 'submit' context");
        var result = sendData(jaffa.valid.okToSubmit(), "PreSubmission", "SubmitData", "PostSubmission");
        // TODO: Global validation handling
        return result;
    }

    // Find our form fields
    $(jaffa.config("selectorFormFields")).each(function(_, elem) {
        processField($(elem));
    });
    return form;
}
