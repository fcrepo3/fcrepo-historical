package fedora.server.storage.replication;

/**
 * <p>Title: RowInsertion.java</p>
 * <p>Description: Database row insertion code.
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Paul Charlton
 * @version 1.0
 */

import java.util.*;
import java.sql.*;
import java.io.*;


/**
* <p>
* Description: Provides methods to insert Fedora database rows.
*
* @version 1.0
*
*/
public class RowInsertion {   

        /**
        * <p>
        * Inserts a Behavior Definition row.
        *
        * @param db Database connection object
        * @param bdef_pid Behavior definition PID
        * @param bdef_label Behavior definition label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertBehaviorDefinitionRow(DbmsConnection db, String bdef_pid, String bdef_label) throws SQLException {

		String insertionStatement = "INSERT INTO BehaviorDefinition (BDEF_DBID, BDEF_PID, BDEF_Label) VALUES ('', '" + bdef_pid + "', '" + bdef_label + "');";

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a Behavior Mechanism row.
        *
        * @param db Database connection object
        * @param bdef_dbid Behavior definition DBID
        * @param bmech_dbid Behavior mechanism DBID
        * @param bmech_label Behavior mechanism label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertBehaviorMechanismRow(DbmsConnection db, String bdef_dbid, String bmech_pid, String bmech_label) throws SQLException {

		String insertionStatement = "INSERT INTO BehaviorMechanism (BMECH_DBID, BDEF_DBID, BMECH_PID, BMECH_Label) VALUES ('', '" + bdef_dbid + "', '" + bmech_pid + "', '" + bmech_label + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a DataStreamBindingRow row.
        *
        * @param db Database connection object
        * @param do_pid Digital object PID
        * @param dsbindingkey_dbid Datastream binding key DBID
        * @param bindingmap_dbid Binding map DBID
        * @param dsbinding_ds_bindingkey_seq Datastream binding key sequence number
        * @param dsbinding_ds_id Datastream ID
        * @param dsbinding_ds_label Datastream label
        * @param dsbinding_ds_mime Datastream mime type
        * @param dsbinding_ds_location Datastream location
        * @param policy_dbid Policy DBID
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDataStreamBindingRow(DbmsConnection db, String do_dbid, String dsbindingkey_dbid, String bindingmap_dbid, String dsbinding_ds_bindingkey_seq, String dsbinding_ds_id, String dsbinding_ds_label, String dsbinding_ds_mime, String dsbinding_ds_location, String policy_dbid) throws SQLException {

		String insertionStatement = "INSERT INTO DataStreamBinding (DO_DBID, DSBindingKey_DBID, BindingMap_DBID, DSBinding_DS_BindingKey_Seq, DSBinding_DS_ID, DSBinding_DS_Label, DSBinding_DS_MIME, DSBinding_DS_Location, POLICY_DBID) VALUES ('" + do_dbid + "', '" + dsbindingkey_dbid + "', '" + bindingmap_dbid + "', '" + dsbinding_ds_bindingkey_seq + "', '" + dsbinding_ds_id + "', '" + dsbinding_ds_label + "', '" + dsbinding_ds_mime + "', '" + dsbinding_ds_location + "', '" + policy_dbid + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a DataStreamBindingMap row.
        *
        * @param db Database connection object
        * @param bmech_dbid Behavior mechanism DBID
        * @param dsbindingmap_id Datastream binding map ID
        * @param dsbindingmap_label Datastream binding map label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDataStreamBindingMapRow(DbmsConnection db, String bmech_dbid, String dsbindingmap_id, String dsbindingmap_label) throws SQLException {

		String insertionStatement = "INSERT INTO DataStreamBindingMap (BindingMap_DBID, BMECH_DBID, DSBindingMap_ID, DSBindingMap_Label) VALUES ('', '" + bmech_dbid + "', '" + dsbindingmap_id + "', '" + dsbindingmap_label + "');"; 
		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a DataStreamBindingSpec row.
        *
        * @param db Database connection object
        * @param bmech_dbid Behavior mechanism DBID
        * @param dsbindingspec_name Datastream binding spec name
        * @param dsbindingspec_ordinality_flag Datastream binding spec ordinality flag
        * @param dsbindingspec_cardinality Datastream binding cardinality
        * @param dsbindingspec_mime Datastream binding spec mime type
        * @param dsbindingspec_label Datastream binding spec lable
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDataStreamBindingSpecRow(DbmsConnection db, String bmech_dbid, String dsbindingspec_name, String dsbindingspec_ordinality_flag, String dsbindingspec_cardinality, String dsbindingspec_mime, String dsbindingspec_label) throws SQLException {

		String insertionStatement = "INSERT INTO DataStreamBindingSpec (DSBindingKey_DBID, BMECH_DBID, DSBindingSpec_Name, DSBindingSpec_Ordinality_Flag, DSBindingSpec_Cardinality, DSBindingSpec_MIME, DSBindingSpec_Label) VALUES ('', '" + bmech_dbid + "', '" + dsbindingspec_name + "', '" + dsbindingspec_ordinality_flag + "', '" + dsbindingspec_cardinality + "', '" + dsbindingspec_mime + "', '" + dsbindingspec_label + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a DigitalObject row.
        *
        * @param db Database connection object
        * @param do_pid DigitalObject PID
        * @param do_label DigitalObject label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDigitalObjectRow(DbmsConnection db, String do_pid, String do_label) throws SQLException {

		String insertionStatement = "INSERT INTO DigitalObject (DO_DBID, DO_PID, DO_Label) VALUES ('', '" + do_pid + "', '" +  do_label + "');";

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a DigitalObjectDissAssoc row.
        *
        * @param db Database connection object
        * @param do_dbid DigitalObject DBID
        * @param diss_dbid Disseminator DBID
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDigitalObjectDissAssocRow(DbmsConnection db, String do_dbid, String diss_dbid) throws SQLException {

		String insertionStatement = "INSERT INTO DigitalObjectDissAssoc (DO_DBID, DISS_DBID) VALUES ('" + do_dbid + "', '" + diss_dbid + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a Disseminator row.
        *
        * @param db Database connection object
        * @param bdef_dbid Behavior definition DBID
        * @param bmech_dbid Behavior mechanism DBID
        * @param diss_id Disseminator ID
        * @param diss_label Disseminator label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertDisseminatorRow(DbmsConnection db, String bdef_dbid, String bmech_dbid, String diss_id, String diss_label) throws SQLException { 

		String insertionStatement = "INSERT INTO Disseminator (DISS_DBID, BDEF_DBID, BMECH_DBID, DISS_ID, DISS_Label) VALUES ('', '" + bdef_dbid + "', '" + bmech_dbid + "', '" + diss_id + "', '" + diss_label + "');"; 
		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a MechanismImpl row.
        *
        * @param db Database connection object
        * @param bmech_dbid Behavior mechanism DBID
        * @param bdef_dbid Behavior definition DBID
        * @param meth_dbid Method DBID
        * @param dsbindingkey_dbid Datastream binding key DBID
        * @param mechimpl_protocol_type Mechanism implementation protocol type
        * @param mechimpl_return_type Mechanism implementation return type
        * @param mechimpl_address_location Mechanism implementation address location
        * @param mechimpl_operation_location Mechanism implementation operation location
        * @param policy_dbid Policy DBID
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertMechanismImplRow(DbmsConnection db, String bmech_dbid, String bdef_dbid, String meth_dbid, String dsbindingkey_dbid, String mechimpl_protocol_type, String mechimpl_return_type, String mechimpl_address_location, String mechimpl_operation_location, String policy_dbid) throws SQLException {

		String insertionStatement = "INSERT INTO MechanismImpl (BMECH_DBID, BDEF_DBID, METH_DBID, DSBindingKey_DBID, MECHImpl_Protocol_Type, MECHImpl_Return_Type, MECHImpl_Address_Location, MECHImpl_Operation_Location, POLICY_DBID) VALUES ('" + bmech_dbid + "', '" + bdef_dbid + "', '" + meth_dbid + "', '" + dsbindingkey_dbid + "', '" + mechimpl_protocol_type + "', '" + mechimpl_return_type + "', '" + mechimpl_address_location + "', '" + mechimpl_operation_location + "', '" + policy_dbid + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * Inserts a Method row.
        *
        * @param db Database connection object
        * @param bdef_dbid Behavior definition DBID
        * @param meth_name Behavior definition label
        * @param meth_label Behavior definition label
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertMethodRow(DbmsConnection db, String bdef_dbid, String meth_name, String meth_label) throws SQLException {

		String insertionStatement = "INSERT INTO Method (METH_DBID, BDEF_DBID, METH_Name, METH_Label) VALUES ('', '" + bdef_dbid + "', '" + meth_name + "', '" + meth_label + "');"; 

		insertGen(db, insertionStatement);
	}

        /**
        * <p>
        * General JDBC row insertion method.
        *
        * @param db Database connection object
        * @param insertionStatement SQL row insertion statement
        *
        * @exception SQLException JDBC, SQL error
        */
	public void insertGen(DbmsConnection db, String insertionStatement) throws SQLException {
		int rowCount = 0;
		Statement statement = null;

		statement = db.connection.createStatement();

System.out.println("insertGen: insertionStatement = " + insertionStatement);
		rowCount = statement.executeUpdate(insertionStatement); 
System.out.println("rowCount = " + rowCount);
		statement.close();
	}

        /**
        * <p>
        * Used for unit testing and demonstration purposes.
        *
        * @param args program arguments
        *
        * @exception Exception exceptions that are thrown from called methods
        */
        public static void main(String[] args) throws Exception {
		String returnString;

		DbmsConnection db = new DbmsConnection();
		db.connectDatabase();

		RowInsertion ri = new RowInsertion();
		ri.insertDigitalObjectRow(db, "x", "x");
		System.out.println("insertDigitalObject returned");
	}
}
