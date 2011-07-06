/*
 * The Fascinator - Manifest Testing
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

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Manifest
 *
 * @author Greg Pendlebury
 */
public class ManifestTest {

    private Manifest manifest;

    @Before
    public void setup() throws Exception {
        manifest = new Manifest(
                getClass().getResourceAsStream("/manifest.json"));
    }

    /**
     * Tests basic reading of the manifest
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void readTests() throws Exception {
        Assert.assertEquals("Beyond the PDF demo", manifest.getTitle());
        Assert.assertEquals("", manifest.getDescription());
        Assert.assertEquals("default", manifest.getType());
        Assert.assertEquals("default", manifest.getViewId());

        // List size checks
        Assert.assertEquals(18, manifest.size());
        List<ManifestNode> topNodes = manifest.getTopNodes();
        Assert.assertEquals(16, topNodes.size());
        List<ManifestNode> children = topNodes.get(7).getChildren();
        Assert.assertEquals(2, children.size());

        // Three different read methods
        Assert.assertEquals("supporting_data.xls", children.get(1).getTitle());
        ManifestNode node = manifest.getNode(
                "node-e68e35fd2a69bd9d7f73ae48967589e3");
        Assert.assertEquals("supporting_data.xls", node.getTitle());
        String basicRead = manifest.getString(null, "manifest",
                "node-d14ba8337803fd865deec16ea1081bbf", "children",
                "node-e68e35fd2a69bd9d7f73ae48967589e3", "title");
        Assert.assertEquals("supporting_data.xls", basicRead);
    }

    /**
     * Test whether normal API access is keeping in sync by object reference.
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void movementTests() throws Exception {
        // Initial counts
        List<ManifestNode> topNodes = manifest.getTopNodes();
        Assert.assertEquals(16, topNodes.size());
        List<ManifestNode> children = topNodes.get(7).getChildren();
        Assert.assertEquals(2, children.size());

        //********************************************
        // Move a top level node into an existing list
        Assert.assertTrue(manifest.move("node-11eef347b93b83e08dc84ff65a3ac002",
                "node-d14ba8337803fd865deec16ea1081bbf"));
        // Check our data
        Assert.assertEquals(18, manifest.size());
        topNodes = manifest.getTopNodes();
        Assert.assertEquals(15, topNodes.size());
        children = topNodes.get(6).getChildren();
        Assert.assertEquals(3, children.size());
        Assert.assertEquals("FinalPaper.pdf", children.get(2).getTitle());

        //********************************************
        // Move a top level node into an existing list - after the previous one
        Assert.assertTrue(manifest.moveAfter(
                "node-242aaf66a8deb93a3506b77cf94a1b41",
                "node-11eef347b93b83e08dc84ff65a3ac002"));
        // Check our data - Note the children.get(2) call is the same
        Assert.assertEquals(18, manifest.size());
        topNodes = manifest.getTopNodes();
        Assert.assertEquals(14, topNodes.size());
        children = topNodes.get(5).getChildren();
        Assert.assertEquals(4, children.size());
        Assert.assertEquals("FinalPaper.pdf", children.get(2).getTitle());
        Assert.assertEquals("Protein Data Bank  Advisory Committee",
                children.get(3).getTitle());

        //********************************************
        // Move the same node - now before the first one
        Assert.assertTrue(manifest.moveBefore(
                "node-242aaf66a8deb93a3506b77cf94a1b41",
                "node-11eef347b93b83e08dc84ff65a3ac002"));
        // Check our data - Note the children.get(2) call is the same
        Assert.assertEquals(18, manifest.size());
        topNodes = manifest.getTopNodes();
        Assert.assertEquals(14, topNodes.size());
        children = topNodes.get(5).getChildren();
        Assert.assertEquals(4, children.size());
        Assert.assertEquals("Protein Data Bank  Advisory Committee",
                children.get(2).getTitle());
        Assert.assertEquals("FinalPaper.pdf", children.get(3).getTitle());

        //********************************************
        // Now we'll try creating a deeper structure
        Assert.assertFalse(children.get(3).hasChildren());
        Assert.assertTrue(manifest.move(
                "node-6d61f96b53e1da65f8896fdda5517a04",
                "node-11eef347b93b83e08dc84ff65a3ac002"));
        // Check our data
        Assert.assertEquals(18, manifest.size());
        topNodes = manifest.getTopNodes();
        // Top nodes goes down by one
        Assert.assertEquals(13, topNodes.size());
        // Our nested structure doesn't move, we picked from the end this time
        children = topNodes.get(5).getChildren();
        // No extra children
        Assert.assertEquals(4, children.size());
        // But we now have a grand child
        List<ManifestNode> grandChildren = children.get(3).getChildren();
        Assert.assertEquals(1, grandChildren.size());
        Assert.assertEquals("supporting_info.doc",
                grandChildren.get(0).getTitle());

        //********************************************
        // Even deeper, this time creating two new nodes
        Assert.assertEquals(18, manifest.size());
        Assert.assertTrue(manifest.addTopNode(
                "topNode", "A new top level node"));
        ManifestNode node = manifest.getNode("node-topNode");
        node.addChild("newNode", "A new child");
        // Check our data
        Assert.assertEquals(20, manifest.size());
        topNodes = manifest.getTopNodes();
        // Top nodes goes up by one
        Assert.assertEquals(14, topNodes.size());
        // No other changes
        children = topNodes.get(5).getChildren();
        Assert.assertEquals(4, children.size());
        grandChildren = children.get(3).getChildren();
        Assert.assertEquals(1, grandChildren.size());
        // Now move
        Assert.assertTrue(manifest.move(
                "node-topNode",
                "node-6d61f96b53e1da65f8896fdda5517a04"));

        // Top nodes goes down by one
        topNodes = manifest.getTopNodes();
        Assert.assertEquals(13, topNodes.size());
        // Children is the same size and index
        children = topNodes.get(5).getChildren();
        Assert.assertEquals(4, children.size());
        grandChildren = children.get(3).getChildren();
        // Newer generations
        Assert.assertEquals(1, grandChildren.size());
        Assert.assertEquals(1, grandChildren.get(0).getChildren().size());
        Assert.assertEquals(1, grandChildren.get(0).getChildren().get(0).getChildren().size());

        // Just a simple read, but demonstrating this deep structure via the API
        String basicRead = manifest.getString(null, "manifest",
                "node-d14ba8337803fd865deec16ea1081bbf", "children",
                "node-11eef347b93b83e08dc84ff65a3ac002", "children",
                "node-6d61f96b53e1da65f8896fdda5517a04", "children",
                "node-topNode", "children",
                "node-newNode", "title");
        Assert.assertEquals("A new child", basicRead);
    }
}
