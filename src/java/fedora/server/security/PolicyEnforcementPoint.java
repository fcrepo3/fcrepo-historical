package fedora.server.security;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;
import com.sun.xacml.attr.StringAttribute;
import java.util.ArrayList; 
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;
import com.sun.xacml.finder.PolicyFinder;
import fedora.server.Context;
import fedora.server.errors.AuthzOperationalException;
import fedora.server.errors.NotAuthorizedException;
import fedora.server.errors.GeneralException;
import fedora.server.storage.DOManager;

public class PolicyEnforcementPoint {
	
	public static final String SUBACTION_SEPARATOR = "//"; 
	public static final String SUBRESOURCE_SEPARATOR = "//";
	
	private static PolicyEnforcementPoint singleton = null;
	private static int count = 0;
	
	private String enforceMode = ENFORCE_MODE_ENFORCE_POLICIES;
	static final String ENFORCE_MODE_ENFORCE_POLICIES = "enforce-policies";
	static final String ENFORCE_MODE_PERMIT_ALL_REQUESTS = "permit-all-requests";
	static final String ENFORCE_MODE_DENY_ALL_REQUESTS = "deny-all-requests";	

	private PolicyEnforcementPoint() {
		URI subjectIdUri = null;
		URI actionIdUri = null;
		URI actionApiUri = null;
		URI contextUri = null;
		URI pidUri = null;
		URI namespaceUri = null;
		try {
			subjectIdUri = new URI(Authorization.SUBJECT_ID_URI_STRING);
			actionIdUri = new URI(Authorization.ACTION_ID_URI_STRING);
			actionApiUri = new URI(Authorization.ACTION_API_URI_STRING);
			contextUri = new URI(Authorization.ACTION_CONTEXT_URI_STRING);
			pidUri = new URI(Authorization.RESOURCE_ID_URI_STRING);
			namespaceUri = new URI(Authorization.RESOURCE_NAMESPACE_URI_STRING);
			System.err.println("all uris set, no throws");
		} catch (URISyntaxException e) {
			System.err.println("***throw in XACMLPep constructor");
		} finally {
			SUBJECT_ID_URI = subjectIdUri;
			ACTION_ID_URI = actionIdUri;
			ACTION_API_URI = actionApiUri;
			ACTION_CONTEXT_URI = contextUri;
			RESOURCE_ID_URI = pidUri;
			RESOURCE_NAMESPACE_URI = namespaceUri;
		}
	}
	
	public static final PolicyEnforcementPoint getInstance() {
		if (singleton == null) {
			singleton = new PolicyEnforcementPoint();
		}
		count++;
		System.err.println ("***another use (" + count + ") of XACMLPep singleton");
		return singleton;
	}

	/**
	 * xacml pdp
	 */
	private PDP pdp = null;

	/**
	 * available during init(); keep as logging hook
	 */
	private ServletContext servletContext = null;
	
	private ContextAttributeFinderModule contextAttributeFinder;
	
	public void initPep(String enforceMode, String combiningAlgorithm, String globalPolicyConfig, String localPolicyConfig, DOManager manager
	) throws Exception {
		System.err.println ("***initPep()");
		destroy();

		this.enforceMode = enforceMode;
		if (ENFORCE_MODE_ENFORCE_POLICIES.equals(enforceMode)) {
		} else if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(enforceMode)) {
		} else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(enforceMode)) {
		} else {
			log("configuration error -- bad enforceModeString parm, will enforce policies by default");
		}

		AttributeFinder attrFinder = new AttributeFinder();
		List attrModules = new ArrayList();
		//AttributeFinderModule attrModule = SSIAttributeFinder.getInstance(servletContext);
		//attrModules.add(attrModule);
		
		ResourceAttributeFinderModule resourceAttributeFinder = ResourceAttributeFinderModule.getInstance();
		resourceAttributeFinder.setServletContext(servletContext);
		resourceAttributeFinder.setDOManager(manager);
		attrModules.add(resourceAttributeFinder);		

		contextAttributeFinder = ContextAttributeFinderModule.getInstance();
		contextAttributeFinder.setServletContext(servletContext);
		attrModules.add(contextAttributeFinder);		

		
		
		//=>>AttributeFinderModule resourceAttributeModule = new FedoraObjectAttributeFinder(manager, servletContext);
		//==>>attrModules.add(resourceAttributeModule);

		attrFinder.setModules(attrModules);		
System.err.println("before building policy finder");
		PolicyFinder policyFinder = new PolicyFinder();
		System.err.println("just constructed policy finder");
		Set policyModules = new HashSet();
		System.err.println("just constructed policy module hashset");
		PolicyFinderModule combinedPolicyModule = null;
		try {
			System.err.println("***before constucting fedora policy finder module");
			combinedPolicyModule = new PolicyFinderModule(combiningAlgorithm, globalPolicyConfig, localPolicyConfig, manager);
			
			System.err.println("after constucting fedora policy finder module");
		} catch (GeneralException e) {
System.err.println("***debugging CombinedPolicyModule");
			e.printStackTrace();
		}
		System.err.println("before adding fedora policy finder module to policy finder hashset");
		policyModules.add(combinedPolicyModule);
		System.err.println("after adding fedora policy finder module to policy finder hashset");
		System.err.println("before setting policy finder hashset into policy finder");
		policyFinder.setModules(policyModules);
		System.err.println("after setting policy finder hashset into policy finder");
		
		PDP pdp = null;
		System.err.println(PolicyFinderModule.getClassErrors() + "class errors");
		if (PolicyFinderModule.getClassErrors() == 0) {
			System.err.println("0 class errors");
			pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));
		}	
		if (pdp == null) {
			System.err.println("null pdp");
			Exception se = new Exception("Xaclmpep.init() failed:  no pdp");
			servletContext.log(se.getMessage());
			throw se;
		}
		this.pdp = pdp;
		System.err.println ("***ending initPep()");
	}

	public void inactivate() {
		destroy();
	}
	
	public void destroy() {
		servletContext = null;
		pdp = null;
	}



	
	/*
	 * 			//there was some failure to initialize
			if ("shutdown".equals(actionId)) {
				authz = true;
			}

	 */
	
	private final Set wrapSubjects(String subjectId) {
		System.err.println("wrapSubjectIdAsSubjects(): " + subjectId);
		if ((subjectId == null) || ("".equals(subjectId))) {
			subjectId = Authorization.UNDEFINED;
		}
		StringAttribute stringAttribute = new StringAttribute(subjectId);
		Attribute singleSubjectAttribute = new Attribute(SUBJECT_ID_URI, null, null, stringAttribute);
		System.err.println("wrapSubjectIdAsSubjects(): singleSubjectAttribute, id=" + singleSubjectAttribute.getId() + ", type=" + singleSubjectAttribute.getType() + ", value=" + singleSubjectAttribute.getValue());
		Set subjectAttributes = new HashSet();
		subjectAttributes.add(singleSubjectAttribute);
		
		/*
		Iterator it = context.names();
		while (it.hasNext()) {
			String name = (String) it.next();
			if (name.indexOf(":") == -1) {
				String value = context.get(name);
				try {
					singleSubjectAttribute = new Attribute(new URI(name), null, null, new StringAttribute(value));
				} catch (URISyntaxException e1) {
				}
				subjectAttributes.add(singleSubjectAttribute);				
			}
		}
		*/
		
		/*
		if (roles != null) {
			for (int i=0; i<roles.length; i++) {
				String[] parts = parseRole(roles[i]);
				if ((parts == null) || (parts.length == 0)|| (parts[0] == null)) {
					System.err.println("no attributes for subjectId=" + subjectId + " for roles[" + i + "]=" + roles[i]);
				} else {
					if ((parts[1] == null) || "".equals(parts[1])) {
						parts[1] = "X";
					}
					System.err.println("XXXXXXXXXXXXX " + i + " " + parts[0] + "value i.e. parts[1] = " + parts[1]);
					try {
						singleSubjectAttribute = new Attribute(new URI(parts[0]), null, null, new StringAttribute(parts[1]));
					} catch (URISyntaxException e1) {
						throw new AuthzOperationalException("couldn't wrap subject roles", e1);
					}
					subjectAttributes.add(singleSubjectAttribute);			
				}
			}			
		}
*/
		Subject singleSubject = new Subject(subjectAttributes);
		Set subjects = new HashSet();
		subjects.add(singleSubject);
		return subjects;
	}

	
	private final URI SUBJECT_ID_URI;
	private final URI ACTION_ID_URI;
	private final URI ACTION_API_URI;
	private final URI ACTION_CONTEXT_URI;
	private final URI RESOURCE_ID_URI;
	private final URI RESOURCE_NAMESPACE_URI;


	
	private final Set wrapActions(String actionId, String actionApi, String contextIndex) {
		Set actions = new HashSet();
		Attribute action = new Attribute(ACTION_ID_URI, null, null, new StringAttribute(actionId));
		actions.add(action);
		action = new Attribute(ACTION_API_URI, null, null, new StringAttribute(actionApi));
		actions.add(action);
		action = new Attribute(ACTION_CONTEXT_URI, null, null, new StringAttribute(contextIndex));
		actions.add(action);
		return actions;
	}
	
	
	private final Set wrapResources(String pid, String namespace) throws AuthzOperationalException {
		Set resources = new HashSet();
		Attribute attribute = null;
		attribute = new Attribute(RESOURCE_ID_URI, null, null, new StringAttribute(pid));
		resources.add(attribute);
		attribute = new Attribute(RESOURCE_NAMESPACE_URI, null, null, new StringAttribute(namespace));
		resources.add(attribute);
		return resources;
	}

/*
	private final Set wrapEnvironment(Context context) throws AuthzOperationalException {
		Set environment = new HashSet();
		Attribute attribute = null;
		attribute = new Attribute(ENVIRONMENT_CURRENT_DATETIME_URI, null, null, new DateTimeAttribute()); //<<<<<<<<<<<<<<<<<<<<<<<<<<<
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_CURRENT_DATE_URI, null, null, new DateAttribute()); //<<<<<<<<<<<<<<<<<<<<<<<<<<<
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_CURRENT_TIME_URI, null, null, new TimeAttribute()); //<<<<<<<<<<<<<<<<<<<<<<<<<<<
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_PROTOCOL_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_PROTOCOL_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_SCHEME_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_SCHEME_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_SECURITY_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_SECURITY_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_AUTHTYPE_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_AUTHTYPE_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_METHOD_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_METHOD_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_SESSION_ENCODING_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_SESSION_ENCODING_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_SESSION_STATUS_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_SESSION_STATUS_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_CONTENT_LENGTH_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_CONTENT_LENGTH_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_CONTENT_TYPE_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_CONTENT_TYPE_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_REQUEST_SOAP_OR_REST_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_REQUEST_SOAP_OR_REST_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_CLIENT_FQDN_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_CLIENT_FQDN_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_CLIENT_IP_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_CLIENT_IP_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_SERVER_FQDN_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_SERVER_FQDN_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_SERVER_IP_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_SERVER_IP_URI_STRING)));
		environment.add(attribute);
		attribute = new Attribute(ENVIRONMENT_SERVER_PORT_URI, null, null, new StringAttribute(context.get(ENVIRONMENT_SERVER_PORT_URI_STRING)));
		environment.add(attribute);
		return environment;
	}
*/

	private int n = 0;
	
	private synchronized int next() {
		return n++;
	}

	private final Set NULL_SET = new HashSet();

	public final void enforce(String subjectId, String action, String api, String pid, String namespace, Context context)  throws NotAuthorizedException {
		if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(enforceMode)) {
			log("permitting request because enforceMode==ENFORCE_MODE_PERMIT_ALL_REQUESTS");
		} else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(enforceMode)) {
			log("denying request because enforceMode==ENFORCE_MODE_DENY_ALL_REQUESTS");
			throw new NotAuthorizedException("all requests are currently denied");				
		} else {
			log("enforcing policies by default");
			ResponseCtx response = null;
			String contextIndex = null;
			try {
				contextIndex = (new Integer(next())).toString();
				log("context index set=" + contextIndex);
				Set subjects = wrapSubjects(subjectId);
				Set actions = wrapActions(action, api, contextIndex);
				Set resources = wrapResources(pid, namespace);

				RequestCtx request = new RequestCtx(subjects, resources, actions, NULL_SET);
				Set tempset = request.getAction();
				Iterator tempit = tempset.iterator();
				while (tempit.hasNext()) {
					Attribute tempobj = (Attribute) tempit.next();
					log("request action has " + tempobj.getId() + "=" + tempobj.getValue().toString());
				}
				/*
				Set testSubjects = request.getSubjects();
				Iterator testIt = testSubjects.iterator();
				while (testIt.hasNext()) {
					Subject testSubject = (Subject) testIt.next();
					System.err.println("testSubject=" + testSubject);
					Set testAttributes = testSubject.getAttributes();
					Iterator testIt2 = testAttributes.iterator();
					while (testIt2.hasNext()) {
						Attribute testAttribute = (Attribute) testIt2.next();
						System.err.println("testAttribute=" + testAttribute);
						AttributeValue attributeValue = testAttribute.getValue();
						System.err.println("attributeValue=" + attributeValue);
						System.err.println("attributeValue.toString()=" + attributeValue.toString());
					}
				}
				*/ /*
				System.err.println("vvv environment vvv");
				Set testEnvironmentAttributes = request.getEnvironmentAttributes();
				Iterator testIt2 = testEnvironmentAttributes.iterator();
				while (testIt2.hasNext()) {
					Attribute testAttribute = (Attribute) testIt2.next();
					URI testAttributeId = testAttribute.getId();
					AttributeValue testAttributeValue = testAttribute.getValue();
					System.err.println("test env attributeId=" + testAttributeId);
					System.err.println("test env attributeValue=" + testAttributeValue);
					System.err.println("test env attributeValue.toString()=" + testAttributeValue.toString());
				}
				*/
				Logger logger = Logger.getLogger("com.sun.xacml");
				logger.setLevel(Level.ALL);
				contextAttributeFinder.registerContext(contextIndex, context);
				response = pdp.evaluate(request);
				System.err.println("in pep, after evaluate() called");
			} catch (Throwable t) {
				System.err.println("got me throwable:");			
				t.printStackTrace();			
				throw new AuthzOperationalException("");
			} finally {
				contextAttributeFinder.unregisterContext(contextIndex);
			}
			System.err.println("in pep, before denyBiasedAuthz() called");
			if (! denyBiasedAuthz(response.getResults())) {
				throw new NotAuthorizedException("");
			}			
		}		
	}
	
	private static final boolean denyBiasedAuthz(Set set) {
		int nPermits = 0; //explicit permit returned
		int nDenies = 0; //explicit deny returned
		int nNotApplicables = 0; //no targets matched
		int nIndeterminates = 0; //for targets matched, no rules matched
		int nWrongs = 0; //none of the above, i.e., unreported failure, should not happen
		Iterator it = set.iterator();
		int which = 0;
		while (it.hasNext()) {
			Result result = (Result) it.next();
			int decision = result.getDecision();
			switch (decision) {
				case Result.DECISION_PERMIT:
					nPermits++;
					break;
				case Result.DECISION_DENY:
					nDenies++;
					break;
				case Result.DECISION_INDETERMINATE:
					nIndeterminates++;
					break;
				case Result.DECISION_NOT_APPLICABLE:
					nNotApplicables++;
					break;
				default:
					nWrongs++;
					break;
			}
			//System.err.println("AUTHZ:  which=" + which + " resource=" + result.getResource() + " toString()=" + result.toString());			
		}
		System.err.println("AUTHZ:  permits=" + nPermits + " denies=" + nDenies + " indeterminates=" + nIndeterminates + " notApplicables=" + nNotApplicables + " unexpecteds=" + nWrongs);			
		return (nPermits >= 1) && (nDenies == 0) && (nIndeterminates == 0) && (nWrongs == 0); // don't care about NotApplicables
	}

	private final void log(String msg) {
		if (servletContext != null) {
			servletContext.log(msg);
		} else {
			System.err.println(msg);			
		}
	}

	
}




