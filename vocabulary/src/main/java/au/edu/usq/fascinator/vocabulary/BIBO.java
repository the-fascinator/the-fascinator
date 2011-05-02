package au.edu.usq.fascinator.vocabulary;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

/**
 * Vocabulary File. Created by org.ontoware.rdf2go.util.VocabularyWriter on Wed Sep 08 14:25:06 EST 2010
 * input file: C:\Users\dickinso.USQ\AppData\Local\Temp\BIBO.xml
 * namespace: http://purl.org/ontology/bibo/
 */
public interface BIBO {
	public static final URI NS_BIBO = new URIImpl("http://purl.org/ontology/bibo/",false);

    /**
     * Label: Note@en 
     * Comment: Notes or annotations about a resource.@en 
     */
    public static final URI Note = new URIImpl("http://purl.org/ontology/bibo/Note", false);

    /**
     */
    public static final URI Event = new URIImpl("http://purl.org/ontology/bibo/Event", false);

    /**
     * Label: Chapter@en 
     * Comment: A chapter of a book.@en 
     */
    public static final URI Chapter = new URIImpl("http://purl.org/ontology/bibo/Chapter", false);

    /**
     * Label: Excerpt@en 
     * Comment: A passage selected from a larger work.@en 
     */
    public static final URI Excerpt = new URIImpl("http://purl.org/ontology/bibo/Excerpt", false);

    /**
     * Label: Book Section@en 
     * Comment: A section of a book.@en 
     */
    public static final URI BookSection = new URIImpl("http://purl.org/ontology/bibo/BookSection", false);

    /**
     * Label: Map@en 
     * Comment: A graphical depiction of geographic features.@en 
     */
    public static final URI Map = new URIImpl("http://purl.org/ontology/bibo/Map", false);

    /**
     * Label: Letter@en 
     * Comment: A written or printed communication addressed to a person or organization and usually transmitted by mail.@en 
     */
    public static final URI Letter = new URIImpl("http://purl.org/ontology/bibo/Letter", false);

    /**
     * Label: Standard@en 
     * Comment: A document describing a standard@en 
     */
    public static final URI Standard = new URIImpl("http://purl.org/ontology/bibo/Standard", false);

    /**
     * Label: Academic Article@en 
     * Comment: A scholarly academic article, typically published in a journal.@en 
     */
    public static final URI AcademicArticle = new URIImpl("http://purl.org/ontology/bibo/AcademicArticle", false);

    /**
     * Label: Manuscript@en 
     * Comment: An unpublished Document, which may also be submitted to a publisher for publication.@en 
     */
    public static final URI Manuscript = new URIImpl("http://purl.org/ontology/bibo/Manuscript", false);

    /**
     * Label: Reference Source@en 
     * Comment: A document that presents authoritative reference information, such as a dictionary or encylopedia .@en 
     */
    public static final URI ReferenceSource = new URIImpl("http://purl.org/ontology/bibo/ReferenceSource", false);

    /**
     * Label: Personal Communication Document@en 
     * Comment: A personal communication manifested in some document.@en 
     */
    public static final URI PersonalCommunicationDocument = new URIImpl("http://purl.org/ontology/bibo/PersonalCommunicationDocument", false);

    /**
     * Label: Report@en 
     * Comment: A document describing an account or statement describing in detail an event, situation, or the like, usually as the result of observation, inquiry, etc..@en 
     */
    public static final URI Report = new URIImpl("http://purl.org/ontology/bibo/Report", false);

    /**
     * Label: Thesis@en 
     * Comment: A document created to summarize research findings associated with the completion of an academic degree.@en 
     */
    public static final URI Thesis = new URIImpl("http://purl.org/ontology/bibo/Thesis", false);

    /**
     * Label: Collection@en 
     * Comment: A collection of Documents or Collections@en 
     */
    public static final URI Collection = new URIImpl("http://purl.org/ontology/bibo/Collection", false);

    /**
     * Label: Film@en 
     * Comment: aka movie.@en 
     */
    public static final URI Film = new URIImpl("http://purl.org/ontology/bibo/Film", false);

    /**
     * Label: Manual@en 
     * Comment: A small reference book, especially one giving instructions.@en 
     */
    public static final URI Manual = new URIImpl("http://purl.org/ontology/bibo/Manual", false);

    /**
     * Label: Legal Case Document@en 
     * Comment: A document accompanying a legal case.@en 
     */
    public static final URI LegalCaseDocument = new URIImpl("http://purl.org/ontology/bibo/LegalCaseDocument", false);

    /**
     * Label: audio document@en 
     * Comment: An audio document; aka record.@en 
     */
    public static final URI AudioDocument = new URIImpl("http://purl.org/ontology/bibo/AudioDocument", false);

    /**
     * Label: Bill@en 
     * Comment: Draft legislation presented for discussion to a legal body.@en 
     */
    public static final URI Bill = new URIImpl("http://purl.org/ontology/bibo/Bill", false);

    /**
     * Label: Quote@en 
     * Comment: An excerpted collection of words.@en 
     */
    public static final URI Quote = new URIImpl("http://purl.org/ontology/bibo/Quote", false);

    /**
     * Label: EMail@en 
     * Comment: A written communication addressed to a person or organization and transmitted electronically.@en 
     */
    public static final URI Email = new URIImpl("http://purl.org/ontology/bibo/Email", false);

    /**
     * Label: Journal@en 
     * Comment: A periodical of scholarly journal Articles.@en 
     */
    public static final URI Journal = new URIImpl("http://purl.org/ontology/bibo/Journal", false);

    /**
     * Label: Interview@en 
     * Comment: A formalized discussion between two or more people.@en 
     */
    public static final URI Interview = new URIImpl("http://purl.org/ontology/bibo/Interview", false);

    /**
     * Label: Document Status@en 
     * Comment: The status of the publication of a document.@en 
     */
    public static final URI DocumentStatus = new URIImpl("http://purl.org/ontology/bibo/DocumentStatus", false);

    /**
     * Label: Issue@en 
     * Comment: something that is printed or published and distributed, esp. a given number of a periodical@en 
     */
    public static final URI Issue = new URIImpl("http://purl.org/ontology/bibo/Issue", false);

    /**
     * Label: Hearing@en 
     * Comment: An instance or a session in which testimony and arguments are presented, esp. before an official, as a judge in a lawsuit.@en 
     */
    public static final URI Hearing = new URIImpl("http://purl.org/ontology/bibo/Hearing", false);

    /**
     * Label: Legislation@en 
     * Comment: A legal document proposing or enacting a law or a group of laws.@en 
     */
    public static final URI Legislation = new URIImpl("http://purl.org/ontology/bibo/Legislation", false);

    /**
     * Label: Decision@en 
     * Comment: A document containing an authoritative determination (as a decree or judgment) made after consideration of facts or law.@en 
     */
    public static final URI LegalDecision = new URIImpl("http://purl.org/ontology/bibo/LegalDecision", false);

    /**
     * Label: Conference@en 
     * Comment: A meeting for consultation or discussion.@en 
     */
    public static final URI Conference = new URIImpl("http://purl.org/ontology/bibo/Conference", false);

    /**
     * Label: Image@en 
     * Comment: A document that presents visual or diagrammatic information.@en 
     */
    public static final URI Image = new URIImpl("http://purl.org/ontology/bibo/Image", false);

    /**
     * Label: Thesis degree@en 
     * Comment: The academic degree of a Thesis@en 
     */
    public static final URI ThesisDegree = new URIImpl("http://purl.org/ontology/bibo/ThesisDegree", false);

    /**
     * Label: document part@en 
     * Comment: a distinct part of a larger document or collected document.@en 
     */
    public static final URI DocumentPart = new URIImpl("http://purl.org/ontology/bibo/DocumentPart", false);

    /**
     * Label: Document@en 
     * Comment: A document (noun) is a bounded physical representation of body of information designed with the capacity (and usually intent) to communicate. A document may manifest symbolic, diagrammatic or sensory-representational information.@en 
     */
    public static final URI Document = new URIImpl("http://purl.org/ontology/bibo/Document", false);

    /**
     * Label: Slideshow@en 
     * Comment: A presentation of a series of slides, usually presented in front of an audience with written text and images.@en 
     */
    public static final URI Slideshow = new URIImpl("http://purl.org/ontology/bibo/Slideshow", false);

    /**
     * Label: Website@en 
     * Comment: A group of Webpages accessible on the Web.@en 
     */
    public static final URI Website = new URIImpl("http://purl.org/ontology/bibo/Website", false);

    /**
     * Label: Series@en 
     * Comment: A loose, thematic, collection of Documents, often Books.@en 
     */
    public static final URI MultiVolumeBook = new URIImpl("http://purl.org/ontology/bibo/MultiVolumeBook", false);

    /**
     * Label: Performance@en 
     * Comment: A public performance.@en 
     */
    public static final URI Performance = new URIImpl("http://purl.org/ontology/bibo/Performance", false);

    /**
     * Label: Legal Document@en 
     * Comment: A legal document; for example, a court decision, a brief, and so forth.@en 
     */
    public static final URI LegalDocument = new URIImpl("http://purl.org/ontology/bibo/LegalDocument", false);

    /**
     * Label: Book@en 
     * Comment: A written or printed work of fiction or nonfiction, usually on sheets of paper fastened or bound together within covers.@en 
     */
    public static final URI Book = new URIImpl("http://purl.org/ontology/bibo/Book", false);

    /**
     * Label: Brief@en 
     * Comment: A written argument submitted to a court.@en 
     */
    public static final URI Brief = new URIImpl("http://purl.org/ontology/bibo/Brief", false);

    /**
     * Label: Workshop@en 
     * Comment: A seminar, discussion group, or the like, that emphasizes zxchange of ideas and the demonstration and application of techniques, skills, etc.@en 
     */
    public static final URI Workshop = new URIImpl("http://purl.org/ontology/bibo/Workshop", false);

    /**
     * Label: Personal Communication@en 
     * Comment: A communication between an agent and one or more specific recipients.@en 
     */
    public static final URI PersonalCommunication = new URIImpl("http://purl.org/ontology/bibo/PersonalCommunication", false);

    /**
     * Label: Article@en 
     * Comment: A written composition in prose, usually nonfiction, on a specific topic, forming an independent part of a book or other publication, as a newspaper or magazine.@en 
     */
    public static final URI Article = new URIImpl("http://purl.org/ontology/bibo/Article", false);

    /**
     * Label: Magazine@en 
     * Comment: A periodical of magazine Articles. A magazine is a publication that is issued periodically, usually bound in a paper cover, and typically contains essays, stories, poems, etc., by many writers, and often photographs and drawings, frequently specializing in a particular subject or area, as hobbies, news, or sports.@en 
     */
    public static final URI Magazine = new URIImpl("http://purl.org/ontology/bibo/Magazine", false);

    /**
     * Label: Newspaper@en 
     * Comment: A periodical of documents, usually issued daily or weekly, containing current news, editorials, feature articles, and usually advertising.@en 
     */
    public static final URI Newspaper = new URIImpl("http://purl.org/ontology/bibo/Newspaper", false);

    /**
     * Label: Patent@en 
     * Comment: A document describing the exclusive right granted by a government to an inventor to manufacture, use, or sell an invention for a certain number of years.@en 
     */
    public static final URI Patent = new URIImpl("http://purl.org/ontology/bibo/Patent", false);

    /**
     * Label: Series@en 
     * Comment: A loose, thematic, collection of Documents, often Books.@en 
     */
    public static final URI Series = new URIImpl("http://purl.org/ontology/bibo/Series", false);

    /**
     * Label: Proceedings@en 
     * Comment: A compilation of documents published from an event, such as a conference.@en 
     */
    public static final URI Proceedings = new URIImpl("http://purl.org/ontology/bibo/Proceedings", false);

    /**
     * Label: Edited Book@en 
     * Comment: An edited book.@en 
     */
    public static final URI EditedBook = new URIImpl("http://purl.org/ontology/bibo/EditedBook", false);

    /**
     * Label: Periodical@en 
     * Comment: A group of related documents issued at regular intervals.@en 
     */
    public static final URI Periodical = new URIImpl("http://purl.org/ontology/bibo/Periodical", false);

    /**
     * Label: audio-visual document@en 
     * Comment: An audio-visual document; film, video, and so forth.@en 
     */
    public static final URI AudioVisualDocument = new URIImpl("http://purl.org/ontology/bibo/AudioVisualDocument", false);

    /**
     * Label: Court Reporter@en 
     * Comment: A collection of legal cases.@en 
     */
    public static final URI CourtReporter = new URIImpl("http://purl.org/ontology/bibo/CourtReporter", false);

    /**
     * Label: Slide@en 
     * Comment: A slide in a slideshow@en 
     */
    public static final URI Slide = new URIImpl("http://purl.org/ontology/bibo/Slide", false);

    /**
     * Label: Code@en 
     * Comment: A collection of statutes.@en 
     */
    public static final URI Code = new URIImpl("http://purl.org/ontology/bibo/Code", false);

    /**
     * Label: Webpage@en 
     * Comment: A web page is an online document available (at least initially) on the world wide web. A web page is written first and foremost to appear on the web, as distinct from other online resources such as books, manuscripts or audio documents which use the web primarily as a distribution mechanism alongside other more traditional methods such as print.@en 
     */
    public static final URI Webpage = new URIImpl("http://purl.org/ontology/bibo/Webpage", false);

    /**
     * Label: Statute@en 
     * Comment: A bill enacted into law.@en 
     */
    public static final URI Statute = new URIImpl("http://purl.org/ontology/bibo/Statute", false);

    /**
     * Label: Collected Document@en 
     * Comment: A document that simultaneously contains other documents.@en 
     */
    public static final URI CollectedDocument = new URIImpl("http://purl.org/ontology/bibo/CollectedDocument", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fca 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI isbn13 = new URIImpl("http://purl.org/ontology/bibo/isbn13", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fcd 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI isbn10 = new URIImpl("http://purl.org/ontology/bibo/isbn10", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fd9 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI eissn = new URIImpl("http://purl.org/ontology/bibo/eissn", false);

    /**
     * Label: chapter@en 
     * Comment: An chapter number@en 
     * Comment: http://purl.org/ontology/bibo/BookSection 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI chapter = new URIImpl("http://purl.org/ontology/bibo/chapter", false);

    /**
     * Label: section@en 
     * Comment: A section number@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI section = new URIImpl("http://purl.org/ontology/bibo/section", false);

    /**
     * Label: content@en 
     * Comment: This property is for a plain-text rendering of the content of a Document. While the plain-text content of an entire document could be described by this property.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI content = new URIImpl("http://purl.org/ontology/bibo/content", false);

    /**
     * Label: volume@en 
     * Comment: A volume number@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI volume = new URIImpl("http://purl.org/ontology/bibo/volume", false);

    /**
     * Label: edition@en 
     * Comment: The name defining a special edition of a document. Normally its a literal value composed of a version number and words.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI edition = new URIImpl("http://purl.org/ontology/bibo/edition", false);

    /**
     * Label: issue@en 
     * Comment: An issue number@en 
     * Comment: http://purl.org/ontology/bibo/Issue 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI issue = new URIImpl("http://purl.org/ontology/bibo/issue", false);

    /**
     * Label: short title@en 
     * Comment: The abbreviation of a title.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI shortTitle = new URIImpl("http://purl.org/ontology/bibo/shortTitle", false);

    /**
     * Label: number of volumes@en 
     * Comment: The number of volumes contained in a collection of documents (usually a series, periodical, etc.).@en 
     * Comment: http://purl.org/ontology/bibo/Collection 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI numVolumes = new URIImpl("http://purl.org/ontology/bibo/numVolumes", false);

    /**
     * Label: abstract 
     * Comment: A summary of the resource. 
     * Comment: http://www.w3.org/2000/01/rdf-schema#Resource 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI abstract_ = new URIImpl("http://purl.org/ontology/bibo/abstract", false);

    /**
     * Label: number@en 
     * Comment: A generic item or document number. Not to be confused with issue number.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI number = new URIImpl("http://purl.org/ontology/bibo/number", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fd3 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI handle = new URIImpl("http://purl.org/ontology/bibo/handle", false);

    /**
     * Label: suffix name@en 
     * Comment: The suffix of a name@en 
     * Comment: http://xmlns.com/foaf/0.1/Agent 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI suffixName = new URIImpl("http://purl.org/ontology/bibo/suffixName", false);

    /**
     * Label: page end@en 
     * Comment: Ending page number within a continuous page range.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI pageEnd = new URIImpl("http://purl.org/ontology/bibo/pageEnd", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fe2 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI coden = new URIImpl("http://purl.org/ontology/bibo/coden", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fdc 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI eanucc13 = new URIImpl("http://purl.org/ontology/bibo/eanucc13", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fc7 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI issn = new URIImpl("http://purl.org/ontology/bibo/issn", false);

    /**
     * Label: uri@en 
     * Comment: Universal Resource Identifier of a document@en 
     * Comment: 43d8f24a:12aef9704d0:-7fb5 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI uri = new URIImpl("http://purl.org/ontology/bibo/uri", false);

    /**
     * Label: number of pages@en 
     * Comment: The number of pages contained in a document@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI numPages = new URIImpl("http://purl.org/ontology/bibo/numPages", false);

    /**
     */
    public static final URI isbn = new URIImpl("http://purl.org/ontology/bibo/isbn", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fb8 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI upc = new URIImpl("http://purl.org/ontology/bibo/upc", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fc1 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI oclcnum = new URIImpl("http://purl.org/ontology/bibo/oclcnum", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fdf 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI doi = new URIImpl("http://purl.org/ontology/bibo/doi", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fe5 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI asin = new URIImpl("http://purl.org/ontology/bibo/asin", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fd6 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI gtin14 = new URIImpl("http://purl.org/ontology/bibo/gtin14", false);

    /**
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI shortDescription = new URIImpl("http://purl.org/ontology/bibo/shortDescription", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fbe 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI pmid = new URIImpl("http://purl.org/ontology/bibo/pmid", false);

    /**
     * Label: date argued@en 
     * Comment: The date on which a legal case is argued before a court. Date is of format xsd:date@en 
     * Comment: http://purl.org/ontology/bibo/LegalDocument 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI argued = new URIImpl("http://purl.org/ontology/bibo/argued", false);

    /**
     * Label: page start@en 
     * Comment: Starting page number within a continuous page range.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI pageStart = new URIImpl("http://purl.org/ontology/bibo/pageStart", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fbb 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI sici = new URIImpl("http://purl.org/ontology/bibo/sici", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fd0 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI identifier = new URIImpl("http://purl.org/ontology/bibo/identifier", false);

    /**
     * Label: prefix name@en 
     * Comment: The prefix of a name@en 
     * Comment: http://xmlns.com/foaf/0.1/Agent 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI prefixName = new URIImpl("http://purl.org/ontology/bibo/prefixName", false);

    /**
     * Label: pages@en 
     * Comment: A string of non-contiguous page spans that locate a Document within a Collection. Example: 23-25, 34, 54-56. For continuous page ranges, use the pageStart and pageEnd properties.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI pages = new URIImpl("http://purl.org/ontology/bibo/pages", false);

    /**
     * Comment: 43d8f24a:12aef9704d0:-7fc4 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI lccn = new URIImpl("http://purl.org/ontology/bibo/lccn", false);

    /**
     * Label: locator@en 
     * Comment: A description (often numeric) that locates an item within a containing document or collection.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Literal 
     */
    public static final URI locator = new URIImpl("http://purl.org/ontology/bibo/locator", false);

    /**
     * Label: presented at@en 
     * Comment: Relates a document to an event; for example, a paper to a conference.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/Event 
     */
    public static final URI presentedAt = new URIImpl("http://purl.org/ontology/bibo/presentedAt", false);

    /**
     * Label: annotates@en 
     * Comment: Critical or explanatory note for a Document.@en 
     * Comment: http://purl.org/ontology/bibo/Note 
     * Range: http://www.w3.org/2000/01/rdf-schema#Resource 
     */
    public static final URI annotates = new URIImpl("http://purl.org/ontology/bibo/annotates", false);

    /**
     * Label: issuer 
     * Comment: An entity responsible for issuing often informally published documents such as press releases, reports, etc. 
     * Comment: 43d8f24a:12aef9704d0:-7ff1 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI issuer = new URIImpl("http://purl.org/ontology/bibo/issuer", false);

    /**
     * Comment: A legal decision that reverses a ruling.@en 
     * Comment: http://purl.org/ontology/bibo/LegalDecision 
     * Range: http://purl.org/ontology/bibo/LegalDecision 
     */
    public static final URI reversedBy = new URIImpl("http://purl.org/ontology/bibo/reversedBy", false);

    /**
     * Label: list of editors@en 
     * Comment: An ordered list of editors. Normally, this list is seen as a priority list that order editors by importance.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: 43d8f24a:12aef9704d0:-7ff4 
     */
    public static final URI editorList = new URIImpl("http://purl.org/ontology/bibo/editorList", false);

    /**
     * Label: status@en 
     * Comment: The thesis degree.@en 
     * Comment: http://purl.org/ontology/bibo/Thesis 
     * Range: http://purl.org/ontology/bibo/ThesisDegree 
     */
    public static final URI degree = new URIImpl("http://purl.org/ontology/bibo/degree", false);

    /**
     * Comment: A legal decision that affirms a ruling.@en 
     * Comment: http://purl.org/ontology/bibo/LegalDecision 
     * Range: http://purl.org/ontology/bibo/LegalDecision 
     */
    public static final URI affirmedBy = new URIImpl("http://purl.org/ontology/bibo/affirmedBy", false);

    /**
     * Label: translator 
     * Comment: A person who translates written document from one language to another.@en 
     * Comment: 43d8f24a:12aef9704d0:-7fe8 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI translator = new URIImpl("http://purl.org/ontology/bibo/translator", false);

    /**
     * Label: editor 
     * Comment: A person having managerial and sometimes policy-making responsibility for the editorial part of a publishing firm or of a newspaper, magazine, or other publication.@en 
     * Comment: 43d8f24a:12aef9704d0:-7ff7 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI editor = new URIImpl("http://purl.org/ontology/bibo/editor", false);

    /**
     * Comment: The resource in which another resource is reproduced.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/Document 
     */
    public static final URI reproducedIn = new URIImpl("http://purl.org/ontology/bibo/reproducedIn", false);

    /**
     * Label: list of authors@en 
     * Comment: An ordered list of authors. Normally, this list is seen as a priority list that order authors by importance.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: 43d8f24a:12aef9704d0:-8000 
     */
    public static final URI authorList = new URIImpl("http://purl.org/ontology/bibo/authorList", false);

    /**
     * Label: translation of@en 
     * Comment: Relates a translated document to the original document.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/Document 
     */
    public static final URI translationOf = new URIImpl("http://purl.org/ontology/bibo/translationOf", false);

    /**
     * Label: presented at@en 
     * Comment: Relates an event to associated documents; for example, conference to a paper.@en 
     * Comment: http://purl.org/ontology/bibo/Event 
     * Range: http://purl.org/ontology/bibo/Document 
     */
    public static final URI presents = new URIImpl("http://purl.org/ontology/bibo/presents", false);

    /**
     * Comment: A legal decision on appeal that takes action on a case (affirming it, reversing it, etc.).@en 
     * Comment: http://purl.org/ontology/bibo/LegalDecision 
     * Range: http://purl.org/ontology/bibo/LegalDecision 
     */
    public static final URI subsequentLegalDecision = new URIImpl("http://purl.org/ontology/bibo/subsequentLegalDecision", false);

    /**
     * Label: performer 
     * Comment: http://purl.org/ontology/bibo/Performance 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI performer = new URIImpl("http://purl.org/ontology/bibo/performer", false);

    /**
     * Label: interviewee 
     * Comment: An agent that is interviewed by another agent.@en 
     * Comment: http://xmlns.com/foaf/0.1/Agent 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI interviewee = new URIImpl("http://purl.org/ontology/bibo/interviewee", false);

    /**
     * Label: owner@en 
     * Comment: Owner of a document or a collection of documents.@en 
     * Comment: 43d8f24a:12aef9704d0:-7fee 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI owner = new URIImpl("http://purl.org/ontology/bibo/owner", false);

    /**
     * Label: review of@en 
     * Comment: Relates a review document to a reviewed thing (resource, item, etc.).@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Resource 
     */
    public static final URI reviewOf = new URIImpl("http://purl.org/ontology/bibo/reviewOf", false);

    /**
     * Label: recipient 
     * Comment: An agent that receives a communication document.@en 
     * Comment: http://purl.org/ontology/bibo/PersonalCommunicationDocument 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI recipient = new URIImpl("http://purl.org/ontology/bibo/recipient", false);

    /**
     * Label: list of contributors@en 
     * Comment: An ordered list of contributors. Normally, this list is seen as a priority list that order contributors by importance.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: 43d8f24a:12aef9704d0:-7ffd 
     */
    public static final URI contributorList = new URIImpl("http://purl.org/ontology/bibo/contributorList", false);

    /**
     * Label: court@en 
     * Comment: A court associated with a legal document; for example, that which issues a decision.@en 
     * Comment: http://purl.org/ontology/bibo/LegalDocument 
     * Range: http://xmlns.com/foaf/0.1/Organization 
     */
    public static final URI court = new URIImpl("http://purl.org/ontology/bibo/court", false);

    /**
     * Label: cited by@en 
     * Comment: Relates a document to another document that cites the
first document.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/Document 
     */
    public static final URI citedBy = new URIImpl("http://purl.org/ontology/bibo/citedBy", false);

    /**
     * Label: status@en 
     * Comment: The publication status of (typically academic) content.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/DocumentStatus 
     */
    public static final URI status = new URIImpl("http://purl.org/ontology/bibo/status", false);

    /**
     * Label: transcript of@en 
     * Comment: Relates a document to some transcribed original.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://www.w3.org/2000/01/rdf-schema#Resource 
     */
    public static final URI transcriptOf = new URIImpl("http://purl.org/ontology/bibo/transcriptOf", false);

    /**
     * Label: distributor@en 
     * Comment: Distributor of a document or a collection of documents.@en 
     * Comment: 43d8f24a:12aef9704d0:-7ffa 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI distributor = new URIImpl("http://purl.org/ontology/bibo/distributor", false);

    /**
     * Label: interviewer 
     * Comment: An agent that interview another agent.@en 
     * Comment: http://xmlns.com/foaf/0.1/Agent 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI interviewer = new URIImpl("http://purl.org/ontology/bibo/interviewer", false);

    /**
     * Label: cites@en 
     * Comment: Relates a document to another document that is cited
by the first document as reference, comment, review, quotation or for
another purpose.@en 
     * Comment: http://purl.org/ontology/bibo/Document 
     * Range: http://purl.org/ontology/bibo/Document 
     */
    public static final URI cites = new URIImpl("http://purl.org/ontology/bibo/cites", false);

    /**
     * Label: director 
     * Comment: A Film director.@en 
     * Comment: http://purl.org/ontology/bibo/AudioVisualDocument 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI director = new URIImpl("http://purl.org/ontology/bibo/director", false);

    /**
     * Label: producer@en 
     * Comment: Producer of a document or a collection of documents.@en 
     * Comment: 43d8f24a:12aef9704d0:-7feb 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI producer = new URIImpl("http://purl.org/ontology/bibo/producer", false);

    /**
     * Label: organizer@en 
     * Comment: The organizer of an event; includes conference organizers, but also government agencies or other bodies that are responsible for conducting hearings.@en 
     * Comment: http://purl.org/NET/c4dm/event.owl#Event 
     * Range: http://xmlns.com/foaf/0.1/Agent 
     */
    public static final URI organizer = new URIImpl("http://purl.org/ontology/bibo/organizer", false);

}
