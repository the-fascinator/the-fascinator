package au.edu.usq.fascinator.vocabulary;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

/**
 * Vocabulary File. Created by org.ontoware.rdf2go.util.VocabularyWriter on Wed Sep 08 14:25:30 EST 2010
 * input file: C:\Users\dickinso.USQ\AppData\Local\Temp\SKOS.xml
 * namespace: http://www.w3.org/2004/02/skos/core
 */
public interface SKOS {
	public static final URI NS_SKOS = new URIImpl("http://www.w3.org/2004/02/skos/core",false);

    /**
     * Label: Ordered Collection@en 
     */
    public static final URI OrderedCollection = new URIImpl("http://www.w3.org/2004/02/skos/core#OrderedCollection", false);

    /**
     * Label: Collection@en 
     */
    public static final URI Collection = new URIImpl("http://www.w3.org/2004/02/skos/core#Collection", false);

    /**
     * Label: Concept Scheme@en 
     */
    public static final URI ConceptScheme = new URIImpl("http://www.w3.org/2004/02/skos/core#ConceptScheme", false);

    /**
     * Label: Concept@en 
     */
    public static final URI Concept = new URIImpl("http://www.w3.org/2004/02/skos/core#Concept", false);

    /**
     * Label: alternative label@en 
     * Comment: skos:prefLabel, skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.@en The range of skos:altLabel is the class of RDF plain literals.@en 
     */
    public static final URI altLabel = new URIImpl("http://www.w3.org/2004/02/skos/core#altLabel", false);

    /**
     * Label: has narrower match@en 
     */
    public static final URI narrowMatch = new URIImpl("http://www.w3.org/2004/02/skos/core#narrowMatch", false);

    /**
     * Label: scope note@en 
     */
    public static final URI scopeNote = new URIImpl("http://www.w3.org/2004/02/skos/core#scopeNote", false);

    /**
     * Label: has narrower@en 
     * Comment: Narrower concepts are typically rendered as children in a concept hierarchy (tree).@en 
     */
    public static final URI narrower = new URIImpl("http://www.w3.org/2004/02/skos/core#narrower", false);

    /**
     * Label: note@en 
     */
    public static final URI note = new URIImpl("http://www.w3.org/2004/02/skos/core#note", false);

    /**
     * Label: is top concept in scheme@en 
     * Comment: http://www.w3.org/2004/02/skos/core#Concept 
     * Range: http://www.w3.org/2004/02/skos/core#ConceptScheme 
     */
    public static final URI topConceptOf = new URIImpl("http://www.w3.org/2004/02/skos/core#topConceptOf", false);

    /**
     * Label: has broader@en 
     * Comment: Broader concepts are typically rendered as parents in a concept hierarchy (tree).@en 
     */
    public static final URI broader = new URIImpl("http://www.w3.org/2004/02/skos/core#broader", false);

    /**
     * Label: has broader transitive@en 
     */
    public static final URI broaderTransitive = new URIImpl("http://www.w3.org/2004/02/skos/core#broaderTransitive", false);

    /**
     * Label: has close match@en 
     */
    public static final URI closeMatch = new URIImpl("http://www.w3.org/2004/02/skos/core#closeMatch", false);

    /**
     * Label: definition@en 
     */
    public static final URI definition = new URIImpl("http://www.w3.org/2004/02/skos/core#definition", false);

    /**
     * Label: has narrower transitive@en 
     */
    public static final URI narrowerTransitive = new URIImpl("http://www.w3.org/2004/02/skos/core#narrowerTransitive", false);

    /**
     * Label: is in scheme@en 
     * Range: http://www.w3.org/2004/02/skos/core#ConceptScheme 
     */
    public static final URI inScheme = new URIImpl("http://www.w3.org/2004/02/skos/core#inScheme", false);

    /**
     * Label: notation@en 
     */
    public static final URI notation = new URIImpl("http://www.w3.org/2004/02/skos/core#notation", false);

    /**
     * Label: history note@en 
     */
    public static final URI historyNote = new URIImpl("http://www.w3.org/2004/02/skos/core#historyNote", false);

    /**
     * Label: hidden label@en 
     * Comment: skos:prefLabel, skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.@en The range of skos:hiddenLabel is the class of RDF plain literals.@en 
     */
    public static final URI hiddenLabel = new URIImpl("http://www.w3.org/2004/02/skos/core#hiddenLabel", false);

    /**
     * Label: has broader match@en 
     */
    public static final URI broadMatch = new URIImpl("http://www.w3.org/2004/02/skos/core#broadMatch", false);

    /**
     * Label: has related@en 
     * Comment: skos:related is disjoint with skos:broaderTransitive@en 
     */
    public static final URI related = new URIImpl("http://www.w3.org/2004/02/skos/core#related", false);

    /**
     * Label: has member@en 
     * Comment: http://www.w3.org/2004/02/skos/core#Collection 
     * Range: 43d8f24a:12aef9704d0:-7f98 
     */
    public static final URI member = new URIImpl("http://www.w3.org/2004/02/skos/core#member", false);

    /**
     * Label: has member list@en 
     * Comment: For any resource, every item in the list given as the value of the
      skos:memberList property is also a value of the skos:member property.@en 
     * Comment: http://www.w3.org/2004/02/skos/core#OrderedCollection 
     * Range: http://www.w3.org/1999/02/22-rdf-syntax-ns#List 
     */
    public static final URI memberList = new URIImpl("http://www.w3.org/2004/02/skos/core#memberList", false);

    /**
     * Label: example@en 
     */
    public static final URI example = new URIImpl("http://www.w3.org/2004/02/skos/core#example", false);

    /**
     * Label: has related match@en 
     */
    public static final URI relatedMatch = new URIImpl("http://www.w3.org/2004/02/skos/core#relatedMatch", false);

    /**
     * Label: is in semantic relation with@en 
     * Comment: http://www.w3.org/2004/02/skos/core#Concept 
     * Range: http://www.w3.org/2004/02/skos/core#Concept 
     */
    public static final URI semanticRelation = new URIImpl("http://www.w3.org/2004/02/skos/core#semanticRelation", false);

    /**
     * Label: change note@en 
     */
    public static final URI changeNote = new URIImpl("http://www.w3.org/2004/02/skos/core#changeNote", false);

    /**
     * Label: has top concept@en 
     * Comment: http://www.w3.org/2004/02/skos/core#ConceptScheme 
     * Range: http://www.w3.org/2004/02/skos/core#Concept 
     */
    public static final URI hasTopConcept = new URIImpl("http://www.w3.org/2004/02/skos/core#hasTopConcept", false);

    /**
     * Label: preferred label@en 
     * Comment: skos:prefLabel, skos:altLabel and skos:hiddenLabel are pairwise
      disjoint properties.@en The range of skos:prefLabel is the class of RDF plain literals.@en A resource has no more than one value of skos:prefLabel per language tag.@en 
     */
    public static final URI prefLabel = new URIImpl("http://www.w3.org/2004/02/skos/core#prefLabel", false);

    /**
     * Label: editorial note@en 
     */
    public static final URI editorialNote = new URIImpl("http://www.w3.org/2004/02/skos/core#editorialNote", false);

    /**
     * Label: has exact match@en 
     * Comment: skos:exactMatch is disjoint with each of the properties skos:broadMatch and skos:relatedMatch.@en 
     */
    public static final URI exactMatch = new URIImpl("http://www.w3.org/2004/02/skos/core#exactMatch", false);

    /**
     * Label: is in mapping relation with@en 
     * Comment: These concept mapping relations mirror semantic relations, and the data model defined below is similar (with the exception of skos:exactMatch) to the data model defined for semantic relations. A distinct vocabulary is provided for concept mapping relations, to provide a convenient way to differentiate links within a concept scheme from links between concept schemes. However, this pattern of usage is not a formal requirement of the SKOS data model, and relies on informal definitions of best practice.@en 
     */
    public static final URI mappingRelation = new URIImpl("http://www.w3.org/2004/02/skos/core#mappingRelation", false);

}
