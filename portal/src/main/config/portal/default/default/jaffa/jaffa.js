// Check for dependencies
if (typeof jQuery == "undefined") {
    alert("ERROR: Jaffa requires jQuery to operate");
} else {
    if (typeof jQuery.ui == "undefined") {
        alert("ERROR: Jaffa requires jQuery UI to operate");
    }
}
if (typeof v2 == "undefined") {
    alert("ERROR: Jaffa requires Validatious to operate");
}

// // IE won't let us refer to jaffa.widgets when we'd like to during instantion
//  this variable will be used to hold loader functions we'll execute later.
// Each widget just needs to call $.registerJaffaWidget(loaderFunction)
var _jaffaWidgetFunctions = [];
if (!$.requestWidgetLoad) {
    $.requestWidgetLoad = function(method) {
        _jaffaWidgetFunctions.push(method);
    };
}

function jaffaFactory(newConfig) { 
    var jaffa = {};

    /********************************************************************/
    /** Configuration handling - user config first, then defaults */
    jaffa.config = function(field) {
        if (field in jaffa.userConfig && jaffa.userConfig[field] != null) {
            return jaffa.userConfig[field];
        } else {
            return jaffa.defaultConfig[field];
        }
    }
    jaffa.userConfig = newConfig;
    // Anything in config that will be referred to by other config should go here
    jaffa.defaultConfig = {
        pathBaseUrl: ""
    }
    // Real config goes here
    jaffa.defaultConfig = {
        errorMessageStartupFailed: "Jaffa failed during startup! Please check console logs.",
        debuggingEnabled: true,
        widgetScripts: [],

        /** For ease of maintenance, all callback functions below here */
        callbackStartupComplete: null,
        // Data
        callbackUnmanagedServerData: null,
        callbackNewManagedServerData: null,
        callbackOnDataChanged: null,
        // Submission / Save
        callbackPreSave: null,
        callbackPostSave: null,
        callbackPreSubmission: null,
        callbackPostSubmission: null,

        /** For ease of maintenance, all standard functions below here */
        functionLog: function(type, message) {
            message = "(JAFFA): " + message;
            if (typeof(console) != "undefined") {
                console[type](message);
            }
        },
        functionSaveData: function() {
            // TODO
            alert("functionSaveData()");
        },
        functionSubmitData: function() {
            // TODO
            alert("functionSubmitData()");
        },
        functionUserFeedback: function(message) {
            // Utils will override this assuming it instantiates without error.
            // This basic alert is only used for very early errors.
            alert(message);
        },

        /** For ease of maintenance, all default CSS selectors */
        selectorFormFields: ".jaffa-field"
    }

    /********************************************************************/
    /** Wrappers for contextual usage of logging */
    jaffa.alertUser = function(message) {
        jaffa.config("functionUserFeedback")(message);
    }
    jaffa.logInfo = function(message) {
        jaffa.config("functionLog")("info", message);
    }
    jaffa.logError = function(message) {
        jaffa.config("functionLog")("error", message);
        jaffa.alertUser(message);
    }
    jaffa.logWarning = function(message) {
        jaffa.config("functionLog")("warn", message);
    }
    jaffa.logDebug = function(message) {
        if (jaffa.config("debuggingEnabled")) {
            jaffa.config("functionLog")("debug", message);
        }
    }
    // Generally not used, but allows external access to default logging.
    //  Overriding log functions can call this something like a 'super()'
    //  method in traditional OOP languages.
    jaffa.log = function(type, message) {
        jaffa.defaultConfig["functionLog"](type, message);
    }

    /********************************************************************/
    /** Javascript Library/Plugin loader */
    var loadingLibraries = [];
    var completeLibraries = [];
    var errorLibraries = [];
    function loadScript(scriptName) {
        loadingLibraries.push(scriptName);
        $.getScript(scriptName)
        .done(function(data, textStatus, response) {
            jaffa.logDebug("Successfully loaded library: " + scriptName);
            libraryLoaded(scriptName);
        })
        .fail(function(response, text) {
            jaffa.logWarning("Failed to load external Javascript Library! '" + scriptName + "'");
            jaffa.logWarning("ERROR Status: '" + text + "'");
            jaffa.logWarning("ERROR Code: " + response.status);
            libraryError(scriptName);
        });  
    }
    function libraryError(scriptName) {
        errorLibraries.push(scriptName);
        libraryLoadTest();
    }
    function libraryLoaded(scriptName) {
        completeLibraries.push(scriptName);
        libraryLoadTest();
    }
    var failedToLoad = false;
    var loadCallsCompleted = false;
    function libraryLoadTest() {
        // Don't run this until all the load calls have run
        if (loadCallsCompleted === false) {
            return;
        }
        // Finish on errors... the flag makes sure we don't keep failing
        if (errorLibraries.length > 0 && failedToLoad === false) {
            failedToLoad = true;
            librariesFailed();
            return;
        }
        // Or finish when fully loaded
        if (loadingLibraries.length == completeLibraries.length) {
            finishJaffa();
        }
    }
    function librariesFailed() {
        jaffa.alertUser(jaffa.config("errorMessageStartupFailed"));
    }

    /********************************************************************/
    /** Start instantiation - Plus callback after Libraries load */
    jaffa.logDebug("Core instantiation beginning");

    // Instanstiate core components; order matters
    jaffa.regex   = {}
    jaffa.regex   = jaffaRegexes(jaffa);
    jaffa.util    = jaffaUtilies(jaffa);
    jaffa.ui      = jaffaUI(jaffa);
    jaffa.valid   = jaffaValidation(jaffa);
    jaffa.serverData = {}; // <= Data structure used in .form
    jaffa.form    = jaffaForm(jaffa);
    jaffa.widgets = jaffaWidgets(jaffa);
    jaffa.logDebug("Core instantiation completed successfully");

    // Widgets/plugin loading... execution will return to finishJaffa()
    var installedWidgets = jaffa.config("widgetScripts");
    var numWidgets = installedWidgets.length;
    for (var i = 0; i < numWidgets; i++) {
        loadScript(installedWidgets[i]);
    }

    // Flag that we've completed load calls and check if they finished. Note
    //  that each call will also run this callback, but just in case they all
    //  completed super-fast we are going to call it here after the flag has
    //  been set.
    loadCallsCompleted = true;
    libraryLoadTest();

    function finishJaffa() {
        jaffa.logDebug("Executing => finishJaffa()");

        // Widget loading... now that all the JS has been parsed, instantiate
        //  each one and make sure they can access jQuery and Jaffa
        var numWidgets = _jaffaWidgetFunctions.length;
        for (var i = 0; i < numWidgets; i++) {
            jaffa.util.callIfFunction(_jaffaWidgetFunctions[i], $, jaffa);
        }

        // Data loading
        var dataSource = jaffa.config("urlDataSource");
        if (dataSource != null && dataSource != "") {
            // Get the JSON Data and run this callback
            jaffa.util.loadJsonUrl(dataSource, function(data) {
                for (var field in data) {
                    jaffa.serverData[field] = data[field];
                }
            });
        // No data source
        } else {
            jaffa.logWarning("No Data Source specified in configuration!");
        }

        // After all data loads... (if any)
        jaffa.util.setFirstLoadCallback(firstPendingLoadCleared);
    }

    /********************************************************************/
    /** Any/all remote data is loaded - Callback after instantiaion */
    function firstPendingLoadCleared() {
        // Is this the last data we are waiting on?
        if (!jaffa.util.hasWorkPending()) {
            // Keep memory data and form values in synch
            jaffa.form.synch(true);

            // User callback?
            var startupCallback = jaffa.config("callbackStartupComplete");
            if (startupCallback != null) {
                // Pass the jaffa object through. The user's callback may have
                //  trouble accessing their object in IE because of variable
                //  scoping in the callback
                startupCallback(jaffa);
            }
        }
    }

    return jaffa;
}
