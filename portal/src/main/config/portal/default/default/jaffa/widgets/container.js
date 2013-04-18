var ContainerWidgetBuilder = function($, jaffa) {
    var containerClass = jaffa.widgets.baseWidget.extend({
        field: null,
        oldField: null,
        v2rules: {},
        deleteWidget: function() {
            jaffa.form.ignoreField(this.field);
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
            container.find("div[id=\""+this.oldField+"\"]").attr("id", this.field);
            // Tell Jaffa to ignore the field's this widget used to manage
            jaffa.form.ignoreField(this.oldField);
            //Tell Jaffa to update it's childWidgets
            for(var i =0; i < this.childWidgets.length; i++) {
               var childContainer = this.childWidgets[i].getContainer();
               childContainer.attr("id", childContainer.attr("id").replace(from, to));
 			   this.childWidgets[i].domUpdate(from, to, depth);
			}
            // TODO: Testing
        },
        // Notify Jaffa that field <=> widget relations need to be updated
        //  This is called separately from above to avoid duplicate IDs that
        //   may occur whilst DOM alterations are occuring
        jaffaUpdate: function() {
            // Only synch if an update has effected this widget
            if (this.oldField != null) {
                this._super();
                //jaffa.form.addField(this.field, this.id());
                this.oldField = null;
            }
            // TODO: Validation alterations ?? Doesn't seem to matter
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

                      

            // Control
            var input = null;
            var containerHtml = "<div> </div>";
            if(this.getConfig("container-html")) {
             containerHtml = this.getConfig("container-html");
            }
            input = $(containerHtml);
            input.attr('id',this.field);
            
            //read all the sub elements and add div placeholders to the dom
            var inputDivs = {};                        
            for(var i=0; i<this.getConfig("sub-elements").length; i++) {
            
            	for( key in this.getConfig("sub-elements")[i]) {
             		var subDiv = $("<div id=\""+this.field+"."+i+"\">"+"to be replaced"+"</div>");
             		var options = $.extend({}, this.getConfig("sub-elements")[i][key]);
                if (options["json-data-url"] != null) {
                  options["json-data-url"] = options["json-data-url"].replace("$portalPath", this.getConfig("portalPath"));
                }
             		options["field"]=this.field+options["suffix"];
 			 		inputDivs[this.field+"."+i] = [key,options];
 			 		input.append(subDiv);
				}
			}               
            ui.append(input);
            
            // Now make function call to turn placeholders to jaffa elements. 
            // We'll keep a track of which elements we have for when we need to update their ids.
            this.childWidgets = [];
            for(divId in inputDivs) {
            	var inputElement = $("[id='"+divId+"']")[inputDivs[divId][0]](inputDivs[divId][1]);
            	this.childWidgets.push(inputElement);
            }

            // Add help content
            this._super();
                                                    
            // Activating a change trigger will synch
            //  all fields and the managed data
            input.trigger("change");

            // Complicated validation gets preference
            var v2Rules = this.getConfig("v2Rules");
            if (v2Rules != null) {
                // Error message toggling
                var v2messages = $("<div class=\"jaffaValidationError\"></div>");
                ui.append(v2messages);
                v2messages.hide();
                var rules = this.v2rules;
                function v2invalid(fieldId, testsFailed) {
                    v2messages.html("");
                    var len = testsFailed.length;
                    for (var i = 0; i < len; i++) {
                        var key = testsFailed[i];
                        var message = rules[key].message || "Validation rule '"+key+"' failed.";
                        v2messages.append("<p>"+message+"</p>");
                    }
                    ui.addClass("error");
                    v2messages.show();
                }
                function v2valid(fieldId, testsPassed) {
                    ui.removeClass("error");
                    v2messages.html("");
                    v2messages.hide();
                }
                // Rule integration with Jaffa
                var rulesList = [];
                for (var key in v2Rules) {
                    // Store it for use later
                    this.v2rules[key] = v2Rules[key];
                    rulesList.push(key);
                    // Add the rule to Jaffa
                    jaffa.valid.addNewRule(key, this.v2rules[key].validator, this.v2rules[key].params);
                }
                // Now set these rules against our field
                jaffa.valid.setSubmitRules(this.field, rulesList, v2valid, v2invalid);

            // What about a basic mandatory flag?
            } else {
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
            }

            // Add our custom classes
            this.applyBranding(ui);
        },

        // If any of the fields we told Jaffa we manage
        //   are changed it will call this.
        change: function(fieldName, isValid) {
            if (fieldName == this.field && this.labelField != null) {
                var label = jaffa.form.field(fieldName).find(":selected").text();
                jaffa.form.value(this.labelField, label);
            }
        },

        // Constructor... any user provided config and the
        //    jQuery container this was called against.
        init: function(config, container) {
            this._super(config, container);
            
        }
    });

    // *****************************************
    // Let Jaffa know how things hang together. 'jaffaContainer' is how the
    //   developer can create a widget, eg: $("#id").jaffaContainer();
    // And the class links to the above variable that is a valid widget
    //   implementation, extending the Jaffa bas widget.
    jaffa.widgets.registerWidget("jaffaContainer", containerClass);
}
$.requestWidgetLoad(ContainerWidgetBuilder);

var ContainerRepeatableWidgetBuilder = function($, jaffa) {
    var containerRepeatableClass = jaffa.widgets.listWidget.extend({
        init: function(config, container) {
            this._super(config, container);
            // Make sure 'listWidget' knows how to create each element
            this.childCreator("jaffaContainer");
        }
    });
    jaffa.widgets.registerWidget("jaffaContainerRepeatable", containerRepeatableClass);
}
$.requestWidgetLoad(ContainerRepeatableWidgetBuilder);