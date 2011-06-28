/*
 * The Fascinator - Common Library
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package com.googlecode.fascinator.transform.vocabulary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.semanticdesktop.aperture.vocabulary.NCO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for Vocabulary
 * 
 * @author Linda Octalina
 */
public class VocabularyTest {
    private static Logger log = LoggerFactory.getLogger(VocabularyTest.class);

    @Test
    public void basicXML() throws Exception {
        InputStream is = getClass().getResourceAsStream("/aperture.rdf");

        URI uri = new URIImpl(
                "file:/home/octalina/workspace-fascinator2-trunk/the-fascinator/portal/98684bbd626085e63cf52929f3c34766");
        log.info("uri: {}", uri.toString());

        Model model = getRdfModel(is);

        RDFContainer container = new RDFContainerImpl(model, uri);
        String mimeType = container.getString(NIE.mimeType);
        Assert.assertEquals(container.getString(NIE.mimeType),
                "application/vnd.oasis.opendocument.text");

        List<String> creators = new ArrayList<String>();
        Collection collection = container.getAll(NCO.creator);
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Node node = (Node) iterator.next();
            URI creatorUri = model.createURI(node.toString());
            RDFContainer creatorContainer = new RDFContainerImpl(model,
                    creatorUri);
            String result = creatorContainer.getString(NCO.fullname);
            creators.add(result.trim());
        }
        Assert.assertEquals(2, creators.size());

        String plainText = container.getString(NIE.plainTextContent).trim();
        Assert.assertEquals(plainText, "Tests............");
    }

    public Model getRdfModel(InputStream rdfIn) throws ModelRuntimeException,
            IOException {

        Model model = RDF2Go.getModelFactory().createModel();

        model.open();
        model.readFrom(rdfIn);
        // model.dump();
        return model;
    }
}
