var jQ = jQuery;

var rvtFactory = function(jQ) {
    // Our return object... This 'class'
    var rvt = {};
    // Used to cache content loads for nodes
    var pages = {};
    // Default manifest entry for 'home page'
    var homePage;
    // Manifest nodes
    var nodes;

    // Default configuration
    rvt.contentBaseUrl = "";
    rvt.serverRequestTemplate = null;
    rvt.contentSelector = "#content";
    rvt.titleSelector = "#contents-title";
    rvt.tocSelector = "#toc .toc";
    rvt.contentScrollToTop = function(){jQ(window).scrollTop(0);};
    rvt.contentLoadedCallback = null;
    rvt.loadingMessage = "Loading. Please wait...";

    // ====================================
    // Overridable method for when to 'ignore' hashes. An 'ignored' hash
    // will lost most of the same event/callbacks, but it will not query
    // ther server for data.
    // ====================================
    rvt.ignoreTest = function (hash) {
        return false;
    }

    // ====================================
    // This method can be overridden. It will parse and process content
    // just before it is added to the page. Intended for link fixing and similar
    // ====================================
    rvt.contentParser = function(content) {
        var href, anchors = {};

        // Get the second hash, the first is for the package
        var a = window.location.hash.split("#",2).slice(1)[0];
        // Make sure the base URI ends with a '/'
        baseUri = rvt.contentBaseUrl;
        if (baseUri[baseUri.length-1] !== "/") baseUri += "/";
        // If there are paths in the hash, add them to the URI
        baseUri += a.split("/").slice(0,-1).join("/");
        // Again, make sure the base URI ends with a '/'
        if (baseUri[baseUri.length-1] !== "/") baseUri += "/";

        // ====================================
        // Update paths found in the provided data with the baseUri.
        function updateUri(node, attrName) {
            var attribute = node.attr(attrName);
            // Make sure we ignore absolute paths
            function testIfLocalUri(uri) {
                if (!uri) {return false;}
                // If it starts with '/' or contains '://'
                return uri.search(/^\/|\:\/\//g) === -1;
            }
            // If relative, add baseUri in front of the attribute
            if (testIfLocalUri(a)){
                node.attr(attrName, baseUri + attribute);
            }
        }

        // Find every anchor element (we want the links)
        content.find("a").each(function(c, a) {
            // Get href attribute
            a = jQ(a); href = a.attr("href");
            if (href) {
                // Does it have a hash link already?
                if (href.substring(0,1) === "#") {
                    // Push our hash in front
                    a.attr("href", "#" + hash + href);
                    // And record the anchor it targeted
                    anchors[href.substring(1)] = hash + href;
                } else {
                    // Resolve this to work inside the package
                    if (href = getHPath(href)) {
                        a.attr("href", "#" + href);
                    }
                }
            }
        });

        // Now for each anchor element (we want the targets this time)
        content.find("a").each(function(c, a) {
            var id, name;
            a = jQ(a);
            id = a.attr("id"); name = a.attr("name");
            // Did we see it during the 'fix' above?
            if (anchors[id]) {
                // Modify the anchor name to match
                a.attr("name", anchors[id]);
            // Check if it has a name match too
            } else if (anchors[name]) {
                // Also update
                a.attr("name", anchors[name]);
            }
        });

        // Update anything with a 'src' attribute
        content.find("*[src]").each(function(c, node){
            node = jQ(node);
            updateUri(node, "src");
        });
        // Or 'object's have a more complicated set of path possibilities
        content.find("object").each(function(c, node){
            node = jQ(node);
            updateUri(node, "data");
            updateUri(node, "codebase");
            node.find("param[name='movie'],param[name='url'],param[name='media']").each(
                function(c, node){
                    node = jQ(node);
                    updateUri(node, "value");
                }
            );
        });
    };

    // ====================================
    // Update the on screen content in the package viewer.
    // ====================================
    rvt.updateContent = function (content) {
        var contentDiv = jQ(rvt.contentSelector);
        // Replace all old content
        rvt.contentParser(content);
        contentDiv.html(content);
        // Remove the loading message if we loaded visible content
        if (content.is(":visible")) {
            try {
                contentDiv.find("#loadingMessage").remove();
                if (rvt.contentLoadedCallback) {
                    rvt.contentLoadedCallback(rvt);
                }
            } catch(e) {
                alert("Error calling loadedContentCallback: " + e)
            }
        // Otherwise make it visible
        } else {
            try {
                content.show();
            } catch(e) {
                alert("Error in loading content: "+e);
            }
        }
        window.location.hash = window.location.hash;
        // Refresh hash so that the browser will go to any anchor locations.
        // This is on a timer so that Anotar has loaded.
        /*
        if (window.location.hash) {
            // It does cause some screen flicker as a side effect :(
            setTimeout(
                function() {
                    window.location.hash = window.location.hash;
                    rvt.contentScrollToTop();
                }, 500);
        }
        */
    };

    // ====================================
    // Update the package title in the package viewer.
    // ====================================
    rvt.setTitle = function(title) {
        jQ(rvt.titleSelector).html(title);
    };

    // ====================================
    // This method is the entry point for 'starting' the system.
    // ====================================
    rvt.getManifestJson = function(jsonFilename) {
        // If the caller didn't provide a name for
        // the json file, use a default value.
        if (!jsonFilename) {
            jsonFilename = "manifest.json";
        }
        // processManifestJson() is our callback
        jQ.get(jsonFilename, {}, processManifestJson, "json");
    };

    // ====================================
    // After the package manifest has been parsed,
    // this method will be called to display a TOC.
    // ====================================
    rvt.displayTOC = function(nodes) {
        // ====================================
        // Take the list of manifest nodes provided and make a html list.
        function getList(data){
            var items = [];
            var list = "";
            data.forEach(function(i) {
                // Ignore nodes that are not visible
                if (i.visible !== false) {
                    // Get basic details
                    var href = (i.relPath || i.attributes.id);
                    var title = i.title || i.data;
                    var children = "";
                    if (i.children) {
                        // Make a recursive call to get child(ren) HTML
                        children = getList(i.children);
                    }
                    // Account for internal anchors
                    if (href.substring(href.length-4) === ".htm") {
                         href = "#" + href;
                    }
                    // Put all the details into a HTML string
                    items.push("<li><a href='" + href + "'>" + title + "</a>" + children + "</li>");
                }
            });
            // Frame all the list entries in a list and return
            if(items.length) {
                list = "<ul>\n" + items.join("\n") + "</ul>\n";
            }
            return list;
        }

        // Find our TOC element in the document and
        // fill it with the html from getList()
        jQ(rvt.tocSelector).html(getList(nodes));

        // ====================================
        // Create a callback function for a location change.
        function onLocationChange(location, data){
            // Work out where we are
            hash = data.hash;
            hash = hash.split("#")[0];
            // Update the UI for for the 'active' link
            jQ("a").removeClass("link-selected");
            jQ("a[href='#"+hash+"']").addClass("link-selected");
        }
        jQ(window.location).change(onLocationChange);
    };

    // ====================================
    // Callback for processing the manifest.
    // ====================================
    function processManifestJson(data) {
        rvt.setTitle(data.title);
        // Note: homePage & nodes are package level variables
        homePage = data.homePage;
        // We accept the manifest in either of two nodes
        manifest = data.toc || data.nodes;

        // But first we want to filter out hidden nodes
        nodes = [];
        jQ.each(manifest, function(c, node){
            var visible = (node.visible !== false);
            if (visible) nodes.push(node);
        });

        // Now parse the variety of field names we accept
        // into a normalised terminology for internal use
        function multiFormat(c, node){
            // Look at what we've got now
            var visible = (node.visible !== false);
            var id = (node.relPath || node.attributes.id);
            var title = (node.title || node.data);
            if (!node.attributes) {
                node.attributes = {};
            }
            // Normalise
            node.visible = visible;
            node.relPath = id;
            node.attributes.id = id;
            node.title = title;
            node.data = title;

            // Recursively process children
            jQ.each(node.children, multiFormat);
        }
        jQ.each(nodes, multiFormat);

        // If we don't have a homepage already
        if(!homePage || homePage == "toc.htm") {
            // Assuming there are even nodes
            if (nodes.length > 0){
                // Use the first node
                homePage = nodes[0].relPath;
                // Unless the URL already has one
                if (!window.location.hash) {
                    // Set in the URL, our polling callback will find it
                    window.location.hash = homePage;
                }
            // This is an empty manifest
            } else {
                homePage = "";
                rvt.updateContent("[No content]");
            }
        }

        // Build a TOC from our processed list
        rvt.displayTOC(nodes);

        // Start our polling callback to watch for URL changes
        checkForLocationChange();
        setInterval(checkForLocationChange, 100);
    }

    // ====================================
    // This function polls to process navigation changes in the URL
    // ====================================
    function checkForLocationChange() {
        // If the URL has changed
        if (checkForLocationChange.href !== window.location.href) {
            var hashOnly = false;
            hash = window.location.hash;
            // Trim the # character
            if (hash.length) {
                hash = hash.substring(1);
            }
            // If we have data from last time
            if (checkForLocationChange.href) {
                // Check whether we can determine if just the hash changed
                var oldHash = checkForLocationChange.href.split("#", 1)[0];
                var currentHash = window.location.href.split("#",1)[0];
                hashOnly = (oldHash === currentHash);
            }
            // Update our tracking variable
            checkForLocationChange.href = window.location.href;
            // Trigger the 'onLocationChange()' callback with our data
            jQ(window.location).trigger("change", {"hash":hash, "hashOnly":hashOnly});
        }
    }

    // ====================================
    //  Callback event for 'window.location' changing.
    //  We will be 'trigger()'ing  this manually however.
    // ====================================
    function onLocationChange(location, data){
        hash = data.hash;
        // Make sure we only look at the packaging hash (first one)
        hash = hash.split("#")[0];
        // If just the hash changed... make sure it really is different
        if (data.hashOnly){
            if (hash === onLocationChange.hash) return;
        }
        onLocationChange.hash = hash;

        // Use the homepage if the hash has been removed
        if (hash === "") {
            hash = homePage;
        }

        // Are we 'ignoring' this node? ie. Does it have server-side data?
        if (rvt.ignoreTest(hash)) {
            // Yes, let the configured callback know we are done
            if (rvt.contentLoadedCallback) {
                rvt.contentLoadedCallback(rvt);
            }
            rvt.contentScrollToTop();
            return;
        }

        // Use cached content if we've seen this node before
        if (pages[hash]) {
            rvt.updateContent(pages[hash]);
            rvt.contentScrollToTop();
            return;
        }

        // ====================================
        // Returns hPath or null (make a relative path an absolute hash path)
        function getHPath(path) {
            var ourParts, ourDepth, upCount=0, depth, hPath;
            // Check if the path is absolute already
            if (path.slice(0,1) === "/" || path.search("://") > 0) return null;
            // Break the hash up
            ourParts = hash.split("/");
            ourDepth = ourParts.length - 1;
            // Get rid of any useless pathing up to the first '/../' entry
            // Should catch sillyness like '././././../'
            path = path.replace(/[^\/\.]+\/\.\.\//g, "");
            // Adjust our depth as we remove '../' elements
            path = path.replace(/\.\.\//g, function(m) {upCount+=1; return "";});
            depth = ourDepth - upCount;
            // Path doesn't make sense inside the package
            if (depth < 0) return null;
            // Get the appropriate depth into the hash, combine
            // with the path and build a new path to return
            hPath = ourParts.slice(0,depth).concat(path.split("/")).join("/");
            return hPath
        }

        // ====================================
        // Callback for new content requests returning.
        // Some logic here is specifically looking for ICE
        // document structures to re-arrange.
        function callback(data) {
            // Large documents will crash jQuery is parsed directly
            //  so we farm them out to the browser by putting them in
            //  the page under a hidden div
            var jqData = $("<div/>");
            jqData.hide();
            jqData.html(data);

            var pageToc = jqData.find("div.page-toc");
            var body = jqData.find("div.body");

            // If ICE document structure not found, drop stright to screen
            if (body.size() == 0) {
                pages[hash] = data;
                rvt.updateContent(jQ(data));
                rvt.contentScrollToTop();
                return;
            }

            // Move ICE title in the document
            body.find("div.title").show().after(pageToc);

            // For content, we just want div children of the body
            var html = body.find(">div");

            // Oops, this should rarely happen, it means the ICE document
            //  has an unexpected page strucutre at the top level.
            if (html.size() == 0) {
                // Just push the html to screen
                pages[hash] = data;
                rvt.updateContent(body);
                rvt.contentScrollToTop();
                return;
            }

            // Move any rendition links down into the body of the
            // document so they aren't lost.
            jqData.find("div.rendition-links").each(
                function(c, node){
                    html.prepend("<div class='rendition-links'><span class='heading'>Renditions</span>" + unescape(jQ(node).html()) + "</div>");
                }
            );

            // Cache the content after all that parsing
            pages[hash] = html;
            // And update the page
            rvt.updateContent(pages[hash]);
            rvt.contentScrollToTop();
        }

        // Drop a loading message into the page
        rvt.updateContent(jQ("<div style='display:none;' id='loadingMessage'>"+rvt.loadingMessage+"<div>"));
        rvt.contentScrollToTop();
        // And go get the page content, it will return to 'callback()' above.
        if (rvt.serverRequestTemplate) {
            thisUrl = rvt.serverRequestTemplate.replace("{rvtHash}", hash);
            jQ.get(thisUrl, {}, callback, "html");
        } else {
            jQ.get(rvt.contentBaseUrl + hash, {}, callback, "html");
        }
    }

    // Register our event handler for location changes.
    // We'll 'trigger()' this manually from our polling above.
    jQ(window.location).change(onLocationChange);

    rvt.processManifestJson = processManifestJson;
    return rvt;
};

