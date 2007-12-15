/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.resourceIndex;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.trippi.RDFFormat;
import org.trippi.RDFUtil;
import org.trippi.TripleIterator;

import fedora.common.Constants;
import fedora.common.PID;
import fedora.common.rdf.RDFName;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.BDefReader;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.MethodDef;
import fedora.server.utilities.DCFields;
import fedora.server.utilities.DateUtility;

/**
 * Generates base RDF triples for Fedora objects.
 *
 * @author cwilper@cs.cornell.edu
 */
public class BaseTripleGenerator implements Constants, TripleGenerator {

    /**
     * The factory this instance will use for creating JRDF objects.
     */
    private GraphElementFactory _geFactory;

    /**
     * Constructor.
     */
    public BaseTripleGenerator(GraphElementFactory geFactory) {
        if (geFactory != null) {
            _geFactory = geFactory;
        } else {
            _geFactory = new RDFUtil();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForBDef(BDefReader reader)
            throws ResourceIndexException {

        Set<Triple> set = new HashSet<Triple>();

        URIReference objURI = addCommonTriples(reader, set);
        add(objURI, RDF.TYPE, MODEL.BDEF_OBJECT, set);
        addMethodDefTriples(objURI, reader, set);

        return set;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForDataObject(DOReader reader)
            throws ResourceIndexException {

        Set<Triple> set = new HashSet<Triple>();

        URIReference objURI = addCommonTriples(reader, set);
        add(objURI, RDF.TYPE, MODEL.DATA_OBJECT, set);

        return set;
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForCModelObject(DOReader reader)
            throws ResourceIndexException {

        Set<Triple> set = new HashSet<Triple>();

        URIReference objURI = addCommonTriples(reader, set);
        add(objURI, RDF.TYPE, MODEL.CMODEL_OBJECT, set);
        addContentModelTriples(objURI, reader, set);

        return set;
    }

    /**
     * Add a "defines" statement for the given bDef for each abstract
     * method it defines.
     */
    private void addMethodDefTriples(URIReference objURI,
                                     BDefReader reader,
                                     Set<Triple> set)
            throws ResourceIndexException {
        try {
            MethodDef[] methodDefs = reader.getAbstractMethods(null);
            for (int i = 0; i < methodDefs.length; i++) {
                add(objURI, MODEL.DEFINES_METHOD, methodDefs[i].methodName, set);
            }
        } catch (ResourceIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceIndexException("Error adding method def "
                    + "triples", e);
        }
    }

    /**
     * Add an "implements" statement for the given content model.
     */
    private void addContentModelTriples(URIReference objURI,
                                          DOReader reader,
                                          Set<Triple> set)
            throws ResourceIndexException 
    {
        //  Are there any content model specific triples ??
    }

    /**
     * Add the common core and datastream triples for the given object.
     */
    private URIReference addCommonTriples(DOReader reader, Set<Triple> set)
            throws ResourceIndexException {

        try {
            
            URIReference objURI = createResource(PID.toURI(reader.GetObjectPID()));

            addCoreObjectTriples(reader, objURI, set);

            addAllDatastreamTriples(reader.GetDatastreams(null, null), objURI, 
                    set);

            return objURI;

        } catch (ResourceIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceIndexException("Error generating triples", e);
        }
    }

    /**
     * For the given object, add the common core system metadata triples.
     *
     * This will include:
     * <ul>
     *   <li> object <i>model:hasContentModel</i></li>
     *   <li> object <i>model:createdDate</i></li>
     *   <li> object <i>model:label</i></li>
     *   <li> object <i>model:owner</i></li>
     *   <li> object <i>model:state</i></li>
     *   <li> object <i>view:lastModifiedDate</i></li>
     * </ul>
     */
    private void addCoreObjectTriples(DOReader r,
                                      URIReference objURI,
                                      Set<Triple> set) throws Exception {
        add(objURI, MODEL.HAS_CONTENT_MODEL,
                _geFactory.createResource(new URI(r.getContentModelId())),
                set);
        add(objURI, MODEL.CREATED_DATE,       r.getCreateDate(), set);
        add(objURI, MODEL.LABEL,              r.GetObjectLabel(), set);
        add(objURI, MODEL.OWNER,              r.getOwnerId(), set);
        add(objURI, MODEL.STATE,              getStateResource(
                                              r.GetObjectState()), set);
        add(objURI, VIEW.LAST_MODIFIED_DATE,  r.getLastModDate(), set);
    }

    /**
     * For the given datastream, add the triples that are common for all
     * datastreams.
     *
     * This will include:
     * <ul>
     *   <li> object     <i>view:disseminates</i> datastream</li>
     *   <li> datastream <i>view:disseminationType</i></li>
     *   <li> datastream <i>view:isVolatile</i></li>
     *   <li> datastream <i>view:lastModifiedDate</i></li>
     *   <li> datastream <i>view:mimeType</i></li>
     *   <li> datastream <i>model:state</i></li>
     * </ul>
     */
    private void addCoreDatastreamTriples(Datastream ds,
                                          URIReference objURI,
                                          Set<Triple> set)
            throws Exception {

        URIReference dsURI = createResource(objURI.getURI().toString() 
                + "/" + ds.DatastreamID);

        add(objURI, VIEW.DISSEMINATES, dsURI, set);

        URIReference dsDissType = createResource(FEDORA.uri + "*/" 
                + ds.DatastreamID);

        add(dsURI, VIEW.DISSEMINATION_TYPE, dsDissType, set);

        boolean isVolatile = ds.DSControlGrp.equals("E") 
                          || ds.DSControlGrp.equals("R");

        add(dsURI, VIEW.IS_VOLATILE, isVolatile, set);
        add(dsURI, VIEW.LAST_MODIFIED_DATE, ds.DSCreateDT, set);
        add(dsURI, VIEW.MIME_TYPE, ds.DSMIME, set);
        add(dsURI, MODEL.STATE, getStateResource(ds.DSState), set);

    }

    /**
     * Add a statement about the object for each predicate, value
     * pair expressed in the DC datastream.
     */
    private void addDCTriples(DatastreamXMLMetadata ds,
                              URIReference objURI,
                              Set<Triple> set) throws Exception {
        DCFields dc = new DCFields(ds.getContentStream());
        Map<RDFName, List<String>> map = dc.getMap();
        for (RDFName predicate : map.keySet()) {
            for (String value : map.get(predicate)) {
                add(objURI, predicate, value, set);
            }
        }
    }


    /**
     * Add all triples found in the RELS-EXT datastream, skipping any with
     * predicate fedora-model:hasContentModel (this is indexed as a "core"
     * triple).
     */
    private void addRELSEXTTriples(DatastreamXMLMetadata ds,
                                   Set<Triple> set) throws Exception {
        TripleIterator iter = TripleIterator.fromStream(ds.getContentStream(),
                                                        RDFFormat.RDF_XML);
        try {
            while (iter.hasNext()) {
                Triple triple = iter.next();
                URIReference u = (URIReference) triple.getPredicate();
                if (!u.getURI().toString().equals(MODEL.HAS_CONTENT_MODEL)) {
                    set.add(triple);
                }
            }
        } finally {
            iter.close();
        }

    }

    /**
     * Add all triples whose values are determined by datastream
     * metadata or content.
     */
    private void addAllDatastreamTriples(Datastream[] datastreams,
                                         URIReference objURI,
                                         Set<Triple> set) 
            throws Exception {

        for (int i = 0; i < datastreams.length; i++) {

            Datastream ds = datastreams[i];

            // triples determined by datastream's metadata
            addCoreDatastreamTriples(ds, objURI, set);

            // triples determined by parsing the datastream's content
            if (ds.DatastreamID.equals("DC")) {
                addDCTriples((DatastreamXMLMetadata) ds, objURI, set);
            } else if (ds.DatastreamID.equals("RELS-EXT")) {
                addRELSEXTTriples((DatastreamXMLMetadata) ds, set);
            }

        }
    }

    // Helper methods for creating RDF components

    protected URIReference createResource(String uri) throws Exception {
        return _geFactory.createResource(new URI(uri));
    }

    protected RDFName getStateResource(String state)
            throws ResourceIndexException {
        if (state == null) {
            throw new ResourceIndexException("State cannot be null");
        } else if (state.equals("A")) {
            return MODEL.ACTIVE;
        } else if (state.equals("D")) {
            return MODEL.DELETED;
        } else if (state.equals("I")) {
            return MODEL.INACTIVE;
        } else {
            throw new ResourceIndexException("Unrecognized state: " + state);
        }
    }

    // Helper methods for adding triples

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       RDFName object,
                       Set<Triple> set) throws ResourceIndexException {
        try {
            add(subject, 
                    predicate, 
                    _geFactory.createResource(object.getURI()),
                    set);
        } catch (GraphElementFactoryException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }

    protected void add(SubjectNode subject,
            RDFName predicate,
            ObjectNode object,
            Set<Triple> set) throws ResourceIndexException {
        try {
            set.add(_geFactory.createTriple(subject, 
                    _geFactory.createResource(predicate.getURI()), 
                    object));
        } catch (GraphElementFactoryException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       String lexicalValue,
                       Set<Triple> set) throws Exception {
        if (lexicalValue != null) {
            set.add(_geFactory.createTriple(subject, 
                    _geFactory.createResource(predicate.getURI()), 
                    _geFactory.createLiteral(lexicalValue)));
        }
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       Date dateValue,
                       Set<Triple> set) throws Exception {
        if (dateValue != null) {
            String lexicalValue = DateUtility.convertDateToXSDString(dateValue);
            ObjectNode object = _geFactory.createLiteral(lexicalValue, 
                    RDF_XSD.DATE_TIME.getURI());
            set.add(_geFactory.createTriple(subject, 
                    _geFactory.createResource(predicate.getURI()), 
                    object));
        }
    }
    
    protected void add(SubjectNode subject, 
            RDFName predicate,
            boolean booleanValue, 
            Set<Triple> set) throws Exception {
        add(subject, predicate, Boolean.toString(booleanValue), set);
    }
}
