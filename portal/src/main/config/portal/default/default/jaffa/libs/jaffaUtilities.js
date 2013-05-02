/********************************************************************/
/** Additions to JQuery interface go here - outside of the function */

// Allow quick access to any attribute starting with 'data-'
// Tested in #util-test-one
if (!$.fn.dataset) {
    $.fn.dataset = function(name, value) {
        return this.attr("data-"+name, value);
    };
}

// Allow quick access to all attribute starting with 'data-'
// Tested in #util-test-two
if (!$.fn.getDatasets) {
    $.fn.getDatasets = function(ff) {
        var atts, d = {}, item, name, value;
        if (this[0]) {
            atts = this[0].attributes;
            for (var i = 0, l = atts.length; i < l; i++) {
                item = atts.item(i);
                name = item.name;
                if (name.substr(0,5) == "data-") {
                    name = name.substr(5);
                    value = item.value;
                    if(!ff || ff(name, value)) {
                        d[name] = value;
                    }
                }
            }
        }
        return d;
    };
}

// Developer use only... this method is for debugging, if you ever push a
//  jQuery object into an alert() box (or similar) and the toString() method
//  is called against, this method lets you control what you will see.
if (/[native code]/.test($.fn.toString)) {
    $.fn.toString = function() {
        return "[jQuery object, size="+this.size()+"]"
    };
}

/**   Additions to JQuery go ABOVE here - outside of the function   */
/********************************************************************/

// One more trivial function added to all Objects for testing thier size
Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

function jaffaUtilies(jaffaObject) { 
    var jaffa = jaffaObject;
    var util = {};

    // Inline function generator, excepts two different syntaxes:
    // 1) Supply parameters with a logic block: "a,b -> a > b ? true : false"
    // 2) Parameter names irrelevant and logic evalutes them
    //      in order (arbitrary limit of 4 parameters): "$1 > $2 ? true : false"
    //      
    // Trivial logic without braces '{' or semicolons ';' are assumed to
    //  be basic statements requiring return and it will be added automatically
    //  to the front.
    // eg: "$1 > $2 ? true : false"      becomes
    //     "return $1 > $2 ? true : false"
    // 
    // Tested in #util-test-three : syntax 1
    // Tested in #util-test-four : syntax 2
    util.fn = function(input) {
        var parameters, logic;
        var re = jaffa.regex.INLINE_FUNCTION_PARAMETERS;
        var re2 = jaffa.regex.INLINE_FUNCTION_COMPLEX;

        // Syntax 1 - {params}->{logic}
        if (re.test(input)) {
            logic = input.replace(re, function(_, match) {
                // Store the parameters
                parameters = match;
                // Then remove them from the Strings, leaving logic behind
                return "";
            });

        // Syntax 2 - {logic} (with numbered parameters)
        } else {
            // Parameters
            parameters = "$1,$2,$3,$4";
            // Pass the logic through untouched
            logic = input;
        }

        // If the logic is a simple statement, add a return block
        if (!re2.test(logic)) {
            logic = "return " + logic;
        }
        return new Function(parameters, logic);
    };

    // ID sequence used for internal task tracking
    // Tested in #util-test-five
    var _idNum = 1;
    util.getIdNum = function() {
        return _idNum++;
    };

    // Get by ID, and wrap in a jQuery object
    // Tested in #util-test-six
    util.getById = function(id) {
        var element = document.getElementById(id);
        if (element) {
            return $(element);
        } else {
            // Something empty
            return $("#_doesNotExist_.-_");
        }
    };

    // Run a custom function across key/value form data, looking for any
    //  'true' return value. Typically used to test if any data has changed
    //  and requires action.
    // Tested in #util-test-seven
    util.any = function(dataMap, func) {
        var key;
        for (key in dataMap) {
            if (func(key, dataMap[key])) {
                return true;
            }
        }
        return false;
    };

    // Wrapper for common usage.
    // Test to see if a variable is in fact a function, and call it if so.
    // Tested in #util-test-eight
    util.callIfFunction = function(func, a, b, c, d, e, f, g, h, i, j) {
        if($.isFunction(func)) {
            try{
                return func(a, b, c, d, e, f, g, h, i, j);
            } catch(ex) {
                if (func.name != undefined) {
                    jaffa.logError("Error calling function '" + func.name + "'");
                }
                jaffa.logError(ex);
            }
        } else {
            // If there is a method of this name (assuming we have a string) call it
            var fn = window[func];
            if (typeof fn === 'function') {
                try {
                    return fn(a, b, c, d, e, f, g, h, i, j);
                } catch(ex) {
                    jaffa.logError("Error calling function '" + func + "'");
                    jaffa.logError(ex);
                }
            } else {
                jaffa.logDebug("Not calling (non)-function '" + func + "'");
            }
        }
        return null;
    };

    // Tracking loading data
    var pendingWork = {};
    var pendingWorkByField = {};
    var firstLoad = false;
    var pendingWorkAllDoneFunc = function() {alert("TODO");};
    util.setFirstLoadCallback = function(callback) {
        if (util.hasWorkPending()) {
            firstLoad = true;
            pendingWorkAllDoneFunc = callback;
        } else {
            callback();
        }
    }
    
    function pendingWorkCleared() {
        if (firstLoad) {
            firstLoad = false;
            pendingWorkAllDoneFunc();
        }
        jaffa.logDebug("Executing => pendingWorkCleared()");
    }

    // Method for checking if all data loading has completed
    util.hasWorkPending = function() {
        return !$.isEmptyObject(pendingWork);
    }

    // Start the tracking of a job and arrange for followup
    function pendingWorkStart(url, field) {
        var workId, pendingWorkDone;
        // Get the next ID available
        workId = util.getIdNum();
        // Store a reference to this URL against the workId
        pendingWork[workId] = url;
        pendingWorkByField[field] = url;
        jaffa.logDebug("Starting Job Tracking URL='"+url+"', ID='"+workId+"'");

        // The tracking function will know how to
        //  flag this particular job as completed
        pendingWorkDone = function() {
            jaffa.logDebug("Job Tracking Complete URL='"+pendingWork[workId]+"', ID='"+workId+"'");
            // Delete the reference to this work
            delete pendingWorkByField[field];
            delete pendingWork[workId];
            // If there is nothing else loading
            if ($.isEmptyObject(pendingWork)) {
                // Fire the callback
                pendingWorkCleared();
            }
        }

        // Return the tracking function
        pendingWorkDone.workId = workId;
        return pendingWorkDone;
    }

    // Testing TODO
    var jsonCache = {};
    util.parseJson = function(cacheIndex, jsonString, success, error, skipCache) {
        // Prefer the cache... unless we've been told not to
        if (skipCache == null) {
            skipCache = false;
        }
        if (skipCache !== false && jsonCache[cacheIndex] != null) {
            util.callIfFunction(success, jsonCache[cacheIndex]);
        }

        // Otherwise parse and cache
        try {
            var jsonData = $.parseJSON(jsonString);
            jsonCache[cacheIndex] = jsonData;
            util.callIfFunction(success, jsonData);
        } catch(e) {
            jaffa.logError("Error parsing JSON! Cache index = '"+cacheIndex+"'");
            error("Error parsing JSON! Cache index = '"+cacheIndex+"', Error: "+ e.message);
        }
    }

    // Testing TODO
    util.getJsonUrl = function(cacheIndex, jsonUrl, success, error, dataType, skipCache, fieldId) {
        // Prefer the cache... unless we've been told not to
        if (skipCache == null) {
            skipCache = false;
        }
        if (skipCache !== false && jsonCache[cacheIndex] != null) {
            util.callIfFunction(success, jsonCache[cacheIndex]);
        }

        if (dataType == null) {
            dataType = "json";
        }
        if (pendingWorkByField[fieldId] == jsonUrl) {
            console.log("THERE IS A PENDING REQUEST FOR " + fieldId + ", USING URL: " + jsonUrl + ", DOING NOTHING");
            return;
        }
        var pendingWorkDone = pendingWorkStart(jsonUrl, fieldId);
        // Go and get the data
        $.ajax({
            url: jsonUrl,
            dataType: dataType,
            contentType: "application/json; charset=utf-8",
            success: function(data) {
                // Cache the response
                jsonCache[cacheIndex] = data;
                // Run a callback if requested
                util.callIfFunction(success, data);
                // Clear job tracking... if required
                if (typeof(pendingWorkDone) !== "undefined") {
                    pendingWorkDone();
                }
            },
            error: function(xhr, status, err) {
                // Run a callback if requested
                util.callIfFunction(error, xhr, status, err);
                // Clear job tracking... if required
                if (typeof(pendingWorkDone) !== "undefined") {
                    pendingWorkDone();
                }
            },
            timeout: 10000
        });
    }

    // A method for request a JSON URL be loaded into the util
    //  cache and a callback be run on completion
    util.loadJsonUrl = function(url, callback) {
        // In the absense of a cacheIndex we'll just use the URL
        util.getJsonUrl(url, url, callback);
    }

    // Look for a callback in configuration with the provided name and execute
    util.callback = function(callbackIndex, a, b, c, d, e, f, g, h, i, j) {
        method = jaffa.config("callback" + callbackIndex);
        // callIfFunction() can safely handle null, but it will log it
        if (method != null) {
            return util.callIfFunction(method, jaffa, a, b, c, d, e, f, g, h, i, j);
        } else {
            return null;
        }
    }
    // We also offer a top-level alias for this
    jaffa.cb = util.callback;

    // Look for a function in configuration with the provided name and execute
    util.runFunction = function(functionIndex, a, b, c, d, e, f, g, h, i, j) {
        method = jaffa.config("function" + functionIndex);
        // callIfFunction() can safely handle null, but it will log it
        if (method != null) {
            return util.callIfFunction(method, jaffa, a, b, c, d, e, f, g, h, i, j);
        } else {
            return null;
        }
    }
    // We also offer a top-level alias for this
    jaffa.fn = util.runFunction;

    return util;
}
