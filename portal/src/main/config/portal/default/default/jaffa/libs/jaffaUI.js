function jaffaUI(jaffaObject) { 
    var jaffa = jaffaObject;
    var ui = {};

    // A common message box utility for on-screen display beyond trivial
    //  alert() calls. Allows a callback event when the user clicks 'OK'
    ui.messageBox = function(messageText, closeCallback, title) {
        var msgBox = $("<div class='box' style='text-align:center;' />");
        msgBox.append($("<span/>"));
        var div = $("<div style='padding-top:1em;' />");
        msgBox.append(div);
        var button = $("<input type='button' value='OK' />");
        div.append(button);

        // Cleanup on close
        msgBox.bind('dialogclose', function(event) {
            if ($.isFunction(closeCallback)) {
                closeCallback();
            }
            msgBox.html("");
            msgBox.remove();
            msgBox = null;
        });

        // Otherwise, if a String was provided it is used as the title
        if (!$.isFunction(closeCallback)) {
            if (closeCallback != undefined &&
                closeCallback != null &&
                closeCallback != "") {
                title = closeCallback;
            }
        }

        // Close if 'OK' is pressed
        button.click(function() {
            msgBox.dialog("close");
        });

        // Build our dialog
        msgBox.dialog({
            title: title || "Message", 
            hide: "blind",
            modal: true, 
            autoOpen: false,
            overlay: {
                opacity: 0.5
            }
        });

        // And display
        msgBox.show();
        msgBox.dialog("open").find("span:first").html(messageText);
    };

    // Now we have the message box available, tell
    //  Jaffa to start using it for user feedback.
    jaffa.defaultConfig["functionUserFeedback"] = function(message) {
        ui.messageBox(message+"", "Alert");
    }
    ui.changeToTabLayout = function(rootElement, headingSelector, swapCallback, id) {
        var li, ul = $("<ul></ul>");
        if(id != null) {
         ul.attr('id', id);
        }
        var heading, hId, hText, hTarget;

        var tabHeadings = rootElement.find(headingSelector);
        if (tabHeadings.size() == 0) {
            jaffa.logError("Tabbed layout failed to load because selector '" + headingSelector + "' returns no elements in document.");
            return null;
        } else {
            jaffa.logInfo("Loading " + tabHeadings.size() +
                " tabs using selector '" + headingSelector + "';");
        }

        // Create an unordered list of tabs for the header
        var errors = false;
        tabHeadings.each(
            function(c, element) {
                heading = $(element);
                hText = heading.text();
                if (hText == null || hText == "") {
                    jaffa.logError("Error creating tabs. No '.text()' for heading "+(c+1)+"!");
                    errors = true;
                }
                hId = heading.attr("jaffa-tab-id");
                if (hId == null) {
                    jaffa.logError("Error creating tabs. No ID for heading '"+hText+"'. IDs should be in the 'jaffa-tab-id' attribute;");
                    errors = true;
                }
                hTarget = jaffa.util.getById(hId);
                if (hTarget == null || hTarget.size() == 0) {
                    jaffa.logError("Error creating tabs. ID '"+hId+"' does not exist for heading '"+hText+"'.");
                    errors = true;
                }
                li = $("<li><a href='#"+hId+"'><span>"+hText+"</span></a></li>");
                ul.append(li);
                heading.remove();
            });
        // Stop here on errors
        if (errors) return null;

        // Navigation links (if styled appropriately)
        var sel;
        rootElement.find(".prev-tab").click(
            function() {
                sel = rootElement.tabs("option", "selected");
                rootElement.tabs("option", "selected", sel - 1);
            });
        rootElement.find(".next-tab").click(
            function() {
                sel = rootElement.tabs("option", "selected");
                rootElement.tabs("option", "selected", sel + 1);
            });

        // Add our new header
        rootElement.prepend(ul);
        if (swapCallback != null) {
            rootElement.tabs({select: swapCallback});
        } else {
            rootElement.tabs();
        }
        return rootElement;
    };

    return ui;
}
