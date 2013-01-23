function jaffaValidation(jaffaObject) { 
    var jaffa = jaffaObject;
    var valid = {};

//TODO: Unit tests

    // 'Private' storage
    var baseV2fields = {};
    var baseV2tests = {};
    var basicRules = {};
    var rulesInContext = {};

    //***********************************
    // Getters from Validatious with caching
    function getV2field(fieldId) {
        if (fieldId in baseV2fields) return baseV2fields[fieldId];
        var field = v2.$f(fieldId);
        if (field != null) {
            baseV2fields[fieldId] = field;
            return field;
        }
        return null;
    }
    function getV2test(testName) {
        if (testName in baseV2tests) return baseV2tests[testName];
        var test = v2.$v(testName);
        if (test != null) {
            baseV2tests[testName] = test;
            return test;
        }
        return null;
    }

    //***********************************
    // Add methods
    valid.addNewRule = function(ruleName, v2test, params) {
        // Ensure the test exists (will also add to cache)
        var test = getV2test(v2test);
        if (test == null) {
            jaffa.logError("Unable to find Validatious test named '"+v2test+"'");
            return false;
        }

        // Store the instruction for this test
        basicRules[ruleName] = {
            "test": v2test,
            "params": params
        };
        return true;
    }
    valid.setSaveRules = function(fieldId, ruleList, successCallback, failureCallback) {
        valid.setRules("save", fieldId, ruleList, successCallback, failureCallback);
    }
    valid.setSubmitRules = function(fieldId, ruleList, successCallback, failureCallback) {
        valid.setRules("submit", fieldId, ruleList, successCallback, failureCallback);
    }
    valid.setRules = function(context, fieldId, ruleList, successCallback, failureCallback) {
        // Ensure these rules are 'known'
        var len = ruleList.length;
        for (var i = 0; i < len; i++) {
            if (!(ruleList[i] in basicRules)) {
                // This rules hasn't been seen before, check if
                //  is a trivial Validatious rule instead.
                var test = getV2test(ruleList[i]);
                // TODO: Unit test for this rejection
                if (test == null) {
                    jaffa.logError("Unable to find rule named '"+ruleList[i]+"' for validation.");
                    return false;
                }
            }
        }

        var field = getV2field(fieldId);
        if (field == null) {
            jaffa.logError("Unable to find field '"+fieldId+"' to attach validation to.");
            return false;
        }

        if (rulesInContext[fieldId] == null) {
            rulesInContext[fieldId] = {};
        }
        rulesInContext[fieldId][context] = {
            "rules": ruleList,
            "success": successCallback,
            "failure": failureCallback
        };
        return true;
    }

    //***********************************
    // Remove methods
    valid.removeOnSave = function(fieldId) {
        return valid.remove("save", fieldId);
    }
    valid.removeOnSubmit = function(fieldId) {
        return valid.remove("submit", fieldId);
    }
    valid.remove = function(context, fieldId) {
        if (fieldId in rulesInContext) {
            var contexts = rulesInContext[fieldId];
            if (context in contexts) {
                delete contexts[context];
                return true;
            }
        }

        jaffa.logWarning("Errors removing validation context '"+context+"' for field '"+fieldId+"'... not found.");
        return false;
    };

    //***********************************
    // Testing calls
    //***********************************

    // Runs a specific test for a field, returning true false after
    //  calling the appropriate callback (if required)
    function runTest(ruleName, fieldId) {
        var field = getV2field(fieldId);
        var test = null;

        //  Is this a trivial Validatious rule?
        if (!(ruleName in basicRules)) {
            test = getV2test(ruleName);
            if (test == null || field == null) {
                return false;
            } else {
                return test.test(field);
            }
        }

        // Or a rule provided to Jaffa with parameters
        var rule = basicRules[ruleName];
        var testName = rule.test;
        var params = rule.params;
        test = getV2test(testName);
        if (test == null || field == null) {
            return false;
        } else {
            return test.test(field, params);
        }
    }

    function testField(context, fieldId) {
        // No tests for this field
        if (!(fieldId in rulesInContext)) return true;
        // No tests for this field (in context)
        var contexts = rulesInContext[fieldId];
        if (!(context in contexts)) return true;

        // Basic variable prep
        var result = true;
        var failures = [];
        var validationInfo = contexts[context];
        var rulesList = validationInfo.rules;
        var len = rulesList.length;

        // Run each test in a loop
        for (var i = 0; i < len ; i++) {
            var ruleName = rulesList[i];
            var thisResult = runTest(ruleName, fieldId);
            if (!thisResult) {
                failures.push(ruleName);
            }
            result = thisResult && result;
        }

        // Callbacks (if any)
        if (!result) {
            var failureCb = validationInfo.failure;
            if (failureCb != null) {
                jaffa.util.callIfFunction(failureCb, fieldId, failures);
            }
        } else {
            var successCb = validationInfo.success;
            if (successCb != null) {
                jaffa.util.callIfFunction(successCb, fieldId, rulesList);
            }
        }

        return result;
    };

    valid.test = function(context, fieldId) {
        var result = true;

        // Testing just one field, or a few fields
        if (fieldId != null) {
            // It might be an array
            if ($.isArray(fieldId)) {
                var len = fieldId.length;
                for (var i = 0; i < len; i++) {
                    result = testField(context, fieldId[i]) && result;
                }
                return result;
            // or just a single field
            } else {
                return testField(context, fieldId);
            }
        }

        // Testing every field, also we may send a list of failures via callback
        var thisResult = true;
        var failures = [];
        for (fieldId in rulesInContext) {
            thisResult = testField(context, fieldId);
            result = thisResult && result;

            // Keep track of anything that fails
            if (!thisResult) {
                failures.push(fieldId);
            }
        }

        // Failure callback (if any)
        if (!result) {
            jaffa.cb("ValidationFailure", context, failures);
        }
        return result;
    };

    //***********************************
    // Some convenience methods for common top level occurances
    valid.okToSave = function() {
        return valid.test("save");
    }
    valid.okToSubmit = function() {
        return valid.test("submit");
    }

    return valid;
}
