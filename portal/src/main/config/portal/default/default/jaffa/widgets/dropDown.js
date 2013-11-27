var DropDownWidgetBuilder = function($, jaffa) {
    var dropDownClass = jaffa.widgets.baseWidget.extend({
        field: null,
        labelField: null,
        oldField: null,
        oldLabelField: null,

        dropDownData: {},
        dropDownDataMapped: null,

        deleteWidget: function() {
            jaffa.form.ignoreField(this.field);
            if (this.labelField != null) {
              jaffa.form.ignoreField(this.labelField);
            }
            this.getContainer().remove();
        },
        // Identity has been altered, adjust the DOM for all fields
        domUpdate: function(from, to, depth) {
            this._super(from, to, depth);
            // Store, we'll need them to notify Jaffa later
            this.oldField = this.field;
            // Replace the portion of the ID that changed
            this.field = this.oldField.domUpdate(from, to, depth);
            // Update DOM but constrain searches to container, since there may
            //  be very temporary duplicate IDs as sort orders swap
            var container = this.getContainer();
            container.find("select[id=\""+this.oldField+"\"]").attr("id", this.field);
            //  ... and don't forget the label
            container.find("label[for=\""+this.oldField+"\"]").attr("for", this.field);
            // Tell Jaffa to ignore the field's this widget used to manage
            jaffa.form.ignoreField(this.oldField);

            // TODO: Testing
            // Do it all again for labels if they are stored
            if (this.labelField != null) {
                this.oldLabelField = this.labelField;
                this.labelField = this.oldLabelField.domUpdate(from, to, depth);
                container.find("input[id=\""+this.oldLabelField+"\"]").attr("id", this.labelField);
                jaffa.form.ignoreField(this.oldLabelField);
            }
        },
        // Notify Jaffa that field <=> widget relations need to be updated
        //  This is called separately from above to avoid duplicate IDs that
        //   may occur whilst DOM alterations are occuring
        jaffaUpdate: function() {
            // Only synch if an update has effected this widget
            if (this.oldField != null || this.oldLabelField != null) {
                this._super();
            }
            if (this.oldField != null) {
                jaffa.form.addField(this.field, this.id());
                this.oldField = null;
            }
            if (this.oldLabelField != null) {
                jaffa.form.addField(this.labelField, this.id());
                this.oldLabelField = null;
            }
            // TODO: Validation alterations
        },

        // Whereas init() is the constructor, this method is called after Jaffa
        // knows about us and needs us to build UI elements and modify the form.
        buildUi: function() {
            var ui = this.getContainer();
            ui.html("");

            // Field
            this.field = this.getConfig("field");
            if (this.field == null) {
                // TODO: Testing
                jaffa.logError("No field name provided for widget '"+this.id()+"'. This is mandatory!");
                return;
            }

            // Label
            var label = this.getConfig("label");
            if (label != null && label.trim() != "") {
                ui.append("<label for=\""+this.field+"\" class=\"widgetLabel\">"+label+"</label>");
            }

            // Control
            var select = $("<select id=\""+this.field+"\"></select>");
            var classList = this.getConfig("class-list");
            if  (classList != null){
            	select.attr('class', classList);
            }
            this.dropDownData = this.getJsonData() || this.getConfig("option-data");
            
            var defaultValue = this.getConfig("default-value");
            var allowEmpty = this.getConfig("allow-empty");
            if (allowEmpty !== false) {
                allowEmpty = true;
            }
            var emptyText = this.getConfig("empty-text") || "Please select one...";
            if (allowEmpty) {
                select.append($("<option value=\"\">"+emptyText+"</option>"));
            }
            var dataIdKey = this.getConfig("data-id-key");
            var dataLabelKey = this.getConfig("data-label-key");
            var dataListKey = this.getConfig("data-list-key");
            
            if  (dataListKey != null){
                this.dropDownData = this.dropDownData[dataListKey];
            }

             if(Object.prototype.toString.call(this.dropDownData) != '[object Array]') {
              ui.append("<span style='color:red;'> [Error&nbsp;loading&nbsp;data... widget '" + this.id() + "' did not load.] </span>");
              return;
            } 
            
            //sorting dropDownData...
            if  (dataIdKey != null){
                this.dropDownData.sort(function (a, b){
                    var a1= a[dataIdKey], b1= b[dataIdKey];
                    if(a1== b1) return 0;
                    return a1> b1? 1: -1;
                });
            }
            else {
                this.dropDownData.sort(function (a, b){
                    var a1= a.id, b1=b.id;
                    if(a1== b1) return 0;
                    return a1> b1? 1: -1;
                });
            }
            

            var len = this.dropDownData.length;            
            
            if ((dataListKey != null) && (dataIdKey != null) && (dataLabelKey != null)){ 
	            for (var i = 0; i < len; i++) {
		            if(typeof this.dropDownData[i][dataIdKey] === 'object') {
	                	this.dropDownData[i][dataIdKey] = JSON.stringify(this.dropDownData[i][dataIdKey]);
	                } 
	                if (defaultValue == this.dropDownData[i][dataIdKey]) {
	                	var option = $("<option selected=\"selected\">"+this.dropDownData[i][dataLabelKey]+"</option>");
                		option.attr('value', this.dropDownData[i][dataIdKey]);
                    	select.append(option);
	                } else {
	                	var option = $("<option>"+this.dropDownData[i][dataLabelKey]+"</option>");
                		option.attr('value', this.dropDownData[i][dataIdKey]);
                    	select.append(option);
	                }
	            }
            }
            else{
	            for (var i = 0; i < len; i++) {
	                if (defaultValue == this.dropDownData[i].value) {
	                    var option = $("<option selected=\"selected\" value=\""+this.dropDownData[i].value+"\">"+this.dropDownData[i].label+"</option>");
                	    option.attr('value', this.dropDownData[i].value);
                	    select.append(option);
                        jaffa.form.value(this.labelField, this.dropDownData[i].label);
	                } else {
	                    var option = $("<option value=\""+this.dropDownData[i].value+"\">"+this.dropDownData[i].label+"</option>");
                		option.attr('value', this.dropDownData[i].value);
                    	select.append(option);
	                }
	            }
            }
            ui.append(select);
            jaffa.form.addField(this.field, this.id());

            // Have we been asked to store the label?
            this.labelField = this.getConfig("label-field");
            if (this.labelField != null && this.labelField.trim() != "") {
                ui.append("<input type=\"hidden\" id=\""+this.labelField+"\" />");
                jaffa.form.addField(this.labelField, this.id());
            }

            // Add help content
            this._super();

            // Activating a change trigger will synch
            //  all fields and the managed data
            select.trigger("change");

            // Add some validation
            var mandatory = this.getConfig("mandatory");
            var mandatoryOnSave = this.getConfig("mandatory-on-save");
            if (mandatory === true || mandatoryOnSave === true) {
                // Error message creation
                var validationText = this.getConfig("validation-text") || "This field is mandatory";
                var validationMessage = $("<div class=\"jaffaValidationError\">"+validationText+"</div>");
                ui.append(validationMessage);
                // Error message toggling
                validationMessage.hide();
                function invalid(fieldId, testsFailed) {
                    ui.addClass("error");
                    validationMessage.show();
                }
                function valid(fieldId, testsPassed) {
                    ui.removeClass("error");
                    validationMessage.hide();
                }
                // Notify Jaffa about what we want
                if (mandatory === true) {
                    jaffa.valid.setSubmitRules(this.field, ["required"], valid, invalid);
                }
                if (mandatoryOnSave === true) {
                    jaffa.valid.setSaveRules(this.field, ["required"], valid, invalid);
                }
            }

            // Add our custom classes
            this.applyBranding(ui);
        },

        // If any of the fields we told Jaffa we manage
        //   are changed it will call this.
        change: function(fieldName, isValid) {
            // To avoid double handling, just pay attention to the actual value field
            if (fieldName == this.field) {
                // Update our label field if we have one and only when there is an associated value
				if (this.labelField != null) {
					var selected = jaffa.form.field(fieldName).find(":selected");
					if (selected.val()) {
						jaffa.form.value(this.labelField, selected.text());
					} else {
						jaffa.form.value(this.labelField, "");
					}
                }
                // Complex relations, we want to look like a jQuery automcomplete
                //  to leverage the same methods on the base widget
                var lookupParser = this.getConfig("lookup-parser");
                if (lookupParser != null) {
                    // Because we have a static data source, we only need to map once
                    if (this.dropDownDataMapped == null)  {
                        var thisWidget = this;
                        function mapWrap(item) {
                            return thisWidget.perItemMapping(item);
                        }
                        this.dropDownDataMapped = $.map(this.dropDownData, mapWrap);
                    }
                    // Find our currently selected item and 'select' it
                    var len = this.dropDownDataMapped.length;
                    for (var i = 0; i < len; i++) {
                        var value = this.dropDownDataMapped[i].value;
                        if (value == jaffa.form.value(fieldName)) {
                            // A fake UI element the handler is expecting
                            var ui = {item: this.dropDownDataMapped[i]};
                            this.onSelectItemHandling(null, ui);
                        }
                    }
                }
            }
        },

        // Constructor... any user provided config and the
        //    jQuery container this was called against.
        init: function(config, container) {
            this._super(config, container);
        }
    });

    // *****************************************
    // Let Jaffa know how things hang together. 'jaffaDropDown' is how the
    //   developer can create a widget, eg: $("#id").jaffaDropDown();
    // And the class links to the above variable that is a valid widget
    //   implementation, extending the Jaffa bas widget.
    jaffa.widgets.registerWidget("jaffaDropDown", dropDownClass);
}
$.requestWidgetLoad(DropDownWidgetBuilder);

var DropDownRepeatableWidgetBuilder = function($, jaffa) {
    var dropDownRepeatableClass = jaffa.widgets.listWidget.extend({
        init: function(config, container) {
            this._super(config, container);
            // Make sure 'listWidget' knows how to create each element
            this.childCreator("jaffaDropDown");
        }
    });
    jaffa.widgets.registerWidget("jaffaDropDownRepeatable", dropDownRepeatableClass);
}
$.requestWidgetLoad(DropDownRepeatableWidgetBuilder);
