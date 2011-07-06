/*
 * The Fascinator - JSON Manifest Node
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Represents a node in a JSON manifest. This object wraps the JSON interface
 * (but does not extend it) with supplemental data to aid navigation.
 * </p>
 *
 * @author Greg Pendlebury
 */
public class ManifestNode extends JsonSimple {
    private String parentKey;
    private String thisKey;

    /**
     * Create a new empty node
     *
     */
    public ManifestNode() {
        super(new JsonObject());
    }

    /**
     * Wrap a JsonObject in this class
     *
     * @param newJsonObject : The JsonObject to wrap
     */
    public ManifestNode(JsonObject newJsonObject) {
        super(newJsonObject);
    }

    /**
     * Get the key of this node.
     *
     * @return String : The node key
     */
    public String getKey() {
        return thisKey;
    }

    /**
     * Set the key of this node.
     *
     * @param newKey : The new key
     */
    public void setKey(String newKey) {
        thisKey = newKey;
    }

    /**
     * Get the key of the parent node to this node.
     *
     * @return String : The parent node key, null if this is a top level node
     */
    public String getParentKey() {
        return parentKey;
    }

    /**
     * Set the key of the parent node to this node.
     *
     * @param newParent : The parent node key, null if this is a top level node
     */
    public void setParentKey(String newParent) {
        parentKey = newParent;
    }

    /**
     * Add a new child node to this node.
     *
     * @param id : The ID value to put in this node.
     * @param title : The title value to put in this node.
     * @return boolean : True if successful, otherwise False
     */
    public boolean addChild(String id, String title) {
        if (id == null || title == null) {
            return false;
        }

        String key = "node-" + id;
        ManifestNode node = new ManifestNode();
        node.setId(id);
        node.setTitle(title);
        node.setKey(key);
        node.setParentKey(thisKey);

        JsonObject object = writeObject("children");
        object.put(key, node.getJsonObject());
        return true;
    }

    /**
     * Check if this node has any children.
     *
     * @return boolean : True if there are children, otherwise False
     */
    public boolean hasChildren() {
        JsonObject object = getObject("children");
        if (object == null) {
            return false;
        }
        return true;
    }

    /**
     * Return a list of children for this node. The list will reflect the order
     * of the underlying manifest.
     *
     * @return List<ManifestNode> : List of child nodes. Will not return null,
     * a zero length List will be returned if no children are found.
     */
    public List<ManifestNode> getChildren() {
        List<ManifestNode> response = new ArrayList();
        Map<String, JsonSimple> children = getJsonSimpleMap("children");
        if (children == null) {
            return response;
        }
        for (String key : children.keySet()) {
            ManifestNode manNode = new ManifestNode(
                    children.get(key).getJsonObject());
            manNode.setParentKey(this.getKey());
            manNode.setKey(key);
            response.add(manNode);
        }
        return response;
    }

    /**
     * Get the hidden flag for this node.
     *
     * @return boolean : True if hidden, False otherwise
     */
    public boolean getHidden() {
        return getBoolean(false, "hidden");
    }

    /**
     * Set the hidden flag for this node.
     *
     * @param hidden : The new flag
     */
    public void setHidden(boolean hidden) {
        getJsonObject().put("hidden", hidden);
    }

    /**
     * Get the ID for this node.
     *
     * @return String : The ID of this node
     */
    public String getId() {
        return getString(null, "id");
    }

    /**
     * Set the ID for this node.
     *
     * @param id : The new ID
     */
    public void setId(String id) {
        getJsonObject().put("id", id);
    }

    /**
     * Get the title for this node.
     *
     * @return String : The title of this node
     */
    public String getTitle() {
        return getString(null, "title");
    }

    /**
     * Set the title for this node.
     *
     * @param title : The new title
     */
    public void setTitle(String title) {
        getJsonObject().put("title", title);
    }
}
