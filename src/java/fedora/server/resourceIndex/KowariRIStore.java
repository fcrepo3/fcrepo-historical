package fedora.server.resourceIndex;

import java.io.InputStream;
import java.io.OutputStream;

import fedora.server.errors.ResourceIndexException;
import fedora.server.errors.UnsupportedQueryLanguageException;

import org.jrdf.graph.Triple;
import org.kowari.itql.ItqlInterpreterBean;
import org.kowari.query.Answer;
import org.kowari.query.QueryException;
import org.kowari.query.rdf.LiteralImpl;
import org.kowari.query.rdf.TripleImpl;
import org.kowari.query.rdf.URIReferenceImpl;
import org.kowari.server.Session;
import org.kowari.store.jena.KowariQueryEngine;
import org.kowari.store.jena.RdqlQuery;
import org.kowari.itql.ItqlInterpreterException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdql.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author eddie
 *  
 */
public class KowariRIStore implements RIStore {
	private Session m_session;
	private Model m_model;
	private URI m_fullTextModelURI;
	
	//URI rdql = new URI("http://jena.hpl.hp.com/2003/07/query/RDQL");
	private Set m_supportedQueryLanguages;
	
	/**
	 * This constructor disables full text indexing of literals.
	 * @param session
	 * @param model
	 */
	public KowariRIStore(Session session, Model model) {
	    this(session, model, null);
	}
	
	public KowariRIStore(Session session, Model model, URI fullTextModelURI) {
		m_session = session;
		m_model = model;
		m_fullTextModelURI = fullTextModelURI;
		m_supportedQueryLanguages = new HashSet();
		m_supportedQueryLanguages.add("rdql");
		m_supportedQueryLanguages.add("itql");
	}
	
	/* (non-Javadoc)
     * @see fedora.server.resourceIndex.RIStore#getSupportedQueryLanguages()
     */
    public Set getSupportedQueryLanguages() {
        return m_supportedQueryLanguages;
    }

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#read(java.io.InputStream, java.lang.String)
	 */
	public void read(InputStream rdfxml, String base) {
		m_model.read(rdfxml, base);
	}
	
    /* (non-Javadoc)
     * @see fedora.server.resourceIndex.RIStore#executeQuery(fedora.server.resourceIndex.RIQuery)
     */
    public RIResultIterator executeQuery(RIQuery query) throws ResourceIndexException {
        String queryLanguage = query.getQueryLanguage();
        if (m_supportedQueryLanguages.contains(queryLanguage) && queryLanguage.equals("rdql")) {
            return executeQuery((RDQLQuery)query);
        } else if (m_supportedQueryLanguages.contains(queryLanguage) && queryLanguage.equals("itql")) {
            return executeQuery((ITQLQuery)query);
        } else {
            throw new UnsupportedQueryLanguageException(queryLanguage + " is not supported.");
        }
    }
    
    /* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#insert(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void insert(String subject, String predicate, String object) {
		m_model.add(m_model.createResource(subject), 
				    m_model.createProperty(predicate), 
					m_model.createResource(object));
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#insertLiteral(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void insertLiteral(String subject, String predicate, String object) {
		m_model.add(m_model.createResource(subject), 
			        m_model.createProperty(predicate), 
				    m_model.createLiteral(object));
		
		insertLiteralIntoFullTextModel(subject, predicate, object);
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#insertTypedLiteral(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void insertTypedLiteral(String subject, String predicate, String object, String datatype) {
		m_model.add(m_model.createResource(subject), 
			        m_model.createProperty(predicate), 
				    m_model.createTypedLiteral(object, datatype));
		
		insertLiteralIntoFullTextModel(subject, predicate, object);
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#insertLocalLiteral(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void insertLocalLiteral(String subject, String predicate, String object, String language) {
		m_model.add(m_model.createResource(subject), 
		            m_model.createProperty(predicate), 
		            m_model.createLiteral(object, language));
		
		insertLiteralIntoFullTextModel(subject, predicate, object);
	}
	
	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.RIStore#write(java.io.OutputStream)
	 */
	public void write(OutputStream rdfxml) {
		m_model.write(rdfxml);
	}
    
    private JenaResultIterator executeQuery(RDQLQuery query) {
	    RdqlQuery q = new RdqlQuery(query.getQuery());
		q.setSource(m_model);
		QueryExecution qe = new KowariQueryEngine(q);
		QueryResults results = qe.exec();
		return new JenaResultIterator(results);
	}
	
	private KowariResultIterator executeQuery(ITQLQuery query) throws ResourceIndexException {
	    try {
			ItqlInterpreterBean interpreter = new ItqlInterpreterBean(m_session, null);
			Answer answer = interpreter.executeQuery(query.getQuery());
			return new KowariResultIterator(answer);
		} catch (ItqlInterpreterException iie) {
		    iie.printStackTrace();
			throw new ResourceIndexException(iie.getMessage());
		}
	}
	
	public void insertLiteralIntoFullTextModel(String subject, String predicate, String object) {
		if (m_fullTextModelURI == null) {
		    return;
		}
		
		try {
			URI s = new URI(subject);
			URI p = new URI(predicate);			
		    Triple t = new TripleImpl(new URIReferenceImpl(s),
		            				  new URIReferenceImpl(p),
		            				  new LiteralImpl(object));
		    Set statements = new HashSet();
		    statements.add(t);
            m_session.insert(m_fullTextModelURI, statements);
		} catch (URISyntaxException ue) {
		    // TODO Auto-generated catch block
            ue.printStackTrace();
        } catch (QueryException qe) {
            // TODO Auto-generated catch block
            qe.printStackTrace();
        }
	}
}