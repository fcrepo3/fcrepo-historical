package fedora.server.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 *
 * <p><b>Title:</b> TypeUtility.java</p>
 * <p><b>Description:</b> A utility class for converting back and forth from the
 * internal Fedora type classes in fedora.server.storage.types and the
 * generated type classes produced by the wsdl2java emitter in
 * fedora.server.types.gen.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public abstract class TypeUtility
{

    public static fedora.server.types.gen.Datastream
            convertDatastreamToGenDatastream(
            fedora.server.storage.types.Datastream in) {
        fedora.server.types.gen.Datastream out=
                new fedora.server.types.gen.Datastream();
        String group=in.DSControlGrp;
        out.setControlGroup(fedora.server.types.gen.DatastreamControlGroup.fromValue(group));
		if (group.equals("R") || group.equals("E")) {
		    // only given location if it's a redirect or external datastream
            out.setLocation(in.DSLocation);
		}
        java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
        cal.setTime(in.DSCreateDT);
        out.setCreateDate(cal);
        out.setID(in.DatastreamID);
        out.setInfoType(in.DSInfoType);
        out.setLabel(in.DSLabel);
        out.setMIMEType(in.DSMIME);
        out.setSize(in.DSSize);
        out.setState(in.DSState);
        out.setVersionID(in.DSVersionID);
        return out;
    }

    public static fedora.server.types.gen.FieldSearchResult
            convertFieldSearchResultToGenFieldSearchResult(
            fedora.server.search.FieldSearchResult result) {
        fedora.server.types.gen.FieldSearchResult ret=
                new fedora.server.types.gen.FieldSearchResult();
        ret.setResultList(convertSearchObjectFieldsListToGenObjectFieldsArray(
                result.objectFieldsList()));
        if (result.getToken()!=null) {
            fedora.server.types.gen.ListSession sess=
                    new fedora.server.types.gen.ListSession();
            sess.setToken(result.getToken());
            if (result.getCursor()>-1) {
                sess.setCursor(new org.apache.axis.types.NonNegativeInteger("" + result.getCursor()));
            }
            if (result.getCompleteListSize()>-1) {
                sess.setCompleteListSize(new org.apache.axis.types.NonNegativeInteger("" + result.getCompleteListSize()));
            }
            if (result.getExpirationDate()!=null) {
                Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTime(result.getExpirationDate());
                sess.setExpirationDate(cal);
            }
            ret.setListSession(sess);
        }
        return ret;
    }

    public static fedora.server.search.FieldSearchQuery
            convertGenFieldSearchQueryToFieldSearchQuery(
            fedora.server.types.gen.FieldSearchQuery gen)
            throws fedora.server.errors.InvalidOperatorException,
            fedora.server.errors.QueryParseException {
        if (gen.getTerms()!=null) {
            return new fedora.server.search.FieldSearchQuery(gen.getTerms());
        } else {
            return new fedora.server.search.FieldSearchQuery(
                    convertGenConditionArrayToSearchConditionList(
                    gen.getConditions()));
        }
    }

    public static java.util.List
            convertGenConditionArrayToSearchConditionList(
            fedora.server.types.gen.Condition[] genConditions)
            throws fedora.server.errors.InvalidOperatorException,
            fedora.server.errors.QueryParseException {
        java.util.ArrayList list=new java.util.ArrayList();
        for (int i=0; i<genConditions.length; i++) {
            fedora.server.types.gen.Condition c=genConditions[i];
            list.add(new fedora.server.search.Condition(c.getProperty(),
                    c.getOperator().toString(), c.getValue()));
        }
        return list;
    }

    public static fedora.server.types.gen.ObjectFields[]
            convertSearchObjectFieldsListToGenObjectFieldsArray(
            java.util.List sfList) {
        fedora.server.types.gen.ObjectFields[] genFields=
                new fedora.server.types.gen.ObjectFields[
                sfList.size()];
        for (int i=0; i<sfList.size(); i++) {
            fedora.server.types.gen.ObjectFields gf=
                    new fedora.server.types.gen.ObjectFields();
            fedora.server.search.ObjectFields sf=
                    (fedora.server.search.ObjectFields) sfList.get(i);
            // Repository key fields
            if (sf.getPid()!=null) gf.setPid(sf.getPid());
            if (sf.getLabel()!=null) gf.setLabel(sf.getLabel());
            if (sf.getFType()!=null) gf.setFType(sf.getFType());
            if (sf.getCModel()!=null) gf.setCModel(sf.getCModel());
            if (sf.getState()!=null) gf.setState(sf.getState());
            if (sf.getOwnerId()!=null) gf.setOwnerId(sf.getOwnerId());
            if (sf.getCDate()!=null) {
                java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
                cal.setTime(sf.getCDate());
                gf.setCDate(cal);
            }
            if (sf.getMDate()!=null) {
                java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
                cal.setTime(sf.getMDate());
                gf.setMDate(cal);
            }
            if (sf.getDCMDate()!=null) {
                java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
                cal.setTime(sf.getMDate());
                gf.setDcmDate(cal);
            }
            if (sf.bDefs().size()!=0) gf.setBDef(toStringArray(sf.bDefs()));
            if (sf.bMechs().size()!=0) gf.setBMech(toStringArray(sf.bMechs()));
            // Dublin core fields
            if (sf.titles().size()!=0) gf.setTitle(toStringArray(sf.titles()));
            if (sf.creators().size()!=0) gf.setCreator(toStringArray(sf.creators()));
            if (sf.subjects().size()!=0) gf.setSubject(toStringArray(sf.subjects()));
            if (sf.descriptions().size()!=0) gf.setDescription(toStringArray(sf.descriptions()));
            if (sf.publishers().size()!=0) gf.setPublisher(toStringArray(sf.publishers()));
            if (sf.contributors().size()!=0) gf.setContributor(toStringArray(sf.contributors()));
            if (sf.dates().size()!=0) gf.setDate(toStringArray(sf.dates()));
            if (sf.types().size()!=0) gf.setType(toStringArray(sf.types()));
            if (sf.formats().size()!=0) gf.setFormat(toStringArray(sf.formats()));
            if (sf.identifiers().size()!=0) gf.setIdentifier(toStringArray(sf.identifiers()));
            if (sf.sources().size()!=0) gf.setSource(toStringArray(sf.sources()));
            if (sf.languages().size()!=0) gf.setLanguage(toStringArray(sf.languages()));
            if (sf.relations().size()!=0) gf.setRelation(toStringArray(sf.relations()));
            if (sf.coverages().size()!=0) gf.setCoverage(toStringArray(sf.coverages()));
            if (sf.rights().size()!=0) gf.setRights(toStringArray(sf.rights()));
            genFields[i]=gf;
        }
        return genFields;
    }

    public static String[] toStringArray(java.util.List l) {
        String[] ret=new String[l.size()];
        for (int i=0; i<l.size(); i++) {
            ret[i]=(String) l.get(i);
        }
        return ret;
    }

  /**
   * <p>Converts an array of fedora.server.storage.types.MethodDef into an
   * array of fedora.server.types.gen.MethodDef.</p>
   *
   * @param methodDefs An array of fedora.server.storage.types.MethodDef.
   * @return An array of fedora.server.types.gen.MethodDef.
   */
  public static fedora.server.types.gen.MethodDef[]
      convertMethodDefArrayToGenMethodDefArray(
      fedora.server.storage.types.MethodDef[] methodDefs)
  {
    if (methodDefs != null && methodDefs.length > 0)
    {
      fedora.server.types.gen.MethodDef[] genMethodDefs =
          new fedora.server.types.gen.MethodDef[methodDefs.length];
      for (int i=0; i<genMethodDefs.length; i++)
      {
        fedora.server.types.gen.MethodDef genMethodDef =
                 new fedora.server.types.gen.MethodDef();
        genMethodDef.setMethodLabel(methodDefs[i].methodLabel);
        genMethodDef.setMethodName(methodDefs[i].methodName);
        fedora.server.storage.types.MethodParmDef[] methodParmDefs =
            methodDefs[i].methodParms;
        //fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = null;
        fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = new fedora.server.types.gen.MethodParmDef[0];
        if (methodParmDefs != null && methodParmDefs.length > 0)
        {
          genMethodParmDefs =
              new fedora.server.types.gen.MethodParmDef[methodParmDefs.length];
          for (int j=0; j<methodParmDefs.length; j++)
          {
            genMethodParmDefs[j] =
                     convertMethodParmDefToGenMethodParmDef(methodParmDefs[j]);
          }
        }
        genMethodDef.setMethodParms(genMethodParmDefs);
        genMethodDefs[i] = genMethodDef;
      }
      return genMethodDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.MethodDef into an
   * instance of fedora.server.types.gen.MethodDef.</p>
   *
   * @param methodDef An instance of fedora.server.storage.types.MethodDef.
   * @return An instance of fedora.server.types.gen.MethodDef.
   */
  public static fedora.server.types.gen.MethodDef
      convertMethodDefToGenMethodDef(
      fedora.server.storage.types.MethodDef methodDef)
  {
    if (methodDef != null )
    {
      fedora.server.types.gen.MethodDef genMethodDefs =
          new fedora.server.types.gen.MethodDef();
      fedora.server.types.gen.MethodDef genMethodDef =
          new fedora.server.types.gen.MethodDef();
      genMethodDef.setMethodLabel(methodDef.methodLabel);
      genMethodDef.setMethodName(methodDef.methodName);
      fedora.server.storage.types.MethodParmDef[] methodParmDefs =
          methodDef.methodParms;
      //fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = null;
      fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = new fedora.server.types.gen.MethodParmDef[0];
      genMethodParmDefs = convertMethodParmDefArrayToGenMethodParmDefArray(
          methodParmDefs);
      if (methodParmDefs != null && methodParmDefs.length > 0)
      {
        genMethodParmDefs =
            new fedora.server.types.gen.MethodParmDef[methodParmDefs.length];
        for (int j=0; j<methodParmDefs.length; j++)
        {
          genMethodParmDefs[j] =
                   convertMethodParmDefToGenMethodParmDef(methodParmDefs[j]);
        }
      }
      genMethodDef.setMethodParms(genMethodParmDefs);
      return genMethodDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.types.gen.MethodDef into an
   * array of fedora.server.storage.types.MethodDef.</p>
   *
   * @param genMethodDefs An array of fedora.server.types.gen.MethodDef.
   * @return An array of fedora.server.storage.types.MethodDef.
   */
  public static fedora.server.storage.types.MethodDef[]
      convertGenMethodDefArrayToMethodDefArray(
      fedora.server.types.gen.MethodDef[] genMethodDefs)
  {
    if (genMethodDefs != null && genMethodDefs.length > 0)
    {
      fedora.server.storage.types.MethodDef[] methodDefs =
          new fedora.server.storage.types.MethodDef[genMethodDefs.length];
      for (int i=0; i<genMethodDefs.length; i++)
      {
        fedora.server.storage.types.MethodDef methodDef =
                 new fedora.server.storage.types.MethodDef();
        methodDef.methodLabel = genMethodDefs[i].getMethodLabel();
        methodDef.methodName = genMethodDefs[i].getMethodName();
        fedora.server.types.gen.MethodParmDef[] genMethodParmDefs =
            genMethodDefs[i].getMethodParms();
        //fedora.server.storage.types.MethodParmDef[] methodParmDefs = null;
        fedora.server.storage.types.MethodParmDef[] methodParmDefs = new fedora.server.storage.types.MethodParmDef[0];
        if (genMethodParmDefs != null && genMethodParmDefs.length > 0)
        {
          methodParmDefs =
              new fedora.server.storage.types.MethodParmDef[
              genMethodParmDefs.length];
          for (int j=0; j<genMethodParmDefs.length; j++)
          {
            methodParmDefs[j] = convertGenMethodParmDefToMethodParmDef(
            genMethodParmDefs[j]);
          }
        }
        methodDef.methodParms = methodParmDefs;
        methodDefs[i] = methodDef;
      }
      return methodDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.types.gen.MethodDef into an
   * instance of fedora.server.storage.types.MethodDef.</p>
   *
   * @param genMethodDef An instance of fedora.server.types.gen.MethodDef.
   * @return An instance of fedora.server.storage.types.MethodDef.
   */
  public static fedora.server.storage.types.MethodDef
      convertGenMethodDefToMethodDef(
      fedora.server.types.gen.MethodDef genMethodDef)
  {
    if (genMethodDef != null)
    {
      fedora.server.storage.types.MethodDef methodDef =
          new fedora.server.storage.types.MethodDef();
      methodDef.methodLabel = genMethodDef.getMethodLabel();
      methodDef.methodName = genMethodDef.getMethodName();
      fedora.server.types.gen.MethodParmDef[] genMethodParmDefs =
          genMethodDef.getMethodParms();
      //fedora.server.storage.types.MethodParmDef[] methodParmDefs = null;
      fedora.server.storage.types.MethodParmDef[] methodParmDefs = new fedora.server.storage.types.MethodParmDef[0];
      if (genMethodParmDefs != null && genMethodParmDefs.length > 0)
      {
        methodParmDefs = convertGenMethodParmDefArrayToMethodParmDefArray(
            genMethodParmDefs);
      }
      methodDef.methodParms = methodParmDefs;
      return methodDef;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.storage.types.MethodParmDef into
   * an array of fedora.server.types.gen.MethodParmDef.</p>
   *
   * @param methodParmDefs An array of fedora.server.storage.types.MethodParmDef.
   * @return An array of fedora.server.types.gen.MethodParmDef.
   */
  public static fedora.server.types.gen.MethodParmDef[]
      convertMethodParmDefArrayToGenMethodParmDefArray(
      fedora.server.storage.types.MethodParmDef[] methodParmDefs)
  {
    if (methodParmDefs != null && methodParmDefs.length > 0)
    {
      fedora.server.types.gen.MethodParmDef[] genMethodParmDefs =
          new fedora.server.types.gen.MethodParmDef[methodParmDefs.length];
      for (int i=0; i<genMethodParmDefs.length; i++)
      {
        fedora.server.types.gen.MethodParmDef genMethodParmDef =
                 new fedora.server.types.gen.MethodParmDef();
        genMethodParmDef =
            convertMethodParmDefToGenMethodParmDef(methodParmDefs[i]);
        genMethodParmDefs[i] = genMethodParmDef;
      }
      return genMethodParmDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.MethodParmDef into
   * an instance of fedora.server.types.gen.MethodParmDef.</p>
   *
   * @param methodParmDef An instance of
   *        fedora.server.storage.types.MethodParmDef.
   * @return An instance of fedora.server.types.gen.MethodParmDef.
   */
  public static fedora.server.types.gen.MethodParmDef
      convertMethodParmDefToGenMethodParmDef(
      fedora.server.storage.types.MethodParmDef methodParmDef)
  {
    if (methodParmDef != null)
    {
      fedora.server.types.gen.MethodParmDef genMethodParmDef =
          new fedora.server.types.gen.MethodParmDef();
      genMethodParmDef.setParmName(methodParmDef.parmName);
      genMethodParmDef.setParmLabel(methodParmDef.parmLabel);
      genMethodParmDef.setParmDefaultValue(methodParmDef.parmDefaultValue);
      genMethodParmDef.setParmDomainValues(methodParmDef.parmDomainValues);
      genMethodParmDef.setParmRequired(methodParmDef.parmRequired);
      genMethodParmDef.setParmType(methodParmDef.parmType);
      genMethodParmDef.setParmPassBy(methodParmDef.parmPassBy);
      return genMethodParmDef;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.types.gen.MethodParmDef into an
   * array of fedora.server.storage.types.MethodParmDef.</p>
   *
   * @param genMethodParmDefs An array of fedora.server.types.gen.MethodParmDef.
   * @return An array of fedora.server.storage.types.MethodParmDef.
   */
  public static fedora.server.storage.types.MethodParmDef[]
      convertGenMethodParmDefArrayToMethodParmDefArray(
      fedora.server.types.gen.MethodParmDef[] genMethodParmDefs)
  {
    if (genMethodParmDefs != null && genMethodParmDefs.length > 0)
    {
      fedora.server.storage.types.MethodParmDef[] methodParmDefs =
          new fedora.server.storage.types.MethodParmDef[
          genMethodParmDefs.length];
      for (int i=0; i<genMethodParmDefs.length; i++)
      {
        fedora.server.storage.types.MethodParmDef methodParmDef =
                 new fedora.server.storage.types.MethodParmDef();
        methodParmDef =
            convertGenMethodParmDefToMethodParmDef(genMethodParmDefs[i]);
        methodParmDefs[i] = methodParmDef;
      }
      return methodParmDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.MethodParmDef into
   * an instance of fedora.server.types.gen.MethodParmDef.</p>
   *
   * @param genMethodParmDef An instance of
   *        fedora.server.storage.types.MethodParmDef.
   * @return An instance of fedora.server.types.gen.MethodParmDef.
   */
  public static fedora.server.storage.types.MethodParmDef
      convertGenMethodParmDefToMethodParmDef(
      fedora.server.types.gen.MethodParmDef genMethodParmDef)
  {
    if (genMethodParmDef != null)
    {
      fedora.server.storage.types.MethodParmDef methodParmDef =
          new fedora.server.storage.types.MethodParmDef();
      methodParmDef.parmName = genMethodParmDef.getParmName();
      methodParmDef.parmLabel = genMethodParmDef.getParmLabel();
      methodParmDef.parmDefaultValue = genMethodParmDef.getParmDefaultValue();
      methodParmDef.parmDomainValues = genMethodParmDef.getParmDomainValues();
      methodParmDef.parmRequired = genMethodParmDef.isParmRequired();
      methodParmDef.parmType = genMethodParmDef.getParmType();
      methodParmDef.parmPassBy = genMethodParmDef.getParmPassBy();
      return methodParmDef;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.MIMETypedStream into
   * an instance of fedora.server.types.gen.MIMETypedStream.</p>
   *
   * @param mimeTypedStream An instance of
   *        fedora.server.storage.types.MIMETypedStream.
   * @return An instance of fedora.server.types.gen.MIMETypedStream.
   */
  public static fedora.server.types.gen.MIMETypedStream
      convertMIMETypedStreamToGenMIMETypedStream(
      fedora.server.storage.types.MIMETypedStream mimeTypedStream)
  {
    if (mimeTypedStream != null)
    {
      fedora.server.types.gen.MIMETypedStream genMIMETypedStream =
          new fedora.server.types.gen.MIMETypedStream();
      genMIMETypedStream.setMIMEType(mimeTypedStream.MIMEType);
      // RLW: change required by conversion fom byte[] to InputStream
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      InputStream is = mimeTypedStream.getStream();
      int byteStream = 0;
      try
      {
        byte[] buffer = new byte[255];
      while ((byteStream = is.read(buffer)) != -1)
      {
        baos.write(buffer, 0, byteStream);
      }
      } catch (IOException ioe)
      {
        System.out.println(ioe.getMessage());
      }
      genMIMETypedStream.setStream(baos.toByteArray());
      mimeTypedStream.setStream(new ByteArrayInputStream(baos.toByteArray()));
      //genMIMETypedStream.setStream(mimeTypedStream.stream);
      // RLW: change required by conversion fom byte[] to InputStream
      return genMIMETypedStream;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.types.gen.MIMETypedStream into
   * an instance of fedora.server.storage.types.MIMETypedStream.</p>
   *
   * @param genMIMETypedStream An instance of
   *        fedora.server.types.gen.MIMETypedStream.
   * @return an instance of fedora.server.storage.types.MIMETypedStream.
   */
  public static fedora.server.storage.types.MIMETypedStream
      convertGenMIMETypedStreamToMIMETypedStream(
      fedora.server.types.gen.MIMETypedStream genMIMETypedStream)
  {
    if (genMIMETypedStream != null)
    {
      // RLW: change required by conversion fom byte[] to InputStream
      InputStream is = new ByteArrayInputStream(genMIMETypedStream.getStream());
      fedora.server.storage.types.MIMETypedStream mimeTypedStream =
              new fedora.server.storage.types.MIMETypedStream(
          genMIMETypedStream.getMIMEType(), is);
      //fedora.server.storage.types.MIMETypedStream mimeTypedStream =
      //    new fedora.server.storage.types.MIMETypedStream(
      //    genMIMETypedStream.getMIMEType(), genMIMETypedStream.getStream());
      // RLW: change required by conversion fom byte[] to InputStream
      return mimeTypedStream;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.types.gen.ObjectMethodsDef into an
   * array of fedora.server.storage.types.ObjectMethodsDef.</p>
   *
   * @param genObjectMethodDefs An array of
   *        fedora.server.types.gen.ObjectMethodsDef.
   * @return An array of fedora.server.storage.types.ObjectMethodsDef.
   */
  public static fedora.server.storage.types.ObjectMethodsDef[]
      convertGenObjectMethodsDefArrayToObjectMethodsDefArray(
      fedora.server.types.gen.ObjectMethodsDef[] genObjectMethodDefs)
  {
    if (genObjectMethodDefs != null && genObjectMethodDefs.length > 0)
    {
      fedora.server.storage.types.ObjectMethodsDef[] objectMethodDefs =
          new fedora.server.storage.types.ObjectMethodsDef[
          genObjectMethodDefs.length];
      for (int i=0; i<genObjectMethodDefs.length; i++)
      {
        fedora.server.storage.types.ObjectMethodsDef objectMethodDef =
                 new fedora.server.storage.types.ObjectMethodsDef();
        objectMethodDef.PID = genObjectMethodDefs[i].getPID();
        objectMethodDef.bDefPID = genObjectMethodDefs[i].getBDefPID();
        objectMethodDef.methodName = genObjectMethodDefs[i].getMethodName();

        fedora.server.types.gen.MethodParmDef[] genMethodParmDefs =
            genObjectMethodDefs[i].getMethodParmDefs();
        //fedora.server.storage.types.MethodParmDef[] methodParmDefs = null;
        fedora.server.storage.types.MethodParmDef[] methodParmDefs = new fedora.server.storage.types.MethodParmDef[0];
        if (genMethodParmDefs != null && genMethodParmDefs.length > 0)
        {
          methodParmDefs =
              new fedora.server.storage.types.MethodParmDef[
              genMethodParmDefs.length];
          for (int j=0; j<genMethodParmDefs.length; j++)
          {
            methodParmDefs[j] = convertGenMethodParmDefToMethodParmDef(
            genMethodParmDefs[j]);
          }
        }
        objectMethodDef.methodParmDefs = methodParmDefs;
        objectMethodDefs[i] = objectMethodDef;
      }
      return objectMethodDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.types.gen.ObjectMethodsDef into
   * an instance of fedora.server.storage.types.ObjectMethodsDef.</p>
   *
   * @param genObjectMethodDef An instance of
   *        fedora.server.types.gen.ObjectMethodsDef.
   * @return An instance of fedora.server.storage.types.ObjectMethodsDef.
   */
  public static fedora.server.storage.types.ObjectMethodsDef
      convertGenObjectMethodsDefToObjectMethodsDef(
      fedora.server.types.gen.ObjectMethodsDef genObjectMethodDef)
  {
    if (genObjectMethodDef != null)
    {
      //fedora.server.storage.types.ObjectMethodsDef objectMethodDefs =
      //    new fedora.server.storage.types.ObjectMethodsDef();
      fedora.server.storage.types.ObjectMethodsDef objectMethodDef =
          new fedora.server.storage.types.ObjectMethodsDef();
      objectMethodDef.PID = genObjectMethodDef.getPID();
      objectMethodDef.bDefPID = genObjectMethodDef.getBDefPID();
      objectMethodDef.methodName = genObjectMethodDef.getMethodName();
      fedora.server.types.gen.MethodParmDef[] genMethodParmDefs =
          genObjectMethodDef.getMethodParmDefs();
      //fedora.server.storage.types.MethodParmDef[] methodParmDefs = null;
      fedora.server.storage.types.MethodParmDef[] methodParmDefs = new fedora.server.storage.types.MethodParmDef[0];
      if (genMethodParmDefs != null && genMethodParmDefs.length > 0)
      {
        methodParmDefs = convertGenMethodParmDefArrayToMethodParmDefArray(
            genMethodParmDefs);
      }
      objectMethodDef.methodParmDefs = methodParmDefs;
      return objectMethodDef;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.storage.types.ObjectMethodsDef into
   * an array of fedora.server.types.gen.ObjectMethodsDef.</p>
   *
   * @param objectMethodDefs An array of
   *        fedora.server.storage.types.ObjectMethodsDef.
   * @return An array of fedora.server.types.gen.ObjectMethodsDef.
   */
  public static fedora.server.types.gen.ObjectMethodsDef[]
      convertObjectMethodsDefArrayToGenObjectMethodsDefArray(
      fedora.server.storage.types.ObjectMethodsDef[] objectMethodDefs)
  {
    if (objectMethodDefs != null && objectMethodDefs.length > 0)
    {
      fedora.server.types.gen.ObjectMethodsDef[] genObjectMethodDefs =
          new fedora.server.types.gen.ObjectMethodsDef[objectMethodDefs.length];
      for (int i=0; i<objectMethodDefs.length; i++)
      {
        fedora.server.types.gen.ObjectMethodsDef genObjectMethodDef =
                 new fedora.server.types.gen.ObjectMethodsDef();
        genObjectMethodDef.setPID(objectMethodDefs[i].PID);
        genObjectMethodDef.setBDefPID(objectMethodDefs[i].bDefPID);
        genObjectMethodDef.setMethodName(objectMethodDefs[i].methodName);
        fedora.server.storage.types.MethodParmDef[] methodParmDefs =
            objectMethodDefs[i].methodParmDefs;
        //fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = null;
        fedora.server.types.gen.MethodParmDef[] genMethodParmDefs = new fedora.server.types.gen.MethodParmDef[0];
        if (methodParmDefs != null && methodParmDefs.length > 0)
        {
          genMethodParmDefs =
              new fedora.server.types.gen.MethodParmDef[methodParmDefs.length];
          for (int j=0; j<methodParmDefs.length; j++)
          {
            genMethodParmDefs[j] =
                     convertMethodParmDefToGenMethodParmDef(methodParmDefs[j]);
          }
        }
        genObjectMethodDef.setMethodParmDefs(genMethodParmDefs);
        genObjectMethodDefs[i] = genObjectMethodDef;
      }
      return genObjectMethodDefs;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.ObjectMethodsDef
   * into an instance of fedora.server.types.gen.ObjectMethodsDef.</p>
   *
   * @param objectMethodDef An instance of
   *        fedora.server.storage.types.ObjectMethodsDef.
   * @return An instance of fedora.server.types.gen.ObjectMethodsDef.
   */
  public static fedora.server.types.gen.ObjectMethodsDef
      convertObjectMethodsDefToGenObjectMethodsDef(
      fedora.server.storage.types.ObjectMethodsDef objectMethodDef)
  {
    if (objectMethodDef != null)
    {
      fedora.server.types.gen.ObjectMethodsDef genObjectMethodDef =
          new fedora.server.types.gen.ObjectMethodsDef();
      genObjectMethodDef.setPID(objectMethodDef.PID);
      genObjectMethodDef.setBDefPID(objectMethodDef.bDefPID);
      genObjectMethodDef.setMethodName(objectMethodDef.methodName);
      return genObjectMethodDef;

    } else
    {
      return null;
    }
  }

    /**
   * <p>Converts an instance of fedora.server.types.gen.ObjectProfile into
   * an instance of fedora.server.access.ObjectProfile.</p>
   *
   * @param genObjectProfile An instance of
   *        fedora.server.types.gen.ObjectProfile.
   * @return An instance of fedora.server.access.ObjectProfile.
   */
  public static fedora.server.access.ObjectProfile
      convertGenObjectProfileToObjectProfile(
      fedora.server.types.gen.ObjectProfile genObjectProfile)
  {
    if (genObjectProfile != null)
    {
      fedora.server.access.ObjectProfile objectProfile =
          new fedora.server.access.ObjectProfile();
      objectProfile.PID = genObjectProfile.getPid();
      objectProfile.objectLabel = genObjectProfile.getObjLabel();
      objectProfile.objectContentModel = genObjectProfile.getObjContentModel();
      objectProfile.objectType = genObjectProfile.getObjType();
      objectProfile.objectCreateDate =
          DateUtility.convertCalendarToDate(genObjectProfile.getObjCreateDate());
      objectProfile.objectLastModDate =
          DateUtility.convertCalendarToDate(genObjectProfile.getObjLastModDate());
      objectProfile.dissIndexViewURL = genObjectProfile.getObjDissIndexViewURL();
      objectProfile.itemIndexViewURL = genObjectProfile.getObjItemIndexViewURL();
      return objectProfile;
    } else
    {
      return null;
    }
  }

  public static fedora.server.types.gen.ObjectProfile
      convertObjectProfileToGenObjectProfile(
      fedora.server.access.ObjectProfile objectProfile)
  {
    if (objectProfile != null)
    {
      fedora.server.types.gen.ObjectProfile genObjectProfile =
          new fedora.server.types.gen.ObjectProfile();
      genObjectProfile.setPid(objectProfile.PID);
      genObjectProfile.setObjLabel(objectProfile.objectLabel);
      genObjectProfile.setObjContentModel(objectProfile.objectContentModel);
      genObjectProfile.setObjType(objectProfile.objectType);
      genObjectProfile.setObjCreateDate(
          DateUtility.convertDateToCalendar(objectProfile.objectCreateDate));
      genObjectProfile.setObjLastModDate(
          DateUtility.convertDateToCalendar(objectProfile.objectLastModDate));
      genObjectProfile.setObjDissIndexViewURL(objectProfile.dissIndexViewURL);
      genObjectProfile.setObjItemIndexViewURL(objectProfile.itemIndexViewURL);
      return genObjectProfile;
    } else
    {
      return null;
    }
  }

  public static fedora.server.types.gen.RepositoryInfo
      convertReposInfoToGenReposInfo(
      fedora.server.access.RepositoryInfo repositoryInfo)
  {
    if (repositoryInfo != null)
    {
      fedora.server.types.gen.RepositoryInfo genRepositoryInfo =
          new fedora.server.types.gen.RepositoryInfo();
      genRepositoryInfo.setRepositoryName(repositoryInfo.repositoryName);
      genRepositoryInfo.setRepositoryBaseURL(repositoryInfo.repositoryBaseURL);
      genRepositoryInfo.setRepositoryVersion(repositoryInfo.repositoryVersion);
      genRepositoryInfo.setRepositoryPIDNamespace(repositoryInfo.repositoryPIDNamespace);
      genRepositoryInfo.setOAINamespace(repositoryInfo.OAINamespace);
      genRepositoryInfo.setAdminEmailList(repositoryInfo.adminEmailList);
      genRepositoryInfo.setSamplePID(repositoryInfo.samplePID);
      genRepositoryInfo.setSampleOAIIdentifier(repositoryInfo.sampleOAIIdentifer);
      genRepositoryInfo.setSampleSearchURL(repositoryInfo.sampleSearchURL);
      genRepositoryInfo.setSampleAccessURL(repositoryInfo.sampleAccessURL);
      genRepositoryInfo.setSampleOAIURL(repositoryInfo.sampleOAIURL);
      return genRepositoryInfo;
    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an array of fedora.server.types.gen.Property into an
   * array of fedora.server.storage.types.Property.</p>
   *
   * @param genProperties An array of fedora.server.types.gen.Property.
   * @return An array of fedora.server.storage.types.Property.
   */
  public static fedora.server.storage.types.Property[]
      convertGenPropertyArrayToPropertyArray(
      fedora.server.types.gen.Property[] genProperties)
  {
    if (genProperties != null && genProperties.length > 0)
    {
      fedora.server.storage.types.Property[] properties =
          new fedora.server.storage.types.Property[genProperties.length];
      for (int i=0; i<genProperties.length; i++)
      {
        fedora.server.storage.types.Property property =
                 new fedora.server.storage.types.Property();
        property =
            convertGenPropertyToProperty(genProperties[i]);
        properties[i] = property;
      }
      return properties;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.types.gen.Property into
   * an instance of fedora.server.storage.types.Property.</p>
   *
   * @param genProperty An instance of fedora.server.types.gen.Property.
   * @return An instance of fedora.server.storage.types.Property.
   */
  public static fedora.server.storage.types.Property
      convertGenPropertyToProperty(
      fedora.server.types.gen.Property genProperty)
  {
    fedora.server.storage.types.Property property =
          new fedora.server.storage.types.Property();
    if (genProperty != null)
    {
      property.name = genProperty.getName();
      property.value = genProperty.getValue();
    }
    return property;
  }

  /**
   * <p>Converts an array of fedora.server.storage.types.Property into an
   * array of fedora.server.types.gen.Property.</p>
   *
   * @param properties An array of fedora.server.storage.typesProperty.
   * @return An array of fedora.server.types.gen.Property.
   */
  public static fedora.server.types.gen.Property[]
      convertPropertyArrayToGenPropertyArray(
      fedora.server.storage.types.Property[] properties)
  {
    if (properties != null && properties.length > 0)
    {
      fedora.server.types.gen.Property[] genProperties =
          new fedora.server.types.gen.Property[properties.length];
      for (int i=0; i<properties.length; i++)
      {
        fedora.server.types.gen.Property genProperty =
                 new fedora.server.types.gen.Property();
        genProperty =
            convertPropertyToGenProperty(properties[i]);
        genProperties[i] = genProperty;
      }
      return genProperties;

    } else
    {
      return null;
    }
  }

  /**
   * <p>Converts an instance of fedora.server.storage.types.Property into
   * an instance of fedora.server.types.gen.Property.</p>
   *
   * @param property An instance of fedora.server.storage.types.Property.
   * @return An instance of fedora.server.types.gen.Property.
   */
  public static fedora.server.types.gen.Property
      convertPropertyToGenProperty(
      fedora.server.storage.types.Property property)
  {
    fedora.server.types.gen.Property genProperty =
        new fedora.server.types.gen.Property();
    if (property != null)
    {
      genProperty.setName(property.name);
      genProperty.setValue(property.value);
    }
    return genProperty;
  }

  public static fedora.server.types.gen.Disseminator
          convertDisseminatorToGenDisseminator(
          fedora.server.storage.types.Disseminator in)
  {
      fedora.server.types.gen.Disseminator out=
              new fedora.server.types.gen.Disseminator();
      java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
      cal.setTime(in.dissCreateDT);
      out.setBDefLabel(in.bDefLabel);
      out.setBDefPID(in.bDefID);
      out.setBMechLabel(in.bMechLabel);
      out.setBMechPID(in.bMechID);
      out.setCreateDT(cal);
      out.setID(in.dissID);
      out.setLabel(in.dissLabel);
      out.setState(in.dissState);
      out.setVersionID(in.dissVersionID);
      out.setDsBindMap(convertDSBindingMapToGenDatastreamBindingMap(in.dsBindMap));
      return out;
    }

    public static fedora.server.storage.types.Disseminator
        convertGenDisseminatorToDisseminator(
        fedora.server.types.gen.Disseminator genDisseminator)
    {

      fedora.server.storage.types.Disseminator diss =
            new fedora.server.storage.types.Disseminator();
      if (genDisseminator != null)
      {
        diss.bDefLabel = genDisseminator.getBDefLabel();
        diss.bDefID = genDisseminator.getBDefPID();
        diss.bMechLabel = genDisseminator.getBMechLabel();
        diss.bMechID = genDisseminator.getBMechPID();
        diss.dissCreateDT = genDisseminator.getCreateDT().getTime();
        diss.dissID = genDisseminator.getID();
        diss.dissLabel = genDisseminator.getLabel();
        diss.dissState = genDisseminator.getState();
        diss.dissVersionID = genDisseminator.getVersionID();
        diss.dsBindMap = convertGenDatastreamBindingMapToDSBindingMap(genDisseminator.getDsBindMap());
      }
      return diss;
  }

    public static fedora.server.types.gen.DatastreamBindingMap
            convertDSBindingMapToGenDatastreamBindingMap(
            fedora.server.storage.types.DSBindingMap in)
    {
        fedora.server.types.gen.DatastreamBindingMap out=
                new fedora.server.types.gen.DatastreamBindingMap();
        java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
        fedora.server.types.gen.DatastreamBinding datastreamBinding =
            new fedora.server.types.gen.DatastreamBinding();
        out.setDsBindings(convertDSBindingArrayToGenDatastreamBindingArray(in.dsBindings));
        out.setDsBindMapID(in.dsBindMapID);
        out.setDsBindMapLabel(in.dsBindMapLabel);
        out.setDsBindMechanismPID(in.dsBindMechanismPID);
        out.setState(in.state);

        return out;
    }

    public static fedora.server.storage.types.DSBindingMap
        convertGenDatastreamBindingMapToDSBindingMap(
        fedora.server.types.gen.DatastreamBindingMap genDatastreamBindingMap)
    {
      fedora.server.storage.types.DSBindingMap dsBindingMap =
            new fedora.server.storage.types.DSBindingMap();
      if (genDatastreamBindingMap != null)
      {
        dsBindingMap.dsBindings = convertGenDatastreamBindingArrayToDSBindingArray(genDatastreamBindingMap.getDsBindings());
        dsBindingMap.dsBindMapID = genDatastreamBindingMap.getDsBindMapID();
        dsBindingMap.dsBindMapLabel = genDatastreamBindingMap.getDsBindMapLabel();
        dsBindingMap.dsBindMechanismPID = genDatastreamBindingMap.getDsBindMechanismPID();
        dsBindingMap.state = genDatastreamBindingMap.getState();
      }
      return dsBindingMap;
  }

    public static fedora.server.types.gen.DatastreamBinding
            convertDSBindingToGenDatastreamBinding(
            fedora.server.storage.types.DSBinding in)
    {
        fedora.server.types.gen.DatastreamBinding out=
                new fedora.server.types.gen.DatastreamBinding();
        java.util.GregorianCalendar cal=new java.util.GregorianCalendar();
        out.setBindKeyName(in.bindKeyName);
        out.setBindLabel(in.bindLabel);
        out.setDatastreamID(in.datastreamID);
        out.setSeqNo(in.seqNo);
        return out;
    }

    public static fedora.server.storage.types.DSBinding
        convertGenDatastreamBindingToDSBinding(
        fedora.server.types.gen.DatastreamBinding genDatastreamBinding)
    {

      fedora.server.storage.types.DSBinding dsBinding =
            new fedora.server.storage.types.DSBinding();
      if (genDatastreamBinding != null)
      {
        dsBinding.bindKeyName = genDatastreamBinding.getBindKeyName();
        dsBinding.bindLabel = genDatastreamBinding.getBindLabel();
        dsBinding.datastreamID = genDatastreamBinding.getDatastreamID();
        dsBinding.seqNo = genDatastreamBinding.getSeqNo();
      }
      return dsBinding;
  }

    public static fedora.server.types.gen.DatastreamBinding[]
        convertDSBindingArrayToGenDatastreamBindingArray(
        fedora.server.storage.types.DSBinding[] dsBindings)
    {

      if (dsBindings != null && dsBindings.length > 0)
      {
        fedora.server.types.gen.DatastreamBinding[] genDatastreamBindings =
            new fedora.server.types.gen.DatastreamBinding[dsBindings.length];
        for (int i=0; i<genDatastreamBindings.length; i++)
        {
          fedora.server.types.gen.DatastreamBinding genDatastreamBinding =
                   new fedora.server.types.gen.DatastreamBinding();
          genDatastreamBindings[i] =
              convertDSBindingToGenDatastreamBinding(dsBindings[i]);
        }
        return genDatastreamBindings;

      } else
      {
        return null;
      }
  }

  public static fedora.server.storage.types.DSBinding[]
      convertGenDatastreamBindingArrayToDSBindingArray(
      fedora.server.types.gen.DatastreamBinding[] genDatastreamBindings)
  {

    if (genDatastreamBindings != null && genDatastreamBindings.length > 0)
    {
      fedora.server.storage.types.DSBinding[] dsBindings =
          new fedora.server.storage.types.DSBinding[genDatastreamBindings.length];
      for (int i=0; i<genDatastreamBindings.length; i++)
      {
        fedora.server.storage.types.DSBinding dsBinding =
                 new fedora.server.storage.types.DSBinding();
        dsBinding =
            convertGenDatastreamBindingToDSBinding(genDatastreamBindings[i]);
        dsBindings[i] = dsBinding;
      }
      return dsBindings;

    } else
    {
      return null;
    }
  }


  public static void main(String[] args)
  {
    try{
    fedora.server.storage.types.MethodParmDef methodParmDef1 =
        new fedora.server.storage.types.MethodParmDef();
    methodParmDef1.parmName = "parm_name1";
    methodParmDef1.parmLabel = "parm_label1";
    methodParmDef1.parmDefaultValue = "parm_default_value1";
    methodParmDef1.parmRequired = true;
    fedora.server.storage.types.MethodParmDef methodParmDef2 =
        new fedora.server.storage.types.MethodParmDef();
    methodParmDef2.parmName = "parm_name2";
    methodParmDef2.parmLabel = "parm_label2";
    methodParmDef2.parmDefaultValue = "parm_default_value2";
    methodParmDef2.parmRequired = false;
    fedora.server.storage.types.MethodDef methodDef1 =
        new fedora.server.storage.types.MethodDef();
    methodDef1.methodName = "method_name1";
    methodDef1.methodLabel = "method_label1";
    fedora.server.storage.types.MethodParmDef[] methodParmDefs =
        new fedora.server.storage.types.MethodParmDef[2];
    methodParmDefs[0] = methodParmDef1;
    methodParmDefs[1] = methodParmDef2;
    methodDef1.methodParms = methodParmDefs;
    fedora.server.storage.types.MethodDef methodDef2 =
        new fedora.server.storage.types.MethodDef();
    methodDef2.methodName = "method_name2";
    methodDef2.methodLabel = "method_label2";
    methodDef2.methodParms = null;

    fedora.server.storage.types.MethodDef[] methodDef =
        new fedora.server.storage.types.MethodDef[2];
    methodDef[0] = methodDef1;
    methodDef[1] = methodDef2;
    fedora.server.storage.types.Property[] properties =
        new fedora.server.storage.types.Property[2];
    fedora.server.storage.types.Property prop1 =
        new fedora.server.storage.types.Property();
    fedora.server.storage.types.Property prop2 =
        new fedora.server.storage.types.Property();
    prop1.name = "prop1_name";
    prop1.value = "prop1_value";
    prop2.name = "prop2_name";
    prop2.value = "prop2_value";
    properties[0] = prop1;
    properties[1] = prop2;

    fedora.server.storage.types.ObjectMethodsDef[] objectMethods =
        new fedora.server.storage.types.ObjectMethodsDef[2];
    fedora.server.storage.types.ObjectMethodsDef objectMethod1 =
        new fedora.server.storage.types.ObjectMethodsDef();
    fedora.server.storage.types.ObjectMethodsDef objectMethod2 =
        new fedora.server.storage.types.ObjectMethodsDef();
    objectMethod1.PID = "PID1";
    objectMethod1.bDefPID = "bDefPID1";
    objectMethod1.methodName = "method1";
    objectMethod2.PID = "PID2";
    objectMethod2.bDefPID = "bDefPID2";
    objectMethod2.methodName = "method2";
    objectMethods[0] = objectMethod1;
    objectMethods[1] = objectMethod2;

    System.out.println("\n----- Started with these values:");
    for (int i=0; i<methodDef.length; i++)
    {
      System.out.println("name: "+methodDef[i].methodName+
      "\nlabel: "+methodDef[i].methodLabel+
      "\nparms:\n");
      fedora.server.storage.types.MethodParmDef[] methodParmDef = null;
      methodParmDef = methodDef[i].methodParms;
      if (methodParmDef != null && methodParmDef.length > 0)
      {
        methodParmDef = methodDef[i].methodParms;
        for (int j=0; j<methodParmDef.length; j++)
        {
          System.out.println("parmname: "+methodParmDef[j].parmName+
          "\nparmLabel: "+methodParmDef[j].parmLabel+
          "\nparmDefault: "+methodParmDef[j].parmDefaultValue+
          "\nparmrequired: "+methodParmDef[j].parmRequired);
        }
      }
    }

    System.out.println("\nObjectMethod  values:");
    for (int i=0; i<objectMethods.length; i++)
    {
      System.out.println("ObjectMethod[" + i + "] = " + "\nPID = "
      + objectMethods[i].PID+"\nbDefPID = " + objectMethods[i].bDefPID
      + "\nmethod = " + objectMethods[i].methodName);
    }

    System.out.println("\nProperty  values:");
    for (int i=0; i<properties.length; i++)
    {
      System.out.println("Prop[" + i + "] = " + "\nname = "
      + properties[i].name+"\nvalue = " + properties[i].value);
    }

    System.out.println("\n----- Converting MethodDefArray to "
                       + "GenMethodDefArray");
    fedora.server.types.gen.MethodDef[] genMethodDef =
        TypeUtility.convertMethodDefArrayToGenMethodDefArray(methodDef);
    for (int i=0; i<genMethodDef.length; i++)
    {
      System.out.println("name: "+genMethodDef[i].getMethodName()+
      "\nlabel: "+genMethodDef[i].getMethodLabel()+
      "\nparms:\n");
      fedora.server.types.gen.MethodParmDef[] methodParmDef = null;
      methodParmDef = genMethodDef[i].getMethodParms();
      if (methodParmDef != null && methodParmDef.length > 0)
      {
        methodParmDef = genMethodDef[i].getMethodParms();
        for (int j=0; j<methodParmDef.length; j++)
        {
          System.out.println("parmname: "+methodParmDef[j].getParmName()+
          "\nparmLabel: "+methodParmDef[j].getParmLabel()+
          "\nparmDefault: "+methodParmDef[j].getParmDefaultValue()+
          "\nparmrequired: "+methodParmDef[j].isParmRequired());
        }
      }
    }
    methodDef = null;

    System.out.println("\n----- Converting GenMethodDefArray to "
                       + "MethodDefArray");
    methodDef =
        TypeUtility.convertGenMethodDefArrayToMethodDefArray(genMethodDef);
    for (int i=0; i<methodDef.length; i++)
    {
      System.out.println("name: "+methodDef[i].methodName+
      "\nlabel: "+methodDef[i].methodLabel+
      "\nparms:\n");
      fedora.server.storage.types.MethodParmDef[] methodParmDef = null;
      methodParmDef = methodDef[i].methodParms;
      if (methodParmDef != null && methodParmDef.length > 0)
      {
        methodParmDef = methodDef[i].methodParms;
        for (int j=0; j<methodParmDef.length; j++)
        {
          System.out.println("parmname: "+methodParmDef[j].parmName+
          "\nparmLabel: "+methodParmDef[j].parmLabel+
          "\nparmDefault: "+methodParmDef[j].parmDefaultValue+
          "\nparmrequired: "+methodParmDef[j].parmRequired);
        }
      }
    }

    System.out.println("\n----- Starting with MIMETypedStream of:");
    String text = "this is some text for the bytestream";
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    java.io.PrintWriter pw = new java.io.PrintWriter(baos);
    pw.println(text);
    pw.close();
    byte[] stream = baos.toByteArray();
    InputStream is = new ByteArrayInputStream(stream);
    fedora.server.storage.types.MIMETypedStream mimeTypedStream =
        //new fedora.server.storage.types.MIMETypedStream("text/plain", stream);
        new fedora.server.storage.types.MIMETypedStream("text/plain", is);
    System.out.println("MIMEType: " + mimeTypedStream.MIMEType);
    //ByteArrayInputStream bais =
    //    new ByteArrayInputStream(mimeTypedStream.stream);
    int byteStream = 0;
    //while ((byteStream = bais.read()) >= 0)
    byte[] buffer = new byte[255];
    while ((byteStream = is.read()) >= 0)
    {
      System.out.write(buffer, 0, byteStream);
    }
    System.out.println("\n----- Converting GenMIMETypedStream to "+
                       " MIMETypedStream");
    fedora.server.types.gen.MIMETypedStream genMIMETypedStream =
        TypeUtility.convertMIMETypedStreamToGenMIMETypedStream(
        mimeTypedStream);
    System.out.println("MIMEType: " + genMIMETypedStream.getMIMEType());
    ByteArrayInputStream bais = null;
    bais = new ByteArrayInputStream(genMIMETypedStream.getStream());
    byteStream = 0;
    while ((byteStream = bais.read(buffer)) >= 0)
    {
      System.out.write(buffer, 0, byteStream);
    }
    System.out.println("\n----- Converting MIMETypedStream to "+
                       " GenMIMETypedStream");
    mimeTypedStream =
        TypeUtility.convertGenMIMETypedStreamToMIMETypedStream(
        genMIMETypedStream);
    System.out.println("MIMEType: " + mimeTypedStream.MIMEType);
    is = mimeTypedStream.getStream();
    //bais = new ByteArrayInputStream(mimeTypedStream.stream);
    byteStream = 0;
    //while ((byteStream = bais.read()) >= 0)
    while ((byteStream = is.read(buffer)) >= -1)
    {
      System.out.write(buffer, 0, byteStream);
    }

    System.out.println("\n----- Converting ObjectMethodsDefArray to "
                       + "GenObjectMethodsDefArray");
    fedora.server.types.gen.ObjectMethodsDef[] genObjectMethods = null;
    genObjectMethods =
        TypeUtility.convertObjectMethodsDefArrayToGenObjectMethodsDefArray(
        objectMethods);
    for (int i=0; i<genObjectMethods.length; i++)
    {
      System.out.println("GenProp[" + i + "]: " + "\nPID = "
      + genObjectMethods[i].getPID()+"\nbDefPID = "
      + genObjectMethods[i].getBDefPID() + "\nmethod = "
      + genObjectMethods[i].getMethodName());
    }
    System.out.println("\n----- Converting GenObjectMethodsDefArray to "
                       + "ObjectMethodsDefArray");
    objectMethods =
        TypeUtility.convertGenObjectMethodsDefArrayToObjectMethodsDefArray(
        genObjectMethods);
    for (int i=0; i<objectMethods.length; i++)
    {
      System.out.println("ObjectMethods[" + i + "]: " + "\nPID = "
      + objectMethods[i].PID+"\nbDefPID = " + objectMethods[i].bDefPID
      + "\nmethod = " + objectMethods[i].methodName);
    }

    System.out.println("\n----- Converting PropertyArray to "
                       + "GenPropertyArray");
    fedora.server.types.gen.Property[] genProperties =
        TypeUtility.convertPropertyArrayToGenPropertyArray(properties);
    for (int i=0; i<genProperties.length; i++)
    {
      System.out.println("GenProp[" + i + "]: " + "\nname = "
      + genProperties[i].getName()+"\nvalue = " + genProperties[i].getValue());
    }
    System.out.println("\n----- Converting GenPropertyArray to "
                       + "PropertyArray");
    properties = TypeUtility.convertGenPropertyArrayToPropertyArray(
        genProperties);
    for (int i=0; i<properties.length; i++)
    {
      System.out.println("Prop[" + i + "]: " + "\nname = "
      + properties[i].name+"\nvalue = " + properties[i].value);
    }
    } catch (Exception e)
    {
      System.out.println(e.getMessage());
    }

  }

}