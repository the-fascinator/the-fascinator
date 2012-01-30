/*
 * The Fascinator - JSON Manifest
 * Copyright (C) 2011 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.googlecode.fascinator.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Represents a JSON manifest as used in other parts of the application.
 * Provides methods to simplify common manipulations of the manifest.
 * </p>
 *
 * @author Greg Pendlebury
 */
public class Manifest extends JsonSimple {
    /**
     * Wrap a JsonObject in this class
     *
     * @param newJsonObject : The JsonObject to wrap
     */
    public Manifest(JsonObject newJsonObject) {
        super(newJsonObject);
    }

    /**
     * Creates Manifest object from the provided input stream
     *
     * @param jsonIn : The input stream to read
     * @throws IOException if there was an error during creation
     */
    public Manifest(InputStream jsonIn) throws IOException {
        super(jsonIn);
    }

    /**
     * Creates Manifest object from the provided string
     *
     * @param jsonString : The JSON in string form
     * @throws IOException if there was an error during creation
     */
    public Manifest(String jsonString) throws IOException {
        super(jsonString);
    }

    /**
     * Return the total number of nodes in this manifest.
     *
     * @return int : The number of nodes.
     */
    public int size() {
        return count(getTopNodes());
    }

    private int count(List<ManifestNode> list) {
        if (list == null) {
            return 0;
        }
        int thisList = list.size();
        for (ManifestNode entry : list) {
            thisList += count(entry.getChildren());
        }
        return thisList;
    }

    /**
     * Get the requested node from the manifest.
     *
     * @param id : The node key to retrieve
     * @return ManifestNode : The retrieved node, null if not found
     */
    public ManifestNode getNode(String key) {
        return findNode(key, getTopNodes());
    }

    private ManifestNode findNode(String key, List<ManifestNode> list) {
        if (list == null) {
            return null;
        }
        // For every node
        for (ManifestNode node : list) {
            // Check this node
            if (node.getKey().equals(key)) {
                return node;
            }
            // And check children
            ManifestNode descendent = findNode(key, node.getChildren());
            if (descendent != null) {
                return descendent;
            }
        }
        // Not found
        return null;
    }

    /**
     * Add a new child node to the top level.
     *
     * @param id : The ID value to put in this node.
     * @param title : The title value to put in this node.
     * @return boolean : True if successful, otherwise False
     */
    public boolean addTopNode(String id, String title) {
        if (id == null || title == null) {
            return false;
        }

        String key = "node-" + id;
        ManifestNode node = new ManifestNode();
        node.setId(id);
        node.setTitle(title);
        node.setKey(key);
        node.setParentKey(null);

        JsonObject object = writeObject("manifest");
        object.put(key, node.getJsonObject());
        return true;
    }

    /**
     * Get the requested node from the manifest.
     *
     * @param id : The node key to retrieve
     * @return ManifestNode : The retrieved node, null if not found
     */
    public List<ManifestNode> getTopNodes() {
        List<ManifestNode> nodes = new ArrayList<ManifestNode>();
        Map<String, JsonSimple> manifest = getJsonSimpleMap("manifest");
        if (manifest == null) {
            return nodes;
        }
        for (String key : manifest.keySet()) {
            // This node
            ManifestNode manNode = new ManifestNode(
                    manifest.get(key).getJsonObject());
            manNode.setParentKey(null);
            manNode.setKey(key);
            nodes.add(manNode);
        }
        return nodes;
    }

    /**
     * Get a writable JsonObject to add nodes at this point in the manifest.
     *
     * If 'key' is null it returns the top-level 'manifest' node, otherwise it
     * will return the 'children' object for the node indicated by 'key'.
     *
     * @param id : The node ID to add children to.
     * @return JsonObject : A writable JsonObject
     */
    private JsonObject getWritableNode(String key) {
        if (key == null) {
            // Top level node
            return getObject("manifest");
        }

        // Make sure it exists
        ManifestNode node = getNode(key);
        if (node == null) {
            return null;
        }

        // Return 'children'
        return node.writeObject("children");

    }

    /**
     * Get a writable JsonObject for the parent of the indicated ID.
     *
     * This method will ensure that the identified node exists and that the
     * object returned is writable and has the ID'd node as a child.
     *
     * @param id : The node ID to find the parent of.
     * @return JsonObject : A writable JsonObject
     */
    private JsonObject getWritableParent(String id) {
        // Find this node
        ManifestNode node = getNode(id);
        if (node == null) {
            return null;
        }

        // Find the parent
        String pId = node.getParentKey();
        JsonObject target = getWritableNode(pId);
        // Confirm our data
        if (target == null || !target.containsKey(id)) {
            return null;
        }

        return target;
    }

    public boolean move(String id, String destination) {
        // Find our nodes
        JsonObject toRemove = getWritableParent(id);
        JsonObject toMove = getWritableNode(destination);

        // Confirm validity of request
        if (toRemove == null || toMove == null) {
            return false;
        }

        // Now actually move it
        toMove.put(id, toRemove.get(id));
        toRemove.remove(id);

        // Update metadata
        getNode(id).setParentKey(destination);
        return true;
    }

    public boolean moveAfter(String id, String destination) {
        // Find our nodes
        JsonObject toRemove = getWritableParent(id);
        JsonObject toMove = getWritableParent(destination);

        // Confirm validity of request
        if (toRemove == null || toMove == null) {
            return false;
        }

        // Now actually move it
        Object movedNode = toRemove.get(id);
        toRemove.remove(id);
        // To avoid walking up two levels we're going to build a second map,
        //  then purge all on orginal, and add all from the second map.
        JsonObject newMap = new JsonObject();
        for (Object objKey : toMove.keySet()) {
            String key = (String) objKey;
            if (key.equals(destination)) {
                // Insert after
                newMap.put(objKey, toMove.get(objKey));
                newMap.put(id, movedNode);
            } else {
                // Just keep in place
                newMap.put(objKey, toMove.get(objKey));
            }
        }
        toMove.clear();
        toMove.putAll(newMap);

        // Update metadata
        String pId = getNode(destination).getParentKey();
        getNode(id).setParentKey(pId);
        return true;
    }

    public boolean moveBefore(String id, String destination) {
        // Find our nodes
        JsonObject toRemove = getWritableParent(id);
        JsonObject toMove = getWritableParent(destination);

        // Confirm validity of request
        if (toRemove == null || toMove == null) {
            return false;
        }

        // Now actually move it
        Object movedNode = toRemove.get(id);
        toRemove.remove(id);
        // To avoid walking up two levels we're going to build a second map,
        //  then purge all on orginal, and add all from the second map.
        JsonObject newMap = new JsonObject();
        for (Object objKey : toMove.keySet()) {
            String key = (String) objKey;
            if (key.equals(destination)) {
                // Insert before
                newMap.put(id, movedNode);
                newMap.put(objKey, toMove.get(objKey));
            } else {
                // Just keep in place
                newMap.put(objKey, toMove.get(objKey));
            }
        }
        toMove.clear();
        toMove.putAll(newMap);

        // Update metadata
        String pId = getNode(destination).getParentKey();
        getNode(id).setParentKey(pId);
        return true;
    }

    public boolean delete(String id) {
        JsonObject toRemove = getWritableParent(id);

        // Confirm validity of request
        if (toRemove == null) {
            return false;
        }

        toRemove.remove(id);
        return true;
    }

    /**
     * Get the description for this manifest
     *
     * @return String : The description of this manifest
     */
    public String getDescription() {
        return getString(null, "description");
    }

    /**
     * Set the description for this manifest
     *
     * @param description : The new description
     */
    public void setDescription(String description) {
        getJsonObject().put("description", description);
    }

    /**
     * Get the title for this manifest
     *
     * @return String : The title of this manifest
     */
    public String getTitle() {
        return getString(null, "title");
    }

    /**
     * Set the title for this manifest
     *
     * @param title : The new title
     */
    public void setTitle(String title) {
        getJsonObject().put("title", title);
    }

    /**
     * Get the type of this manifest
     *
     * @return String : The type of this manifest
     */
    public String getType() {
        return getString(null, "packageType");
    }

    /**
     * Set the type of this manifest
     *
     * @param type : The new type
     */
    public void setType(String type) {
        getJsonObject().put("packageType", type);
    }

    /**
     * Get the 'viewId' node for this manifest
     *
     * @return String : The viewId of this manifest
     */
    public String getViewId() {
        return getString(null, "viewId");
    }

    /**
     * Set the 'viewId' node for this manifest
     *
     * @param viewId : The new viewId
     */
    public void setViewId(String viewId) {
        getJsonObject().put("viewId", viewId);
    }
}
