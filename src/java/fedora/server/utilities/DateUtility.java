package fedora.server.utilities;

import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>Title: DateUtility.java</p>
 * <p>Description: A collection of utility methods for performing</p>
 * <p>frequently require tasks.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */
public abstract class DateUtility
{
  private static final boolean debug = true; //Testing
  private SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * <p>Converts a datetime string into and instance of java.util.Calendar using
   * the date format: yyyy-MM-ddTHH:mm:ss.</p>
   *
   * @param dateTime A datetime string.
   * @return Corresponding instance of java.util.Calendar (returns null
   *         if dateTime string argument is empty string or null).
   */
  public static Calendar convertStringToCalendar(String dateTime)
  {
    Calendar calendar = null;
    SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    if (!(dateTime == null) && !dateTime.equalsIgnoreCase(""))
    {
      calendar = Calendar.getInstance();
      ParsePosition pos = new ParsePosition(0);
      Date date = formatter.parse(dateTime, pos);
      calendar.setTime(date);
    }
    return(calendar);

  }

  /**
   * <p>Converts a datetime string into and instance of java.util.Date using
   * the date format: yyyy-MM-ddTHH:mm:ss.</p>
   *
   * @param dateTime A datetime string
   * @return Corresponding instance of java.util.Date (returns null
   *         if dateTime string argument is empty string or null)
   */
  public static Date convertStringToDate(String dateTime)
  {
    Date date = null;
    SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    if (!(dateTime == null) && !dateTime.equalsIgnoreCase(""))
    {
      ParsePosition pos = new ParsePosition(0);
      date = formatter.parse(dateTime, pos);
    }
    return(date);
  }

  /**
   * <p>Converts an instance of java.util.Calendar into a string using
   * the date format: yyyy-MM-ddTHH:mm:ss.</p>
   *
   * @param calendar An instance of java.util.Calendar.
   * @return Corresponding datetime string (returns null if Calendar
   *         argument is null).
   */
  public static String convertCalendarToString(Calendar calendar)
  {
    String dateTime = null;
    SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    if (!(calendar == null))
    {
      Date date = calendar.getTime();
      dateTime = formatter.format(date);
    }
    return(dateTime);
  }

  /**
   * <p>Converts an instance of java.util.Date into a String using
   * the date format: yyyy-MM-ddTHH:mm:ss.</p>
   *
   * @param date Instance of java.util.Date.
   * @return Corresponding datetime string (returns null if Date argument
   *         is null).
   */
  public static String convertDateToString(Date date)
  {
    String dateTime = null;
    SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    if (!(date == null))
    {
      dateTime = formatter.format(date);
    }
    return(dateTime);
  }

  /**
   * <p>Converts an instance of java.util.Calendar into and instance
   * of java.util.Date.</p>
   *
   * @param calendar Instance of java.util.Calendar.
   * @return Corresponding instance of java.util.Date (returns null
   *         if Calendar argument is null).
   */
  public static Date convertCalendarToDate(Calendar calendar)
  {
    Date date = null;
    if(!(calendar == null))
    {
      date = calendar.getTime();
    }
    return(date);
  }

  /**
   * <p>Converts an instance of java.util.Date into an instance
   * of java.util.Calendar.</p>
   *
   * @param date Instance of java.util.Date.
   * @return Corresponding instance of java.util.Calendar (returns null
   *         if Date argument is null).
   */
  public static Calendar convertDateToCalendar(Date date)
  {
    Calendar calendar = null;
    if (!(date == null))
    {
      calendar = Calendar.getInstance();
      ParsePosition pos = new ParsePosition(0);
      calendar.setTime(date);
    }
    return(calendar);
  }
  
  public static Date convertLocalDateToUTCDate(Date localDate) 
  {
    // figure out the time zone offset of this machine (in millisecs)
    Calendar cal=Calendar.getInstance();
    int tzOffset=cal.get(Calendar.ZONE_OFFSET);
    // ...and account for daylight savings time, if applicable
    TimeZone tz = cal.getTimeZone();
    if (tz.inDaylightTime(localDate)) 
    {
      tzOffset+=cal.get(Calendar.DST_OFFSET);
    }
    // now we have UTF offset in millisecs... so add it to localDate.millisecs
    // and return a new Date object.
    Date UTCDate=new Date();
    UTCDate.setTime(localDate.getTime() + tzOffset);
    return UTCDate;
  }
  
    /**
     * Attempt to parse the given string of form: yyyy-MM-dd[Thh:mm:ss[Z]] 
     * as a Date.  If the string is not of that form, return null.
     *
     * @param str the date string to parse
     * @return Date the date, if parse was successful; null otherwise
     */
    public static Date parseDate(String str) {
        if (str.indexOf("T")!=-1) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(str);
            } catch (ParseException pe) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'").parse(str);
                } catch (ParseException pe2) {
                    return null;
                }
            }
        } else {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(str);
            } catch (ParseException pe3) {
                return null;
            }
        }
        
    }

  public static void main(String[] args)
  {
    String dateTimeString = "2002-08-22T13:58:06";
    Calendar cal = convertStringToCalendar(dateTimeString);
    System.out.println("DateString: " + dateTimeString
                       + "\nConvertCalendarToString: " + cal);
    Date date = convertStringToDate(dateTimeString);
    System.out.println("\nDateString: " + dateTimeString
                       + "\nConvertDateToString: "
                       + convertDateToString(date));
    System.out.println("\nCalendar: " + cal
                       + "\nConvertCalendarToString: "
                       + convertCalendarToString(cal));
    System.out.println("\nDate: " + convertDateToString(date)
                       + "\nConvertDateT0String: "
                       + convertDateToString(date));
    System.out.println("\nCalendar: " + cal
                       + "\nConvertCalendarToDate: "
                       + convertDateToString(convertCalendarToDate(cal)));
    System.out.println("\nDate: " + convertDateToString(date)
                       + "\nConvertDateToCalendar: "
                       + convertDateToCalendar(date));
  }
}