/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.types;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * <p><b>Title:</b> BasicDigitalObject.java</p>
 * <p><b>Description:</b> A basic implementation of DigitalObject that stores
 * things in memory.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class BasicDigitalObject
        implements DigitalObject {

    private boolean m_isNew;
    private String m_fedoraObjectType;
    private String m_pid;
    //private String m_uri;
    private String m_state;
    private String m_ownerId;
    private String m_label;
    private String m_contentModelId;
    private Date m_createDate;
    private Date m_lastModDate;
    private ArrayList m_auditRecords;
    private HashMap<String, ArrayList<Datastream>> m_datastreams;
    //used for deserializing old-style objects for conversion to CMDA-style objects
    private HashMap<String, ArrayList<Disseminator>> m_disseminators;
    private Map m_prefixes;
	private Map<String, String> m_extProperties;

    public BasicDigitalObject() {
        m_auditRecords=new ArrayList();
        m_datastreams=new HashMap<String, ArrayList<Datastream>>();
        // only used for translating old-style disseminator objects to CMDA objects
        m_disseminators = new HashMap<String, ArrayList<Disseminator>>();
        m_extProperties=new HashMap<String, String>();
		setNew(false);
        setContentModelId("");
        m_fedoraObjectType = "";
    }

    public boolean isNew() {
	    return m_isNew;
	}

    public void setNew(boolean isNew) {
	    m_isNew=isNew;
	}
    
    public boolean isFedoraObjectType(int type)
    {
        return(m_fedoraObjectType.indexOf(type) != -1);
    }


    public String getFedoraObjectTypes() 
    {
        return m_fedoraObjectType;
    }

    public void addFedoraObjectType(int type) 
    {
        if (m_fedoraObjectType.indexOf(type) == -1)
        {
            m_fedoraObjectType = m_fedoraObjectType + (char)type;
        }
    }
    
    public void removeFedoraObjectType(int type) 
    {
        if (m_fedoraObjectType.indexOf(type) != -1)
        {
            m_fedoraObjectType = m_fedoraObjectType.replaceAll(""+(char)type, "");
        }
    }

    public String getPid() {
        return m_pid;
    }

    public void setPid(String pid) {
        m_pid=pid;
    }
	
    public String getState() {
        return m_state;
    }

    public void setState(String state) {
        m_state=state;
    }

    public String getOwnerId() {
        return m_ownerId;
    }

    public void setOwnerId(String owner) {
        m_ownerId=owner;
    }

    public String getLabel() {
        return m_label;
    }

    public void setLabel(String label) {
        m_label=label;
    }

    public String getContentModelId() {
        return m_contentModelId;
    }

    public void setContentModelId(String id) {
        m_contentModelId=id;
    }

    public Date getCreateDate() {
        return m_createDate;
    }

    public void setCreateDate(Date createDate) {
        m_createDate=createDate;
    }

    public Date getLastModDate() {
        return m_lastModDate;
    }

    public void setLastModDate(Date lastModDate) {
        m_lastModDate=lastModDate;
    }

    public void setNamespaceMapping(Map mapping) {
        m_prefixes=mapping;
    }

    public Map getNamespaceMapping() {
        return m_prefixes;
    }

    public List getAuditRecords() {
        return m_auditRecords;
    }

    public Iterator datastreamIdIterator() {
        return copyOfKeysForNonEmptyLists(m_datastreams).iterator();
    }

    private static Set copyOfKeysForNonEmptyLists(HashMap map) {
        HashSet<String> set=new HashSet<String>();
        Iterator iter=map.keySet().iterator();
        while (iter.hasNext()) {
            String key=(String) iter.next();
            List list=(List) map.get(key);
            if (list.size()>0) {
                set.add(key);
            }
        }
        return set;
    }

    public List<Datastream> datastreams(String id) {
        ArrayList<Datastream> ret=(ArrayList<Datastream>) m_datastreams.get(id);
        if (ret==null) {
            ret=new ArrayList<Datastream>();
            m_datastreams.put(id, ret);
        }
        return ret;
    }

    public void addDatastreamVersion(Datastream ds, boolean addNewVersion)
    {
    	List<Datastream> datastreams = datastreams(ds.DatastreamID);
    	if (!addNewVersion)
    	{
            Iterator dsIter = datastreams.iterator();
            Datastream latestCreated = null;
            long latestCreateTime=-1;
    	    while (dsIter.hasNext()) 
    	    {
    	    	Datastream ds1 =(Datastream) dsIter.next();
    	        if (ds1.DSCreateDT.getTime() > latestCreateTime) 
    	        {
    	        	latestCreateTime = ds1.DSCreateDT.getTime();
    	            latestCreated = ds1;
    	        }
    	    }
    	    datastreams.remove(latestCreated);
    	}
   		datastreams.add(ds);
    }
    
    public Iterator disseminatorIdIterator() 
    {
        return copyOfKeysForNonEmptyLists(m_disseminators).iterator();
    }

    public List<Disseminator> disseminators(String id) 
    {
        ArrayList<Disseminator> ret=(ArrayList<Disseminator>) m_disseminators.get(id);
        if (ret == null) 
        {
            ret = new ArrayList<Disseminator>();
            m_disseminators.put(id, ret);
        }
        return ret;
    }

    public String newDatastreamID() {
        return newID(datastreamIdIterator(), "DS");
    }

    public String newDatastreamID(String id) {
        ArrayList<String> versionIDs = new ArrayList<String>();
        Iterator iter=((ArrayList) m_datastreams.get(id)).iterator();
        while (iter.hasNext()) {
            Datastream ds=(Datastream) iter.next();
            versionIDs.add(ds.DSVersionID);
        }
        return newID(versionIDs.iterator(), id + ".");
    }

//    public String newDisseminatorID() {
//        return newID(disseminatorIdIterator(), "DISS");
//    }
//
//    public String newDisseminatorID(String id) {
//        ArrayList<String> versionIDs=new ArrayList<String>();
//        Iterator iter=((ArrayList) m_disseminators.get(id)).iterator();
//        while (iter.hasNext()) {
//            Disseminator diss=(Disseminator) iter.next();
//            versionIDs.add(diss.dissVersionID);
//        }
//        return newID(versionIDs.iterator(), id + ".");
//    }
//
//    public String newDatastreamBindingMapID() {
//        ArrayList<String> mapIDs=new ArrayList<String>(); // the list we'll put
//                                          // allbinding map ids in
//        Iterator dissIter=m_disseminators.keySet().iterator();
//        // for every List of disseminators...
//        while (dissIter.hasNext()) {
//            // get the dissID
//            String id=(String) dissIter.next();
//            Iterator iter=((ArrayList) m_disseminators.get(id)).iterator();
//            // then for every version with that id...
//            while (iter.hasNext()) {
//                Disseminator diss=(Disseminator) iter.next();
//                // add its dsBindMapID to the mapIDs list
//                mapIDs.add(diss.dsBindMapID);
//            }
//        }
//        // get a new, unique binding map id, starting with "S" given the complete list
//        return newID(mapIDs.iterator(), "S");
//    }

    public String newAuditRecordID() {
        ArrayList<String> auditIDs=new ArrayList<String>();
        Iterator iter=m_auditRecords.iterator();
        while (iter.hasNext()) {
            AuditRecord record=(AuditRecord) iter.next();
            auditIDs.add(record.id);
        }
        return newID(auditIDs.iterator(), "AUDREC");
    }
    
	/**
	 * Sets an extended property on the object.
	 *
	 * @param propName The extende property name, either a string, or URI as string.
	 */
	public void setExtProperty(String propName, String propValue) {
		m_extProperties.put(propName, propValue);
		
	}

	/**
	 * Gets an extended property value, given the property name.
	 *
	 * @return The property value.
	 */
	public String getExtProperty(String propName) {
		return (String) m_extProperties.get(propName);
		
	}

	/**
	 * Gets a Map containing all of the extended properties
	 * on the object.  Map key is property name.
	 *
	 * @return The property Map.
	 */	
	public Map getExtProperties() {
		return m_extProperties;
		
	}

    /**
     * Given an iterator of existing ids, return a new id that
     * starts with <code>start</code> and is guaranteed to be
     * unique.  This algorithm adds one to the highest existing
     * id that starts with <code>start</code>.  If no such existing
     * id exists, it will return <i>start</i> + "1".
     */
    private String newID(Iterator iter, String start) {
        int highest=0;
        while (iter.hasNext()) {
            String id=(String) iter.next();
            if (id.startsWith(start) && id.length()>start.length()) {
                try {
                    int num=Integer.parseInt(id.substring(start.length()));
                    if (num>highest) highest=num;
                } catch (NumberFormatException ignored) { }
            }
        }
        int newNum=highest+1;
        return start + newNum;
    }

}
