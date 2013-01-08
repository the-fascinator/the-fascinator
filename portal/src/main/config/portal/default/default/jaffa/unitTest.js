var testLog = null;
var testLogging = function(result, message) {
    if (testLog == null) {
        testLog = $(".unit-tests");
    }
    var logLine = "<div class='log-row'>";
    if (result) {
        logLine += "<span class='result pass'>Passed</span>";
    } else {
        logLine += "<span class='result fail'>Failed</span>";
    }
    logLine += "<span class='message'>" + message + "</span>";
    logLine += "</div>";
    testLog.prepend(logLine);
};

function unitTestFunction(jaffa) {
    if (jaffa == null || jaffa.util == null) {
        alert("WARNING: The Jaffa object used by the unit tests is null! Unit tests will not run.");
        return;
    }

    // Some variables we constantly reuse
    var testFunction;
    var testObject;
    var testValue, testValue2;

    var unitTestsPassed = 0;
    var unitTestsFailed = 0;
    function testResults(success, message) {
        if (success) {
            unitTestsPassed++;
            testLogging(true, message);
        } else {
            unitTestsFailed++;
            testLogging(false, message);
        }
        if (typeof(fireunit) != "undefined") {
            fireunit.ok(success, message);
        }
    }

    /************************************************/
    /*******   Testing for jaffaUtilities.js   ******/
    /************************************************/
    /** Test 1 Utility $.dataset testing read and write using utility */
    testValue = $("#util-test-one").dataset("test");
    testResults(testValue == "start", "#util-test-one (Read)");
    $("#util-test-one").dataset("test", "finish");
    testValue = $("#util-test-one").dataset("test");
    testResults(testValue == "finish", "#util-test-one (Write)");

    /** Test 2 Utility $.getDatasets testing read of attributes into an associative array/map */
    testObject = $("#util-test-two").getDatasets();
    testResults(Object.size(testObject) == 2, "#util-test-two (response size)");
    testResults(testObject["one"] == "foo", "#util-test-two (1st value)");
    testResults(testObject["two"] == "bar", "#util-test-two (2nd value)");

    /** Test 3 Inline function generation - syntax 1 */
    testFunction = jaffa.util.fn("a,b -> a == b ? true : false");
    testResults(testFunction(1, 2) === false, "#util-test-three (comparison should fail)");
    testResults(testFunction(1, 1) === true, "#util-test-three (comparison should pass)");
    testFunction = jaffa.util.fn("a,b -> a + b");
    testResults(testFunction(1, 2) == 3, "#util-test-three (should be 3)");
    testResults(testFunction(7, 1) == 8, "#util-test-three (should be 8)");
    testFunction = jaffa.util.fn("a,b,c -> if (a == \"add\") {return b + c;} if (a == \"subtract\") {return b - c;} return null;");
    testResults(testFunction("add",      5, 2) == 7, "#util-test-three (should be 7)");
    testResults(testFunction("subtract", 5, 2) == 3, "#util-test-three (should be 3)");
    testResults(testFunction("invalid",  5, 2) == null, "#util-test-three (should be null)");

    /** Test 4 Inline function generation - syntax 2 */
    testFunction = jaffa.util.fn("$1 == $2 ? true : false");
    testResults(testFunction(1, 2) === false, "#util-test-four (comparison should fail)");
    testResults(testFunction(1, 1) === true, "#util-test-four (comparison should pass)");
    testFunction = jaffa.util.fn("$1 + $2");
    testResults(testFunction(1, 2) == 3, "#util-test-four (should be 3)");
    testResults(testFunction(7, 1) == 8, "#util-test-four (should be 8)");
    testFunction = jaffa.util.fn("if ($1 == \"add\") {return $2 + $3;} if ($1 == \"subtract\") {return $2 - $3;} return null;");
    testResults(testFunction("add",      5, 2) == 7, "#util-test-four (should be 7)");
    testResults(testFunction("subtract", 5, 2) == 3, "#util-test-four (should be 3)");
    testResults(testFunction("invalid",  5, 2) == null, "#util-test-four (should be null)");

    /** Test 5 ID Numbers in sequence */
    testValue = jaffa.util.getIdNum();
    testValue2 = jaffa.util.getIdNum();
    testResults((testValue + 1) == testValue2, "#util-test-five (sequence not... sequential)");

    /** Test 6 Get by ID */
    testObject = jaffa.util.getById("util-test-six");
    testValue = testObject.dataset("one");
    testResults(testValue == "foo", "#util-test-six (attribute not found)");
    testValue = testObject.html();
    testResults(testValue == "testValue", "#util-test-six (content not found)");

    /** Test 7 any() values in map match test */
    testFunction = function(_, v) {return v == "";};
    testValue = {one: "foo", two: "bar"};
    testResults(!jaffa.util.any(testValue, testFunction), "#util-test-seven (nothing should be empty)");
    testValue = {one: "foo", two: ""};
    testResults(jaffa.util.any(testValue, testFunction), "#util-test-seven (something should be empty)");
    // Below is a more complicated example from real (although
    //  simplified) usage with non-intuitive variable scope
    var oldDataOutsideLoop = {one: "foo", two: "bar"};
    var oldValue;
    testFunction = function(k, v) {
        // Use key from inside loop on old data
        oldValue = oldDataOutsideLoop[k];
        if (oldValue != v) {
            return true; // Something is different
        }
        return false;
    };
    testValue = {one: "foo", two: "bar"};
    testResults(!jaffa.util.any(testValue, testFunction), "#util-test-seven (nothing changed)");
    testValue = {one: "bar", two: "foo"};
    testResults(jaffa.util.any(testValue, testFunction), "#util-test-seven (values swapped)");

    /** Test 8 callIfFunction() utility */
    testFunction = function(arg) {return arg ? true : false;};
    testResults(jaffa.util.callIfFunction(testFunction, true), "#util-test-eight (Should return true)");
    testResults(!jaffa.util.callIfFunction(testFunction, false), "#util-test-eight (Should return false)");
    // Test except handling - we are going to deliberately suppress user
    //  dialogs here too, but test that our 'fake' dialog did run
    function deliberateFail() {throw "This is a forced failure inside a unit test";}
    var backupDialog = jaffa.userConfig.functionUserFeedback;
    var alertGenerated = false;
    jaffa.userConfig.functionUserFeedback = function(message) {alertGenerated = true;}
    testFunction = deliberateFail;
    testResults(jaffa.util.callIfFunction(deliberateFail) == null, "#util-test-eight (exceptions should return null)");
    testResults(alertGenerated, "#util-test-eight (exceptions should generate alerts)");
    jaffa.userConfig.functionUserFeedback = backupDialog;
    // Test non-function null results
    testFunction = "thisIsNotAFunction";
    testResults(jaffa.util.callIfFunction(testFunction, true) == null, "#util-test-eight (non-functions should return null)");

    /************************************************/
    /*******     Testing for jaffaForm.js      ******/
    /************************************************/
    /** Forms - Text */
    jaffa.form.value("testFieldOne", "Basic text <input>"); // Reset value to avoid browser caching
    testValue = jaffa.form.value("testFieldOne");
    testResults(testValue == "Basic text <input>", "#form-test-text-one (basic text input)");
    testValue = jaffa.form.value("testFieldOne", "Basic text <input> (modified)");
    testResults(testValue == "Basic text <input> (modified)", "#form-test-text-two (basic text input - write)");
    jaffa.form.value("testFieldTwo", "Larger <textarea>"); // Reset value to avoid browser caching
    testValue = jaffa.form.value("testFieldTwo");
    testResults(testValue == "Larger <textarea>", "#form-test-text-three (textarea input)");
    testValue = jaffa.form.value("testFieldTwo", "Larger <textarea> (modified)");
    testResults(testValue == "Larger <textarea> (modified)", "#form-test-text-four (textarea input - write)");
    testValue = jaffa.form.value("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-text-five (form.value() invalid ID)");

    /** Forms - Checkbox */
    jaffa.form.uncheck("testFieldThree"); // Reset value to avoid browser caching
    testValue = jaffa.form.isChecked("testFieldThree");
    testResults(testValue === false, "#form-test-check-one (checkbox input)");
    testValue = jaffa.form.value("testFieldThree");
    testResults(testValue == null, "#form-test-check-two (checkbox value - unchecked)");
    testValue = jaffa.form.check("testFieldThree");
    testResults(testValue == "checkbox1", "#form-test-check-three (checkbox input - toggle return value)");
    testValue = jaffa.form.isChecked("testFieldThree");
    testResults(testValue === true, "#form-test-check-four (checkbox input - toggle persists)");
    testValue = jaffa.form.value("testFieldThree");
    testResults(testValue == "checkbox1", "#form-test-check-five (checkbox value - checked)");
    // Invalid tests
    testValue = jaffa.form.isChecked("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-check-six (form.isChecked() invalid ID)");
    testValue = jaffa.form.check("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-check-seven (form.check() invalid ID)");
    testValue = jaffa.form.uncheck("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-check-eight (form.uncheck() invalid ID)");
    // Convenience mapping - isSelected() => isChecked()
    testValue = jaffa.form.isSelected("testFieldThree");
    testResults(testValue === true, "#form-test-check-nine (form.isSelected() mapping)");
    testValue = jaffa.form.uncheck("testFieldThree");
    testValue2 = jaffa.form.isSelected("testFieldThree");
    testResults(testValue === false && testValue === testValue2,
            "#form-test-check-ten (form.isSelected() mapping - after change)");
    // Convenience mapping - select() => check()
    jaffa.form.select("testFieldThree");
    testValue = jaffa.form.isSelected("testFieldThree");
    testResults(testValue === true, "#form-test-check-eleven (form.select() mapping)");
    // Convenience mapping - value() clear => uncheck()
    jaffa.form.value("testFieldThree", false);
    testValue = jaffa.form.isSelected("testFieldThree");
    testResults(testValue === false, "#form-test-check-twelve (form.value() clear mapping)");
    // Convenience mapping - value() set => check()
    jaffa.form.value("testFieldThree", true);
    testValue = jaffa.form.isSelected("testFieldThree");
    testResults(testValue === true, "#form-test-check-thirteen (form.value() set mapping)");

    /** Forms - Radio */
    jaffa.form.check("testFieldFour", false);
    testValue = jaffa.form.isChecked("testFieldFour");
    testResults(testValue === false, "#form-test-radio-one (radio input - unchecked)");
    // Toggle on
    testValue = jaffa.form.check("testFieldFour", "radio3");
    testValue2 = jaffa.form.value("testFieldFour");
    testResults(testValue == testValue2, "#form-test-radio-two (radio input - checked returns value)");
    testResults(testValue2 == "radio3", "#form-test-radio-three (radio input - checked value)");
    testValue = jaffa.form.isChecked("testFieldFour");
    testResults(testValue === true, "#form-test-radio-four (radio input - checked)");
    // Toggle off
    testValue = jaffa.form.check("testFieldFour", false);
    testValue2 = jaffa.form.isChecked("testFieldFour");
    testResults(testValue === false && testValue === testValue2,
            "#form-test-radio-five (radio input - checked using 'false')");
    testValue = jaffa.form.check("testFieldFour", "radio3");
    testValue2 = jaffa.form.value("testFieldFour");
    testResults(testValue == "radio3" && testValue === testValue2,
            "#form-test-radio-six (radio input - checked again)");
    testValue = jaffa.form.uncheck("testFieldFour");
    testValue2 = jaffa.form.isChecked("testFieldFour");
    testResults(testValue === false && testValue === testValue2,
            "#form-test-radio-seven (radio input - checked using uncheck() )");
    // Toggle invalid value
    testValue = jaffa.form.uncheck("testFieldFour");
    testValue2 = jaffa.form.check("testFieldFour", "valueThatDoesNotExist");
    testResults(testValue === false && testValue2 == null,
            "#form-test-radio-eight (radio input - invalid value check() )");
    // Convenience mapping - isSelected() => isChecked()
    testValue = jaffa.form.isSelected("testFieldFour");
    testResults(testValue === false, "#form-test-radio-nine (form.isSelected() mapping)");
    // Convenience mapping - select() => check()
    testValue = jaffa.form.select("testFieldFour", "radio3");
    testValue2 = jaffa.form.isSelected("testFieldFour");
    testResults(testValue == "radio3" && testValue2 === true,
            "#form-test-radio-ten (form.select() mapping)");
    // Convenience mapping - selected() => value()
    testValue = jaffa.form.selected("testFieldFour");
    testResults(testValue == "radio3", "#form-test-radio-eleven (form.selected() mapping)");
    // Convenience mapping - value() set => check()
    testValue = jaffa.form.value("testFieldFour", "radio2");
    testValue2 = jaffa.form.selected("testFieldFour");
    testResults(testValue == "radio2" && testValue2 == "radio2",
            "#form-test-radio-twelve (form.value() set mapping)");
    // Convenience mapping - value() clear => check()
    testValue = jaffa.form.value("testFieldFour", false);
    testValue2 = jaffa.form.isSelected("testFieldFour");
    testResults(testValue === false && testValue2 === false,
            "#form-test-radio-thirteen (form.value() clear mapping)");
    testValue = jaffa.form.value("testFieldFour", "radio2");

    /** Forms - Select */
    jaffa.form.select("testFieldFive", "select2"); // Reset value to avoid browser caching
    testValue = jaffa.form.selected("testFieldFive");
    testResults(testValue == "select2", "#form-test-select-one (select drop-down)");
    jaffa.form.select("testFieldFive", "select1");
    testValue = jaffa.form.selected("testFieldFive");
    testResults(testValue == "select1", "#form-test-select-two (select drop-down - changed)");
    testValue = jaffa.form.value("testFieldFive");
    testResults(testValue == "select1", "#form-test-select-three (select drop-down) - value() )");
    // Invalid tests
    testValue = jaffa.form.isSelected("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-select-four (form.isSelected() invalid ID)");
    testValue = jaffa.form.selected("testFieldDoesNotExist");
    testResults(testValue == null, "#form-test-select-five (form.selected() invalid ID)");
    testValue = jaffa.form.select("testFieldDoesNotExist", "");
    testResults(testValue == null, "#form-test-select-six (form.select() invalid ID)");
    testValue = jaffa.form.select("testFieldFive", "valueThatDoesNotExist");
    testResults(testValue == null, "#form-test-select-seven (form.select() invalid value)");
    // Convenience mapping - value() set => select()
    testValue = jaffa.form.value("testFieldFive", "select2");
    testValue2 = jaffa.form.value("testFieldFive");
    testResults(testValue == "select2" && testValue2 == "select2",
            "#form-test-select-eight - (value() set mapping)");

    /** Forms - Valid (hidden) input types */
    jaffa.form.value("testFieldHiddenHidden", "This is a hidden input"); // Reset value to avoid browser caching
    testValue = jaffa.form.value("testFieldHiddenHidden");
    testResults(testValue == "This is a hidden input", "#form-test-hidden-one (hidden input)");
    testValue = jaffa.form.value("testFieldHiddenHidden", "This is a hidden input (modified)");
    testResults(testValue == "This is a hidden input (modified)", "#form-test-hidden-two (hidden input - write)");
    testValue = jaffa.form.value("testFieldHiddenPassword"); // We'll avoid a full write test here, just make sure it exists
    testResults(testValue == "This is a hidden password", "#form-test-hidden-three (password)");

    /** Forms - Invalid (hidden) input types */
    testValue = jaffa.form.value("testFieldHiddenButton");
    testResults(testValue == null, "#form-test-invalid-one (button input)");
    testValue = jaffa.form.value("testFieldHiddenFile");
    testResults(testValue == null, "#form-test-invalid-two (file input)");
    testValue = jaffa.form.value("testFieldHiddenImage");
    testResults(testValue == null, "#form-test-invalid-three (image input)");
    testValue = jaffa.form.value("testFieldHiddenReset");
    testResults(testValue == null, "#form-test-invalid-four (reset input)");
    testValue = jaffa.form.value("testFieldHiddenSubmit");
    testResults(testValue == null, "#form-test-invalid-five (submit input)");

    /** Forms - Adding fields after init() */
    testValue = jaffa.form.value("testFieldAddLater");
    testResults(testValue == null, "#form-test-adding-one (not visible yet)");
    testValue = jaffa.form.addField("testFieldAddLater");
    testResults(testValue === true, "#form-test-adding-two (added)");
    testValue = jaffa.form.value("testFieldAddLater");
    testResults(testValue == "Hidden value", "#form-test-adding-three (now visible)");
    // Add by name - radio group
    testValue = jaffa.form.value("testFieldAddLaterRadio");
    testResults(testValue == null, "#form-test-adding-four (radio not visible yet)");
    testValue = jaffa.form.addField("testFieldAddLaterRadio");
    testResults(testValue === true, "#form-test-adding-five (radio added)");
    testValue = jaffa.form.check("testFieldAddLaterRadio", "hiddenRadio2");
    testValue2 = jaffa.form.value("testFieldAddLaterRadio");
    testResults(testValue == testValue2, "#form-test-adding-six (radio added - write)");
    testResults(testValue2 == "hiddenRadio2", "#form-test-adding-seven (radio added - read)");
    // Exception testing
    testValue = jaffa.form.addField("testFieldAddLater");
    testResults(testValue === false, "#form-test-adding-seven (already added)");
    testValue = jaffa.form.addField("testFieldDoesNotExist");
    testResults(testValue === false, "#form-test-adding-eight (does not exist)");

    /** Forms - Removing a field from Jaffa */
    testValue = jaffa.form.value("testFieldAddLater");
    testResults(testValue == "Hidden value", "#form-test-remove-one (currently visible)");
    testValue = jaffa.form.ignoreField("testFieldAddLater");
    testResults(testValue === true, "#form-test-remove-two (removing)");
    testValue = jaffa.form.value("testFieldAddLater");
    testResults(testValue == null, "#form-test-remove-three (no longer visible)");

    /** Forms - Basic isEmpty() testing */
    testValue = jaffa.form.value("testFieldHiddenHidden");
    testValue2 = jaffa.form.isEmpty("testFieldHiddenHidden");
    testResults(testValue == "This is a hidden input (modified)" && testValue2 === false, "#form-test-empty-one (not empty)");
    testValue = jaffa.form.value("testFieldHiddenHidden", "");
    testValue2 = jaffa.form.isEmpty("testFieldHiddenHidden");
    testResults(testValue == "" && testValue2 === true, "#form-test-empty-two (empty)");

    /** Demo on 'Data' tab for dynamically managing data */
    var textRunOnce = true;
    $("#data-manage-text").click(function() {
        if (textRunOnce) {
            var newInput = $("<input type=\"text\" id=\"formText\" />");
            $("#data-manage-text-span").html(newInput);
            jaffa.form.addField("formText");
            textRunOnce = false;
        }
    });
    var textareaRunOnce = true;
    $("#data-manage-textarea").click(function() {
        if (textareaRunOnce) {
            var newInput = $("<textarea id=\"formTextarea\"></textarea>");
            $("#data-manage-textarea-span").html(newInput);
            jaffa.form.addField("formTextarea");
            textareaRunOnce = false;
        }
    });
    var dateRunOnce = true;
    $("#data-manage-date").click(function() {
        if (dateRunOnce) {
            var newInput = $("<input type=\"text\" id=\"formDate\" />");
            $("#data-manage-date-span").html(newInput);
            jaffa.form.addField("formDate");
            newInput.datepicker({dateFormat: 'yy-mm-dd' });
            dateRunOnce = false;
        }
    });
    var japaneseRunOnce = true;
    $("#data-manage-utf8").click(function() {
        if (japaneseRunOnce) {
            var newInput = $("<textarea id=\"utf-8-日本語\" rows=\"10\" cols=\"15\"></textarea>");
            $("#data-manage-textarea-utf8").html(newInput);
            jaffa.form.addField("utf-8-日本語");
            japaneseRunOnce = false;
        }
    });

    /** Demo on 'Data' tab for synch and view server data */
    $("#data-manage-text").click(function() {
        if (textRunOnce) {
            var newInput = $("<input type=\"text\" id=\"formText\" />");
            $("#data-manage-text-span").html(newInput);
            jaffa.form.addField("formText");
            textRunOnce = false;
        }
    });

    /************************************************/
    /*******      Testing for jaffaUI.js       ******/
    /************************************************/
    /** Message box demo */
    function msgBoxCallback() {
        $("#msg-box-test-output").val("This text was written by a callback function. You can provide this to execute after the user closes a message dialog.")
    }
    $("#msg-box-test-btn").click(function() {
        jaffa.ui.messageBox($("#msg-box-test-text").val(), msgBoxCallback, "Demonstration Message Box");
    });
    $("#msg-box-test-reset").click(function() {
        $("#msg-box-test-text").val("This text will display in a message box.");
        $("#msg-box-test-output").val("This text will change after the dialog closes.");
    });

    /************************************************/
    /*******    Testing finished - feedback    ******/
    /************************************************/
    if (unitTestsFailed > 0) {
        testLogging(false, "ERROR: " + unitTestsFailed + " test(s) failed");
        alert("ERROR: " + unitTestsFailed + " Unit test(s) failed");
    } else {
        testLogging(true, "Complete... All " + unitTestsPassed + " tests passed");
    }
    if (typeof(fireunit) != "undefined") {
        fireunit.testDone();
    }
}
