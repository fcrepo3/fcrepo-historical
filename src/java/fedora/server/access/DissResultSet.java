package fedora.server.access;

/**
 * <p>Title: DissResultSet.java</p>
 * <p>Description: Data structure class for Dissemination result sets.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */

public class DissResultSet
{
  public String addressLocation = null;
  public String operationLocation = null;
  public String dsLocation = null;
  public String dsBindingKey = null;
  public String returnType = null;
  public String protocolType = null;
  public String doPID = null;
  public String dissName = null;
  public String method = null;

  public DissResultSet()
  {}

  /**
   * Constructor that initializes the class variables.
   *
   * @param sqlResults - array containing the results from a dissemination
   * query against the SQL database.
   */
  public DissResultSet(String[] sqlResults)
  {
      this.doPID = sqlResults[0];
      this.dissName = sqlResults[1];
      this.method = sqlResults[2];
      this.addressLocation = sqlResults[3];
      this.operationLocation = sqlResults[4];
      this.protocolType = sqlResults[5];
      this.returnType = sqlResults[6];
      this.dsLocation = sqlResults[7];
      this.dsBindingKey = sqlResults[8];
  }

  public static void main(String[] args)
  {

    // initialize array for testing
    String[] sqlResults = new String[9];
    sqlResults[0] = "uva-lib:1220";
    sqlResults[1] = "DISS1";
    sqlResults[2] = "getThumbnail";
    sqlResults[3] = "LOCAL";
    sqlResults[4] = "(THUMBRES_IMG)";
    sqlResults[5] = "http";
    sqlResults[6] = "image/jpeg";
    sqlResults[7] = "http://dl.lib.virginia.edu/"+
                    "data/image/saskia/006-007a11.jpg";
    sqlResults[8] = "THUMBRES_IMG";

    DissResultSet result = new DissResultSet(sqlResults);

    System.out.println("Digital Object PID = "+result.doPID);
    System.out.println("Disseminator Name = "+result.dissName);
    System.out.println("Method Name = "+result.method);
    System.out.println("Address Location = "+result.addressLocation);
    System.out.println("Operation Location = "+result.operationLocation);
    System.out.println("Protocol Type = "+result.protocolType);
    System.out.println("Return MIME Type = "+result.returnType);
    System.out.println("Datastream Location = "+result.dsLocation);
    System.out.println("Datastream Binding Key = "+result.dsBindingKey);
  }

}