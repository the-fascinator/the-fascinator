/********************************************************************/
/** This top-level implementation of Class inheritance is sourced   */
/** from here: http://ejohn.org/blog/simple-javascript-inheritance/ */
/**                                                                 */
/** It remains here along with it's own licensing (MIT), separate   */
/**   to that of Jaffa (GPLv2), and has simply been reformatted and */
/**   renamed.                                                      */

/* Simple JavaScript Inheritance
 * By John Resig http://ejohn.org/
 * MIT Licensed.
 */
// Inspired by base2 and Prototype
(function() {
    var initializing = false;
    var fnTest = /xyz/.test(function() {xyz;}) ? /\b_super\b/ : /.*/;

    // The base Class implementation (does nothing)
    this.JaffaClass = function() {};

    // Create a new Class that inherits from this class
    JaffaClass.extend = function(prop) {
        var _super = this.prototype;

        // Instantiate a base class (but only create the instance,
        // don't run the init constructor)
        initializing = true;
        var prototype = new this();
        initializing = false;

        // Copy the properties over onto the new prototype
        for (var name in prop) {
            // Check if we're overwriting an existing function
            prototype[name] = typeof prop[name] == "function" &&
            typeof _super[name] == "function" && fnTest.test(prop[name]) ?
            (function(name, fn){
                return function() {
                    var tmp = this._super;

                    // Add a new ._super() method that is the same method
                    // but on the super-class
                    this._super = _super[name];

                    // The method only need to be bound temporarily, so we
                    // remove it when we're done executing
                    var ret = fn.apply(this, arguments);       
                    this._super = tmp;

                    return ret;
                };
            })(name, prop[name]) :
            prop[name];
        }

        // The dummy class constructor
        function JaffaClass() {
            // All construction is actually done in the init method
            if ( !initializing && this.init )
                this.init.apply(this, arguments);
        }

        // Populate our constructed prototype object
        JaffaClass.prototype = prototype;
        // Enforce the constructor to be what we expect
        JaffaClass.prototype.constructor = JaffaClass;
        // And make this class extendable
        JaffaClass.extend = arguments.callee;

        return JaffaClass;
    };
})();

/** END of  top-level implementation of Class inheritance (and MIT) */
/********************************************************************/

// A not so straight forward adaptation taken from here. Very useful starting point though
// http://stackoverflow.com/questions/36183/replacing-the-nth-instance-of-a-regex-match-in-javascript
// 
// Very useful in manipulating lists during a reorder when the contents may
//  be at any arbitrary depth in a nested hierarchy. This method search for
//  how many lists deep the element is and the then replaces the *.N.* portion
//  of the ID, ONLY if it is match the criteria.
// ie. We aren't replace the Nth match, we are replace the Nth POSSIBLE match
//     if the numbers line up.

String.prototype.domUpdate = function(from, to, depth) {
  var parts, tempParts, toggle, depthCount;
  var regex = new RegExp("(\\.[0-9]+\\.)");

  // If there's no match, bail
  if (this.search(regex) === -1) {
    return this;
  }

  // Break up the String
  parts = this.split(regex);

  // Now walk through it. Every 2nd element is a potential candidate
  // but it must match at both depth and content to be valid
  toggle = false;
  depthCount = 1;
  tempParts = [];
  for (var k = 0; k < parts.length; k++) {
    // List section
    if (toggle) {
      toggle = false;
      if (depthCount == depth && from == parts[k]) {
        tempParts.push(to);
      } else {
        tempParts.push(parts[k]);
      }
      depthCount++;

    // Normal section
    } else {
      tempParts.push(parts[k]);
      toggle = true;
    }
  }

  return tempParts.join("");
};

function jaffaWidgets(jaffaObject) {
    var jaffa = jaffaObject;
    var widgets = {};

    //*******************************************
    // Widget class definition - extended by widget implementations
    widgets.baseWidget = JaffaClass.extend({
        config: {},
        container: {},
        jsonData: null,
        jsonLoader: null,
        responseParser: {},
        responseParserFields: {},
        responseParserOutputs: {},

        test: function() {
          return this.responseParserFields;
        },
        getJsonData: function() {
            return this.jsonData;
        },

        applyBranding: function(element) {
            var branding = this.getConfig("class-list");
            if (branding == null) return;

            for (var selector in branding) {
                var classList = branding[selector];
                var len = classList.length;
                for (var i = 0; i < len; i++) {
                    if (selector == "") {
                        element.addClass(classList[i]);
                    } else {
                        element.find(selector).addClass(classList[i]);
                    }
                }
            }
        },

        // Widgets should typically overwrite these
        buildUi: function() {
            // What will the help content look like?
            var helpContent = this.getConfig("help-content");
            if (helpContent == null) {
                var helpText = this.getConfig("help-text");
                if (helpText != null) {
                    helpContent = $("<div class=\"ui-state-highlight jaffaHelpContent\"><span class=\"ui-icon ui-icon-info\"></span> "+helpText+"</div>");
                }
            } else {
                helpContent = $(helpContent);
            }

            // Now add some toggles for it
            if (helpContent != null) {
                var container = this.getContainer();
                container.append();

                // Where to attach the toggle
                var helpToggleRelative = this.getConfig("help-toggle-relative");
                if (helpToggleRelative !== true && helpToggleRelative !== false) {
                    helpToggleRelative = true;
                }
                var helpToggleAppender = this.getConfig("help-toggle-after");
                if (helpToggleAppender == null) {
                    helpToggleAppender = "h1, h2, h3, h4, h5, h6";
                }
                // And what will it look like
                var helpToggleHtml = this.getConfig("help-toggle-html");
                var helpToggle = null;
                if (helpToggleHtml == null) {
                    helpToggleHtml = "<button class=\"jaffaHelpToggle\"></button>";
                    helpToggle = $(helpToggleHtml);
                    helpToggle.button({icons: {primary:'ui-icon-help'}});
                } else {
                    helpToggle = $(helpToggleHtml);
                }

                // Setup click logic
                helpContent.hide();
                helpToggle.click(function(){helpContent.toggle('fast');});

                // And attach to our container
                var target = null;
                if (helpToggleRelative) {
                    target = container.find(helpToggleAppender).first();
                } else {
                    target = $(helpToggleAppender).first();
                }
                if (target.size() == 0) {
                    // So we've been given some help content, but knowledge of
                    //  where to put it... just drop it after the widget
                    container.append(helpToggle);
                    container.append(helpContent);

                } else {
                    target.append(helpToggle);
                    target.after(helpContent);
                }
            }
        },

        // A small utility for traversing an object, propety by property
        dataGetOnJsonPath: function(jsonData, path) {
            var result = jsonData;
            var len = path.length;
            for (var i = 0; i < len; i++) {
                result = result[path[i]];
                if (result == null) {
                    return null;
                }
            }
            return result;
        },

        // Once a selection has been made, work out if any additional outputs
        //  were required. This confirms to the jQuery UI 'api', but Jaffa will
        //  leverage it as well.
        onSelectItemHandling: function(event, ui) {
            for (var field in ui.item) {
                if (field != "label" && field != "value") {
                    var value = ui.item[field];
                    var found = false;
                    // First, is the target a Jaffa field?
                    var target = jaffa.form.field(field);
                    if (target != null) {
                        jaffa.form.value(field, value);
                        found = true;
                    }

                    // Second, is it in our document as a selector?
                    target = $(field);
                    if (target != null) {
                        target.html(value);
                        found = true;
                    }

                    // Hmm, log something
                    if (!found) {
                        jaffa.logWarning("Unable to send output to '"+field+"', could not find anything meaningful with that value.");
                    }

                }
            }
        },

        // Will process a data structure according to configuration and generate
        //  item by item entries. If called inside autocomplete handlers, you'll
        //  screw handlers unles you wrap in a closure to fix 'this'
        perItemMapping: function(item) {
            // Find every field we care about
            var myFields = {};
            for (var thisField in this.responseParserFields) {
                var target = this.responseParserFields[thisField];
                var value = this.dataGetOnJsonPath(item, target);
                if (value != null) {
                    myFields[thisField] = value;
                }
            }

            // And then build all the outputs requested by config
            var returnObject = {};
            for (var key in this.responseParserOutputs) {
                var template = this.responseParserOutputs[key];
                returnObject[key] = this.valueByTemplate(template, myFields);
            }
            return returnObject;
        },

        // Process a template by replacing fields
        //  where their placeholders are found
        valueByTemplate: function(template, fields) {
            var result = template;
            for (var field in fields) {
                var value = fields[field];
                var target = "${"+field+"}";
                result = result.replace(target, value);
            }
            return result
        },

        // Called directly by Jaffa rather then buildUI(), just in case there
        //  is any JSON data to load prior to the UI getting built.
        startLoading: function() {
            var jsonDataUrl = this.getConfig("json-data-url");
            var topLevelId = this.getConfig("data-top-level-id");
            if (topLevelId != null)
            	jsonDataUrl += topLevelId;
            // We have data to load
            if (jsonDataUrl != null) {
                var ui = this.getContainer();
                this.jsonLoader = $("<span style='color:green;'> [loading&nbsp;please&nbsp;wait...] </span>");
                ui.append(this.jsonLoader);

                // Check if something like 'jsonp' was requested
                var jsonDataType = this.getConfig("json-data-type") || "json";

                // URL is used twice, the first is actually just a cache index
                var thisWidget = this;
                jaffa.util.getJsonUrl(jsonDataUrl, jsonDataUrl,
                    function(data) {thisWidget.dataCallback(data);},
                    function(xhr, status, err) {thisWidget.dataFailure(xhr, status, err);},
                    jsonDataType);

            // Otherwise just get on with it.
            } else {
                this.buildUi();
            }
        },
        dataCallback: function(data) {
            if (this.jsonLoader != null) {
                this.jsonLoader.remove();
            }
            this.jsonData = data;
            this.buildUi();
        },
        dataFailure: function(xhr, status, err) {
            if (this.jsonLoader != null) {
                this.jsonLoader.remove();
            }
            this.getContainer().append("<span style='color:red;'> [Error&nbsp;loading&nbsp;data... widget '" + this.id() + "' did not load.] </span>");
            jaffa.logError("Failed loading JSON data for widget '" + this.id() + "', Error: " + err);
        },

        change: function(fieldName, isValid) {},
        domUpdate: function(from, to, depth) {
            this.config.id = this.config.id.domUpdate(from, to, depth);
        },
        jaffaUpdate: function() {},

        // Gasic getters
        getConfig: function(property) {
            if (property != null) {
                return this.config[property];
            } else {
                return this.config;
            }
        },
        getContainer: function() {
            return this.container;
        },
        id: function() {
            return this.config.id;
        },

        // Constructor - always call as 'this._super(config, container)'
        //   before doing anything else
        init: function(newConfig, jQContainer) {
            // NOTE: Properties that are not functions MUST be reset here
            //       as each call to extend() copies them as is.
            this.config = newConfig;
            this.container = jQContainer;
            this.responseParser = newConfig["lookup-parser"] || {};
            this.responseParserFields = this.responseParser["fields"] || {
                "label": "label",
                "value": "value"
            };
            this.responseParserOutputs = this.responseParser["outputs"] || {};
            if (!("label" in this.responseParserOutputs)) {
                this.responseParserOutputs["label"] = "${label}";
            }
            if (!("value" in this.responseParserOutputs)) {
                this.responseParserOutputs["value"] = "${value}";
            }
            jaffa.logDebug("Widget '"+this.id()+"' starting.");
        },

        // TODO : Testing
        // This method is an adaptation of Douglas Crockford's 'swiss inheritance'.
        //  => http://www.crockford.com/javascript/inheritance.html
        // Let's a 'class' deliberately choose to inherit a specific method
        //    under a new name.
        inherit: function(parentClass, parentMethod, childMethod) {
            this.prototype[childMethod] = parentClass.prototype[parentMethod];
        }
    });

    //*******************************************
    // A Widget class for lists of widgets ie. repeatable complex fields
    widgets.listWidget = widgets.baseWidget.extend({
        // Children
        myChildren: {},
        childMethod: null,
        // Field storage
        baseField: null,
        subFields: null,
        field: null,

        // Child getters
        child: function(id) {
            return this.myChildren[id];
        },
        childByIndex: function(index) {
            for (var id in this.myChildren) {
                // this.baseField.length prevents finding the parent of a List in List
                var found = id.indexOf("."+index+".", this.baseField.length);
                if (found != -1) {
                    return this.myChildren[id];
                }
            }
            return null;
        },
        childCount: function(index) {
            return Object.size(this.children());
        },
        children: function() {
            return this.myChildren;
        },

        // Child Management (CM)
        // CM 1) Called by extending classes so we know how to instantiate children
        childCreator: function(methodName) {
            this.childMethod = methodName;
        },
        // CM 2) Logical 'top' function for creating children
        addChild: function(forceNew) {
            // Basic validity check
            var field = this.subFields["field"];
            if (field == null) {
                jaffa.logError("No field name provided for widget '"+this.id()+"'. This is mandatory!");
                return null;
            }

            // Find out if we already have data
            var counter = this.childCount() + 1;
            var subField = this.baseField + "." + counter + "." + field;

            // TODO minSize instead of assuming counter != 1
            // TODO maxSize constraint

            function hasNoUnmanagedSubfield(baseName, checkList) {
                for (var key in checkList) {
                    var testName = baseName + "." + counter + "." + checkList[key];
                    if (jaffa.form.unmanaged[testName] != null) {
                        return false;
                    }
                }
                return true;
            }

            // Reasons to create... unmanaged data, minimum rows... or forced
            var minSize = this.getConfig("min-size");
            if (minSize == null) {
              minSize = 1;
            }
            if (hasNoUnmanagedSubfield(this.baseField, this.subFields) && (counter > minSize) && !forceNew) {
                return false;
            }
            jaffa.logDebug("Widget '"+this.id()+"' adding child "+counter);

            // Add wrapping UI elements
            var childContainer = this.buildChild(counter, subField);

            // Config for the child
            var childConfig = $.extend({}, this.getConfig("child-config"));
            for (var key in childConfig) {
                // If the key + value match something from 'sub-fields' we
                // need to convert to a full field name built from the base
                var unmappedField = childConfig[key];
                if (key in this.subFields && this.subFields[key] == unmappedField) {
                    childConfig[key] = this.baseField + "." + counter + "." + unmappedField;
                }
            }

            // Finally instantiate the child inside our wrappings
            var result = this.createChildWidget(childContainer, childConfig);
            if (result == null) {
                jaffa.logError("Failed to instantiate child widget in list '"+this.id()+"'. Aborting creation.");
                return false;
            }

            return true;
        },
        // CM 3) Child UI wrapping
        buildChild: function(counter, subField) {
            var ui = this.getContainer();
            var thisWidget = this;

            // Setup a container for the child and surrounding UI elements (item)
            var itemContainer = $("<div id=\""+subField+"ContainerItem\" class=\"jaffaList\"></div>");

            // Sorting & Numbering
            var disableSorting = this.getConfig("disable-sorting");
            if (disableSorting !== true) {
                itemContainer.append("<div class=\"jaffaSorting ui-icon ui-icon-arrowthick-2-n-s\" style=\"float: left; cursor: pointer;\"></div>");
            }
            var disableItemNumber = this.getConfig("disable-numbers");
            if (disableItemNumber === true) {
                itemContainer.append("<div class=\"jaffaItemNumber\" style=\"float: left; display: none;\">"+counter+"</div>");
            } else {
                itemContainer.append("<div class=\"jaffaItemNumber\" style=\"float: left;\">"+counter+"</div>");
            }

            // Widget holder
            var childContainer = $("<div id=\""+subField+"Container\" class=\"jaffaListItem\" style=\"float: left;\"></div>");
            itemContainer.append(childContainer);

            // 'Delete Item' element
            var deleteItemText = this.getConfig("delete-item-text");
            if (deleteItemText == null) {
                deleteItemText = "delete";
            }
            var deleteItemHtml = this.getConfig("delete-item-html");
            if (deleteItemHtml == null) {
                deleteItemHtml = "<span style=\"cursor: pointer;\">&laquo;"+deleteItemText+"&raquo;</span>";
            }
            var delItem = $("<div class=\"jaffaDeleteItem\" style=\"float: left;\">"+deleteItemHtml+"</div>");
            itemContainer.append(delItem);
            delItem.on("click", function() {
                thisWidget.clickDelete(this);
            });

            // Since we float them all left, we want something to keep them well behaved
            itemContainer.append("<div style=\"clear: both;\"></div>");
            this.applyBranding(itemContainer);

            // Add our item conainer to the list,
            // just before the 'Add Item' control
            itemContainer.insertBefore(ui.find(".jaffaAddItem:last"));

            if (disableSorting !== true) {
                ui.sortable({
                    items: "> .jaffaList",
                    handle: ".jaffaSorting",
                    placeholder: "ui-state-highlight",
                    forcePlaceholderSize: true,
                    update: function(event, ui) {
                        // We need to get back from this callback
                        //  into a 'this' context for the object.
                        thisWidget.reorder(event, ui);
                    }
                });
            }
            return childContainer;
        },
        // CM 4) Takes care of object creation and management for children
        createChildWidget: function(childContainer, config) {
            if (this.childMethod == null) {
                // TODO: Testing
                jaffa.logError("No childCreator() method provided by widget '"+this.id()+"'. This is mandatory!");
                return null;
            }
            var newChild = childContainer[this.childMethod](config);
            this.myChildren[newChild.id()] = newChild;
            return newChild;
        },

        // List UI
        buildUi: function() {
            this.baseField = this.getConfig("base-field");
            this.subFields = this.getConfig("sub-fields");

            var thisWidget = this;
            var ui = this.getContainer();
            ui.html("");

            // Basic validity check
            this.field = this.subFields["field"];
            if (this.field == null) {
                // TODO: Testing
                jaffa.logError("No field name provided for widget '"+this.id()+"'. This is mandatory!");
                return null;
            }

            // Heading
            this.heading = this.getConfig("heading");
            if (this.heading != null) {
                ui.append(this.heading);
            }

            // 'Add Item' element
            var addItemText = this.getConfig("add-item-text");
            if (addItemText == null) {
                addItemText = "Add Item";
            }
            var addItemHtml = this.getConfig("add-item-html");
            if (addItemHtml == null) {
                addItemHtml = "<button>"+addItemText+"</button>";
            }
            var addItem = $("<div class=\"jaffaAddItem\"></div>");
            addItemHtml = $(addItemHtml);
            addItem.append(addItemHtml);
            ui.append(addItem);
            addItemHtml.on("click", function() {
                thisWidget.clickAdd();
            });

            // Create as many children as we have data for (or just row 1)
            var result = this.addChild(false);
            while (result === true) {
                result = this.addChild(false);
            }
            if (this.childCount() == 0 && this.getConfig("min-size") != 0) {
                alert("Children for '" + this.id() + "' " + this.childCount());
            }

            // Help/Validation etc
            this._super();
            this.applyBranding(ui);
            this.updateUi();

            return this;
        },

        updateUi: function() {
            var ui = this.getContainer();
            var numChildren = this.childCount();

            // Show / Hide message if list is empty
            if (numChildren == 0) {
                var emptyMessage = this.getConfig("empty-message");
                if (emptyMessage == null) {
                    emptyMessage = "This list is empty. Click below to add an entry.";
                }
                var emptyContainer = $("<div class=\"jaffaEmptyList\">"+emptyMessage+"</div>");
                emptyContainer.insertBefore(ui.find(".jaffaAddItem"));
            } else {
                ui.find("> .jaffaEmptyList").remove();
            }

            // Show / Hide 'Delete' if min limit reached
            var minSize = this.getConfig("min-size");
            if (minSize == null) {
                minSize = 1;
            }
            var maxSize = this.getConfig("max-size") || -1;
            if (numChildren <= minSize) {
                // Very, very strange. ONLY when using List in List, adding timings to the hide()/show()
                // transitions will cause them to not fire at all if the elements start in a hidden tab.
                ui.find("> .jaffaList > .jaffaDeleteItem").hide();
            } else {
                ui.find("> .jaffaList > .jaffaDeleteItem").show();
            }

            // Show / Hide 'Add Item' if max limit reached
            if (maxSize != -1 && numChildren >= maxSize) {
                ui.children(".jaffaAddItem").hide();
            } else {
                ui.children(".jaffaAddItem").show();
            }
        },

        deleteWidget: function() {
            for (var widgetId in this.myChildren) {
                this.myChildren[widgetId].deleteWidget();
            }
        },

        domUpdate: function(from, to, depth) {
            for (var widgetId in this.myChildren) {
                this.myChildren[widgetId].domUpdate(from, to, depth);
            }
            // Fix baseField. If children are added after the update
            //  they will have the wrong field otherwise.
            this.baseField = this.baseField.domUpdate(from, to, depth);
        },

        jaffaUpdate: function() {
            for (var widgetId in this.myChildren) {
                this.myChildren[widgetId].jaffaUpdate();
            }
        },

        // Callbacks
        reorder: function(event, ui) {
            var newChildren = {};

            // Find all the children via the DOM for a new order
            var container = this.getContainer();
            var thisWidget = this;
            container.children(".jaffaList").each(function(i, element) {
                element = $(element);
                var rowCount = element.find(".jaffaItemNumber:first");
                var newIndex = i + 1;
                var oldIndex = rowCount.text();
                var oldChild = thisWidget.childByIndex(oldIndex);
                if (oldChild == null) {
                    // TODO: Testing
                    jaffa.logError("Error sorting widget '"+thisWidget.id()+"'. Index '"+oldIndex+"' failed to return a child element!");
                    return;
                }

                // Change the index value and move it
                if (oldIndex != newIndex) {
                    // How deep are we?
                    var depth = element.parents(".jaffaList").size() + 1;
                    // Step 1) Update 'myChildren' indexes
                    var oldId = oldChild.id();
                    var newId = oldId.domUpdate("."+oldIndex+".", "."+newIndex+".", depth);
                    newChildren[newId] = oldChild;
                    // Step 2) Row numbers updated
                    rowCount.html(newIndex);
                    // Step 3) Update the containers in DOM
                    element.attr("id", newId+"Item");
                    element.find(".jaffaListItem").attr("id", newId);
                    // Step 4) Let the widget know that sub-fields will change
                    oldChild.domUpdate("."+oldIndex+".", "."+newIndex+".", depth);
                // Just move
                } else {
                    newChildren[oldChild.id()] = oldChild;
                }
            });
            
            // Step 5) Finally, after the DOM has settled down
            //  again, tell Jaffa about the changes
            this.myChildren = newChildren;
            for (var widgetId in this.myChildren) {
                this.myChildren[widgetId].jaffaUpdate();
            }

            this.updateUi();
        },
        clickAdd: function() {
            // Use 'true' to force a new child
            this.addChild(true)
            this.updateUi();
        },
        clickDelete: function(element) {
            element = $(element);
            var index = element.parent().find(".jaffaItemNumber:first").text();
            var widgetToDelete = this.childByIndex(index);
            var itemContainer = widgetToDelete.getContainer().parent();
            widgetToDelete.deleteWidget();
            itemContainer.remove();
            this.reorder();
        },

        init: function(newConfig, jQContainer) {
            this._super(newConfig, jQContainer);
            // NOTE: Properties that are not functions MUST be reset here as
            //       each call to extend() copies them as is.
            this.myChildren = {};
            this.childMethod = null;
            this.baseField = null;
            this.subFields = null;
            this.field = null;
        }
    });

    //*******************************************
    // Register how a widget starts and integrate it with Jaffa
    widgets.registerWidget = function(methodName, classObject) {
        if (!$.fn[methodName]) {
            $.fn[methodName] = function(config) {
                // ID: User config over what is present in the DOM
                var id  = this.attr("id");
                if (id == null) {
                    jaffa.logError("Error instantiating widget '"+methodName+"'. The provided container has no ID to use!");
                    return null;
                }
                // Just in case some dodgy user data was provided
                config.id = id;

                // Create and register
                var newWidget = new classObject(config, this);
                jaffa.form.addWidget(newWidget);
                return newWidget;
            }
        }
    };

    return widgets;
}