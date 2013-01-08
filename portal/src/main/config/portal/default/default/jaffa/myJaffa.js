var jaffa;
var enableProfiling = false;

// Trivial wrappers for on-screen stuff
function padInt(i) {
    if (i < 10) return '0' + i;
    return i;
}
function now() {
    var d = new Date();
    return padInt(d.getHours()) + ":" + padInt(d.getMinutes()) + ":" + padInt(d.getSeconds());
}
// We are just going to take a stab at this working, as per suggestion here:
// http://stackoverflow.com/questions/1359761/sorting-a-json-object-in-javascript
function sortedJson(object) {
    var sorted = {}, key, sortedKeys = [];

    // Get all the keys
    for (key in object) {
        if (object.hasOwnProperty(key)) {
            sortedKeys.push(key);
        }
    }

    // Sort them, this method is from Jaffa's utils and
    //  accounts for odd digits in field names
    sortedKeys.alphanumSort();

    // Build our new object in sorted order... we are just hoping that the
    //  browser will keep this, even though there is nothing requiring it to
    for (key = 0; key < sortedKeys.length; key++) {
        sorted[sortedKeys[key]] = object[sortedKeys[key]];
    }

    // Turn it to JSON + escape some HTML characters that will break <pre> tags
    var rawString = JSON.stringify(sorted, null, 4);
    return rawString.replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// Will be called from index.html after all the tabs finish loading
function startJaffa() {
    // If there is a console, start profiling
    if (enableProfiling && typeof(console) != "undefined") {
        console.profile();
    }

    // ***************************************
    // Custom logging function
    var logging = null;
    var debugLogging = function(type, message) {
        if (logging == null) {
            logging = $(".jaffa-logs");
        }
        var logLine = "<div class='log-row'>";
        logLine += "<span class='time'>" + now() + "</span>";
        logLine += "<span class='level'>" + type + "</span>";
        logLine += "<span class='message'>" + message + "</span>";
        logLine += "</div>";
        logging.prepend(logLine);
        // Send them to Jaffa's standard log as well, the null test
        //  avoids some issues in IE due to startup timing.
        if (jaffa != null) {
            jaffa.log(type, message);
        }
    };

    // ***************************************
    // Callbacks we use on the 'Data' tab for demonstrating how data is used
    function onDataChanged(jaffa, fieldName, isValid) {
        if (fieldName == "aaaFirstFieldDemo") {
            jaffa.form.save();
        }
    }
    function preSave(jaffa, unmanagedData, passedValidation, serverData) {
        // We need to force save even though there is invalid data
        return true;
    }
    var autoSaveDiv = $("#autoSaveMessage");
    function saveOverride(jaffa, unmanagedData, passedValidation, serverData) {
        var jsonString = sortedJson(serverData);
        var element = $("#showMeTheJson");
        element.html("");
        element.append("<h4>Form Data to save</h4><br/>");
        if (unmanagedData) {
            element.append("<div class=\"alert alert-error\"><i class=\"icon-warning-sign\"></i> At least some of this data is '<b>unmanaged</b>'.</div>");
        }
        if (!passedValidation) {
            element.append("<div class=\"alert alert-error\"><i class=\"icon-warning-sign\"></i> One or more fields failed validation checks.</div>");
        }
        element.append("<pre class=\"prettyprint lang-js\">"+prettyPrintOne(jsonString)+"</pre>");
        // If validation failed (typically we wouldn't reach this
        //  point, but this is a demo) we need to don't do this.
        if (passedValidation) {
            autoSaveDiv.addClass("alert");
            autoSaveDiv.removeClass("alert-error");
            autoSaveDiv.addClass("alert-success");
            autoSaveDiv.html("Auto save successfull! Well, the fake one anyway... :)");
            autoSaveDiv.show();
            autoSaveDiv.delay(3000).hide('slow');
        }
    }
    // Callback when unmanaged data is first found
    function unmanagedData(jaffa, fieldName) {
        $("#unmanagedDataAlerts").append("<p id=\"unmanaged-"+fieldName+"\"><i class=\"icon-warning-sign\"></i> Unmanaged field '" + fieldName + "' in server data!</p>");
    }
    // Callback for data that is now managed and wasn't previously
    function newmanagedData(jaffa, fieldName) {
        jaffa.util.getById("unmanaged-"+fieldName).remove();
        $("#managedDataAlerts").append("<p><i class=\"icon-info-sign\"></i> Field '" + fieldName + "' is now managed.</p>");
    }

    // ***************************************
    // Logic specific to the validation tab and demo'ing there
    function validationDemoJaffa(jaffa) {
        // A callback for validation failures
        function validationFail(fieldId, testsFailed) {
            if (fieldId == "requiredText") {
                if ($.inArray("required", testsFailed) != -1) {
                    jaffa.ui.messageBox("The 'Really Important' field is mandatory!");
                }
                if ($.inArray("myCustomFooBar", testsFailed) != -1) {
                    jaffa.ui.messageBox("For submission, the 'Really Important' field must be 'FooBar'!");
                }
            }
            if (fieldId == "requiredEmail") {
                if ($.inArray("required", testsFailed) != -1) {
                    jaffa.ui.messageBox("An email address is mandatory!");
                }
                if ($.inArray("email", testsFailed) != -1) {
                    jaffa.ui.messageBox("Your email address is malformed!");
                }
            }
        }

        // A custom Validatious validator
        v2.Validator.add({
            name: "myCustom",
            fn: function(field, value, params) {
                // Field value must equal first parameter (if it has one)
                if (params[0] == null || value == params[0]) {
                    return true;
                }
                return false;
            }
        });
        // Add some rules using the above validator
        jaffa.valid.addNewRule("myCustomFooBar", "myCustom", ["FooBar"]);
        jaffa.valid.addNewRule("myCustomTest",   "myCustom", ["Test"]);

        // Button clicks on this tab should be scoped to only the fields being demo'd
        //  and only on this tab, so we don't leave them active all the time.
        var fields = ["requiredText", "requiredEmail"];
        $("#validateTabSaveNowDemo").click(function() {
            jaffa.valid.setSaveRules("requiredText",  ["required"], null, validationFail);
            jaffa.valid.setSaveRules("requiredEmail",  ["required", "email"], null, validationFail);

            if (jaffa.valid.test("save", fields)) {
                jaffa.ui.messageBox("Form is valid for save... Yay!<br/>Try submission as well!");
            }

            jaffa.valid.removeOnSave("requiredText");
            jaffa.valid.removeOnSave("requiredEmail");
        });

        $("#validateTabSubmitNowDemo").click(function() {
            jaffa.valid.setSubmitRules("requiredText",  ["required", "myCustomFooBar"], null, validationFail);
            jaffa.valid.setSubmitRules("requiredEmail",  ["required", "email"], null, validationFail);

            if (jaffa.valid.test("submit", fields)) {
                jaffa.ui.messageBox("Form is valid for submission... Yay!");
            }

            jaffa.valid.removeOnSubmit("requiredText");
            jaffa.valid.removeOnSubmit("requiredEmail");
        });

        // A callback for validation failures on change()
        $("#validOnChangeDemoMsg").hide();
        function validationOnChangeFail(fieldId, testsFailed) {
            $("#validOnChangeDemo").addClass("error");
            $("#validOnChangeDemoMsg").show();
        }
        function validationOnChangePass(fieldId, testsPassed) {
            $("#validOnChangeDemo").removeClass("error");
            $("#validOnChangeDemoMsg").hide();
        }
        jaffa.valid.setSubmitRules("requiredTest", ["required", "myCustomTest"], validationOnChangePass, validationOnChangeFail);

        // A custom Validatious OR validator
        v2.Validator.add({
            name: "myCustomOr",
            acceptEmpty: false,
            fn: function(field, value, params) {
                // Get the values from Jaffa
                var orField1 = jaffa.form.value(params[0]);
                var orField2 = jaffa.form.value(params[1]);
                return orField1 == params[2] || orField2 == params[2];
            }
        });
        jaffa.valid.addNewRule("myCustomOrDemo", "myCustomOr", ["requiredTestOr1", "requiredTestOr2", "Test"]);

        // A callback for validation failures on change()
        $("#validOnChangeOrDemoMsg").hide();
        function validationOnChangeOrFail(fieldId, testsFailed) {
            $("#validOnChangeOrDemo").addClass("error");
            $("#validOnChangeOrDemoMsg").show();
        }
        function validationOnChangeOrPass(fieldId, testsPassed) {
            $("#validOnChangeOrDemo").removeClass("error");
            $("#validOnChangeOrDemoMsg").hide();
        }
        jaffa.valid.setSubmitRules("requiredTestOr1", ["myCustomOrDemo"], validationOnChangeOrPass, validationOnChangeOrFail);
        jaffa.valid.setSubmitRules("requiredTestOr2", ["myCustomOrDemo"], validationOnChangeOrPass, validationOnChangeOrFail);
    }

    // A 'global' callback for all top-level validation failures. Individual
    //  callbacks are responsible for updating UI features tied to controls.
    //  This callback is for the entire form. It will not fire if validation is
    //  called against a subset of fields.
    function formValidationFail(jaffa, context, failedFields) {
        // Styling
        autoSaveDiv.addClass("alert");
        autoSaveDiv.removeClass("alert-success");
        autoSaveDiv.addClass("alert-error");
        autoSaveDiv.html("Auto save failed! Some form fields did not validate:");

        // Add a list of all failures
        var len = failedFields.length;
        var list = $("<ul></ul>");
        for (var i = 0; i < len; i++) {
            // Make them clickable
            var link = $("<button for=\""+failedFields[i]+"\" class=\"jaffaHelpToggle\"></button>");
            link.button({icons: {primary:'ui-icon-circle-arrow-e'}});
            link.click(function() {jaffa.form.focus($(this).attr("for"));});
            // And add to list
            var item = $("<li>Field: '"+failedFields[i]+"' </li>");
            item.append(link);
            list.append(item);
        }
        // Add the list and show message
        autoSaveDiv.append(list);
        autoSaveDiv.show();
    }

    // ***************************************
    //   Callback for post-instantiation
    // Make sure the instantiated jaffa object is used as a parameter.
    //  The callback will work, but on IE the unit tests will not run without
    //  access to Jaffa's internals because variable scoping is causing issues.
    function jaffaLoaded(jaffa) {
        debugLogging("info", "User callback method executing, starting unit testing");
        runWidgetDemos(jaffa); // This function(s) lives on the widgets tab... pretty big
        validationDemoJaffa(jaffa);

        // ***************************************
        // Setup on-screen tabs based on our divs, then start unit testing
        var saveOnTabSwap = function(event, ui) {
            jaffa.form.save();
        }
        var myTabs = jaffa.ui.changeToTabLayout($(".tab-layout"), "legend.group-label", saveOnTabSwap);
        if (myTabs != null) {
            // Run unit testing
            unitTestFunction(jaffa);
            // Demo stuff
            var tabTestCallback = function(event, ui) {
                jaffa.ui.messageBox("This callback is fired by swapping tabs.");
            }
            jaffa.ui.changeToTabLayout($(".my-tab-class"), "h3", tabTestCallback);
            jaffa.ui.changeToTabLayout($(".my-tab-class2"), "h3");
        }

        // ***************************************
        // If there is a console, finish profiling
        if (enableProfiling && typeof(console) != "undefined") {
            console.profileEnd();

            // And if FireUnit is installed we'll add
            //  content to the unit testing tab
            if (typeof(fireunit) != "undefined") {
                // Wait a moment or we won't get all the profiling data, just a
                //  subset. The solution here came from one of the FireUnit devs
                //  http://www.softwareishard.com/blog/firebug/fireunit-testing-in-the-firebug-world/
                var holder = $("#fireUnitProfiling");
                holder.html("<p><img src=\"loading-progress.gif\" /> Delaying 5s to let the console collate profiling data...</p>");

                function fireUnitFailed(message) {
                    holder.html("<p>ERROR: Failed to access profiling data from FireUnit!</p>")
                    holder.append("<p>"+message+"</p>")
                    jaffa.logWarning("Error accessing profile data from FireUnit!");
                }

                setTimeout(function() {
                    var data = null;
                    try {
                        data = fireunit.getProfile();
                    } catch(ex) {
                        fireUnitFailed(ex);
                        return;
                    }
                    if (data == null) {
                        fireUnitFailed("Data returned NULL");
                        return;
                    }
                    var totalCalls = data.calls;
                    var totalTime = data.time;
                    var nonJQueryStats = $("<p></p>");
                    var dataRows = $("<table id=\"jqGrid\"></table><div id=\"jqGridPager\"></div>");
                    holder.html("<p>Profiling details: <b>Calls</b>: "+totalCalls+", <b>Time</b>: "+totalTime+"ms (includes jQuery)</p>")
                    holder.append(nonJQueryStats)
                    holder.append(dataRows)

                    // Set up the jqGrid table
                    $("#jqGrid").jqGrid({
                        datatype: "local",
                        colNames: ["Method Name", "# Calls", "% of Time", "Own Time", "Avg Own", "Time", "Avg Time", "Min Time", "Max Time", "File Name"],
                        colModel: [
                            {name:"name",     index:"name",     width:150, align:"left",  sortable:true},
                            {name:"calls",    index:"calls",    width:80,  align:"right", sortable:true, sorttype:"int"},
                            {name:"percent",  index:"percent",  width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"ownTime",  index:"ownTime",  width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"avgOwn",   index:"avgOwn",   width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"time",     index:"time",     width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"avgTime",  index:"avgTime",  width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"minTime",  index:"minTime",  width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"maxTime",  index:"maxTime",  width:80,  align:"right", sortable:true, sorttype:"float"},
                            {name:"fileName", index:"fileName", width:200, align:"left",  sortable:true}
                        ],
                        caption: "Profiling Data... all times in microseconds (ms)",
                        height: "100%",
                        pager: "#jqGridPager",
                        sortname: "ownTime",
                        sortorder: "desc"
                    });

                    // Populate our data
                    for (var i in data.data) {
                        var fileName = data.data[i]["fileName"];
                        // Remove jQuery
                        if (fileName.indexOf("jquery") == 0) {
                            totalCalls -= data.data[i]["calls"];
                            totalTime -= data.data[i]["ownTime"];
                        // Log everything else
                        } else {
                            var row = data.data[i];
                            row.avgOwn = (row.ownTime / row.calls).toFixed(3);
                            $("#jqGrid").jqGrid('addRowData', i+1, row); 
                        }
                    }
                    nonJQueryStats.html("<p><b>Excluding jQuery</b>: <b>Calls</b>: "+totalCalls+", <b>Time</b>: "+totalTime+"ms</p>");

                    // Debugging, if you want to see the raw output
                    //var json = JSON.stringify(data, null, 4);
                    //holder.append("<pre>" + json + "</pre>");
                }, 5000);
            } else {
                $("#fireUnitProfiling").html("... but you aren't. Your browser does appear to have a console though. Check the profiler...");
            }
        } else {
            if (enableProfiling) {
                $("#fireUnitProfiling").html("... but your browser doesn't appear to have a console (or it isn't enabled).");
            }
        }
    }

    // ***************************************
    // Jaffa Instantiation
    jaffa = jaffaFactory({
            // Logging
            debuggingEnabled: true,
            functionLog: debugLogging,
            // Form info
            formSelector: ".jaffa-form",
            // Data
            urlDataSource: "data/formData.json",
            // Callbacks
            callbackNewManagedServerData: newmanagedData,
            callbackOnDataChanged: onDataChanged,
            callbackPreSave: preSave,
            callbackStartupComplete: jaffaLoaded,
            callbackUnmanagedServerData: unmanagedData,
            callbackValidationFailure: formValidationFail,
            // Overide an internal function
            functionSaveData: saveOverride,
            // Widgets
            widgetScripts: [
                "widgets/text.js",
                "widgets/dropDown.js",
                "widgets/radioGroup.js"
            ]
        });
}
