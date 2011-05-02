/*
 * The Fascinator - Solr Result Testing
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
package au.edu.usq.fascinator.common.solr;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for SolrResult
 *
 * @author Greg Pendlebury
 */
public class SolrResultTest {

    private SolrResult result;

    @Before
    public void setup() throws Exception {
        result = new SolrResult(
                getClass().getResourceAsStream("/solr-result.json"));
    }

    /**
     * Tests simple metadata on results
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void simpleTests() throws Exception {
        // Metadata
        Assert.assertEquals((Integer) 20, result.getNumFound());
        Assert.assertEquals((Integer) 0,  result.getStartRow());
        Assert.assertEquals((Integer) 20, result.getRows());
        Assert.assertEquals((Integer) 0,  result.getStatus());
        Assert.assertEquals((Integer) 24, result.getQueryTime());

        // Result sizes
        List<SolrDoc> results = result.getResults();
        Assert.assertEquals(20, results.size());

        // Invalid request
        Assert.assertNull(results.get(0).get("randomGarbage"));
    }

    /**
     * Test various get logic against a basic String
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void basicString() throws Exception {
        SolrDoc record = result.getResults().get(0);
        Assert.assertEquals("workflow1", record.get("workflow_id"));
        Assert.assertEquals("workflow1", record.getFirst("workflow_id"));
        Assert.assertEquals("workflow1", record.getList("workflow_id").get(0));
        Assert.assertEquals(1, record.getList("workflow_id").size());
    }

    /**
     * Test various get logic against a single entry array
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void singleEntryArray() throws Exception {
        SolrDoc record = result.getResults().get(0);
        Assert.assertEquals("Pending", record.get("workflow_step_label"));
        Assert.assertEquals("Pending", record.getFirst("workflow_step_label"));
        Assert.assertEquals("Pending", record.getList("workflow_step_label").get(0));
        Assert.assertEquals(1, record.getList("workflow_step_label").size());
    }

    /**
     * Test various get logic against an array
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void multiEntryArray() throws Exception {
        SolrDoc record = result.getResults().get(0);
        Assert.assertEquals("[\"admin\", \"editor\", \"metadata\"]",
                record.get("security_filter"));
        Assert.assertEquals("admin", record.getFirst("security_filter"));
        Assert.assertEquals("admin", record.getList("security_filter").get(0));
        Assert.assertEquals(3, record.getList("security_filter").size());
    }

    /**
     * Test facet data
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void facetTest() throws Exception {
        Map<String, SolrFacet> facets = result.getFacets();
        Assert.assertEquals(9, facets.size());
        SolrFacet paths = facets.get("file_path");
        Assert.assertEquals(6, paths.values().size());
        Integer count = facets.get("workflow_step_label").values().get("Pending");
        Assert.assertEquals((Integer) 11, count);
        count = facets.get("workflow_step_label").count("Pending");
        Assert.assertEquals((Integer) 11, count);
    }

    /**
     * Test getting lists from all documents
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void listTest() throws Exception {
        List<String> repsonse = result.getFieldList("id");
        Assert.assertEquals(20, repsonse.size());
    }

    /**
     * Test getting the score from all documents
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void scoreTest() throws Exception {
        for (SolrDoc doc : result.getResults()) {
            Assert.assertNotNull("Should not return null!", doc.get("score"));
        }
    }
}
