package au.edu.usq.fascinator.vocabulary;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

/**
 * Vocabulary File. Created by org.ontoware.rdf2go.util.VocabularyWriter on Wed Sep 08 14:25:24 EST 2010
 * input file: C:\Users\dickinso.USQ\AppData\Local\Temp\DC.xml
 * namespace: http://purl.org/dc/elements/1.1/
 */
public interface DC {
	public static final URI NS_DC = new URIImpl("http://purl.org/dc/elements/1.1/",false);

    /**
     * Label: Date@en-us 
     * Comment: A point or period of time associated with an event in the lifecycle of the resource.@en-us 
     */
    public static final URI date = new URIImpl("http://purl.org/dc/elements/1.1/date", false);

    /**
     * Label: Language@en-us 
     * Comment: A language of the resource.@en-us 
     */
    public static final URI language = new URIImpl("http://purl.org/dc/elements/1.1/language", false);

    /**
     * Label: Format@en-us 
     * Comment: The file format, physical medium, or dimensions of the resource.@en-us 
     */
    public static final URI format = new URIImpl("http://purl.org/dc/elements/1.1/format", false);

    /**
     * Label: Title@en-us 
     * Comment: A name given to the resource.@en-us 
     */
    public static final URI title = new URIImpl("http://purl.org/dc/elements/1.1/title", false);

    /**
     * Label: Publisher@en-us 
     * Comment: An entity responsible for making the resource available.@en-us 
     */
    public static final URI publisher = new URIImpl("http://purl.org/dc/elements/1.1/publisher", false);

    /**
     * Label: Description@en-us 
     * Comment: An account of the resource.@en-us 
     */
    public static final URI description = new URIImpl("http://purl.org/dc/elements/1.1/description", false);

    /**
     * Label: Type@en-us 
     * Comment: The nature or genre of the resource.@en-us 
     */
    public static final URI type = new URIImpl("http://purl.org/dc/elements/1.1/type", false);

    /**
     * Label: Coverage@en-us 
     * Comment: The spatial or temporal topic of the resource, the spatial applicability of the resource, or the jurisdiction under which the resource is relevant.@en-us 
     */
    public static final URI coverage = new URIImpl("http://purl.org/dc/elements/1.1/coverage", false);

    /**
     * Label: Source@en-us 
     * Comment: A related resource from which the described resource is derived.@en-us 
     */
    public static final URI source = new URIImpl("http://purl.org/dc/elements/1.1/source", false);

    /**
     * Label: Relation@en-us 
     * Comment: A related resource.@en-us 
     */
    public static final URI relation = new URIImpl("http://purl.org/dc/elements/1.1/relation", false);

    /**
     * Label: Contributor@en-us 
     * Comment: An entity responsible for making contributions to the resource.@en-us 
     */
    public static final URI contributor = new URIImpl("http://purl.org/dc/elements/1.1/contributor", false);

    /**
     * Label: Subject@en-us 
     * Comment: The topic of the resource.@en-us 
     */
    public static final URI subject = new URIImpl("http://purl.org/dc/elements/1.1/subject", false);

    /**
     * Label: Identifier@en-us 
     * Comment: An unambiguous reference to the resource within a given context.@en-us 
     */
    public static final URI identifier = new URIImpl("http://purl.org/dc/elements/1.1/identifier", false);

    /**
     * Label: Creator@en-us 
     * Comment: An entity primarily responsible for making the resource.@en-us 
     */
    public static final URI creator = new URIImpl("http://purl.org/dc/elements/1.1/creator", false);

    /**
     * Label: Rights@en-us 
     * Comment: Information about rights held in and over the resource.@en-us 
     */
    public static final URI rights = new URIImpl("http://purl.org/dc/elements/1.1/rights", false);

}
