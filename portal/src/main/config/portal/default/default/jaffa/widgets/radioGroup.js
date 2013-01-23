var RadioGroupWidgetBuilder = function($, jaffa) {
    var radioGroupClass = jaffa.widgets.baseWidget.extend({
        field: null,
        labelField: null,
        oldField: null,
        oldLabelField: null,

        // TODO: Validation
        // TODO: ???
        // TODO: Edge-case / Invalid / various config instantiation

        deleteWidget: function() {
            jaffa.form.ignoreField(this.field);
            jaffa.form.ignoreField(this.labelField);
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
            container.find("input[name=\""+this.oldField+"\"]").attr("name", this.field);
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
            if (label != null) {
                ui.append("<label for=\""+this.field+"\" class=\"widgetLabel\">"+label+"</label>");
            }

            // Control
            var data = this.getConfig("radio-data");
            var defaultValue = this.getConfig("default-value");
            var checkedSomething = false;
            for (var value in data) {
                var radioLabel = data[value] || value;
                var radio = $("<label for=\""+value+"\" class=\"radioLabel\"></label>")
                if (defaultValue == value) {
                    radio.append("<input type=\"radio\" name=\""+this.field+"\" value=\""+value+"\" checked=\"checked\"/>");
                    checkedSomething = true;
                } else {
                    radio.append("<input type=\"radio\" name=\""+this.field+"\" value=\""+value+"\"/>");
                }
                radio.append(radioLabel);
                ui.append(radio);
            }
            // If something MUST be checked, sort it out now
            var allowEmpty = this.getConfig("allow-empty");
            if (allowEmpty !== false) {
                allowEmpty = true;
            }
            if (!allowEmpty && !checkedSomething) {
                ui.find("input[type=\"radio\"]:first").attr("checked", "checked");
            }

            // Some server side data may reset the value anyway, after we synch()
            jaffa.form.addField(this.field, this.id());

            // Have we been asked to store the label?
            this.labelField = this.getConfig("label-field");
            if (this.labelField != null) {
                ui.append("<input type=\"hidden\" id=\""+this.labelField+"\" />");
                jaffa.form.addField(this.labelField, this.id());
            }

            // Add help content
            this._super();
            // Add our custom classes
            this.applyBranding(ui);

            // Finish up by activating a change trigger -
            //  will synch all fields and the managed data
            ui.find("input[name=\""+this.field+"\"]:first").change();
        },

        // If any of the fields we told Jaffa we manage
        //   are changed it will call this.
        change: function(fieldName, isValid) {
            if (fieldName == this.field && this.labelField != null) {
                var value = jaffa.form.value(fieldName);
                if (value != null) {
                    var label = $("label[for=\""+value+"\"]").text();
                    jaffa.form.value(this.labelField, label);
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
    // Let Jaffa know how things hang together. 'jaffaRadioGroup' is how the
    //   developer can create a widget, eg: $("#id").jaffaRadioGroup();
    // And the class links to the above variable that is a valid widget
    //   implementation, extending the Jaffa bas widget.
    jaffa.widgets.registerWidget("jaffaRadioGroup", radioGroupClass);
}
$.requestWidgetLoad(RadioGroupWidgetBuilder);


var RadioGroupRepeatableWidgetBuilder = function($, jaffa) {
    var radioGroupRepeatableClass = jaffa.widgets.listWidget.extend({
        init: function(config, container) {
            this._super(config, container);
            // Make sure 'listWidget' knows how to create each element
            this.childCreator("jaffaRadioGroup");
        }
    });
    jaffa.widgets.registerWidget("jaffaRadioGroupRepeatable", radioGroupRepeatableClass);
}
$.requestWidgetLoad(RadioGroupRepeatableWidgetBuilder);
