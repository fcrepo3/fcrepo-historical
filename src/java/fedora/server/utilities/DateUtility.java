package fedora.server.utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 
 * <p>
 * <b>Title: </b> DateUtility.java
 * </p>
 * <p>
 * <b>Description: </b> A collection of utility methods for performing
 * </p>
 * <p>
 * frequently require tasks.
 * </p>
 * 
 * @author rlw@virginia.edu
 * @version $Id$
 */
public abstract class DateUtility {

    /**
     * <p>
     * Converts a datetime string into and instance of java.util.Date using the
     * date format: yyyy-MM-ddTHH:mm:ss.SSSZ.
     * </p>
     * 
     * <p>
     * Follows Postel's Law (lenient about what it accepts, as long as it's
     * sensible)
     * </p>
     * 
     * @param dateTime
     *            A datetime string
     * @return Corresponding instance of java.util.Date (returns null if
     *         dateTime string argument is empty string or null)
     */
    public static Date convertStringToDate(String dateTime) {
        return parseDate(dateTime);
    }

    /**
     * <p>
     * Converts an instance of java.util.Date into a String using the date
     * format: yyyy-MM-ddTHH:mm:ss.SSSZ.
     * </p>
     * 
     * @param  date Instance of java.util.Date.
     * @return      ISO 8601 String representation (yyyy-MM-ddTHH:mm:ss.SSSZ) 
     *              of the Date argument or null if the Date argument is null.
     */
    public static String convertDateToString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return df.format(date);
        }
    }
    
    
    /**
     * <p>
     * Converts an instance of java.util.Date into a String using the date
     * format: yyyy-MM-ddZ.
     * </p>
     * 
     * @param date
     *            Instance of java.util.Date.
     * @return Corresponding date string (returns null if Date argument is
     *         null).
     */
    public static String convertDateToDateString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat(
                    "yyyy-MM-dd'Z'");
            return df.format(date);
        }
    }
    
    
    /**
     * <p>
     * Converts an instance of java.util.Date into a String using the date
     * format: mm:ss.SSSZ.
     * </p>
     * 
     * @param date
     *            Instance of java.util.Date.
     * @return Corresponding time string (returns null if Date argument is
     *         null).
     */
    public static String convertDateToTimeString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat(
                    "HH:mm:ss.SSS'Z'");
            return df.format(date);
        }
    }    

    /**
     * Attempt to parse the given string of form: yyyy-MM-dd[THH:mm:ss[.SSS][Z]]
     * as a Date. If the string is not of that form, return null.
     * 
     * @param str
     *            the date string to parse
     * @return Date the date, if parse was successful; null otherwise
     */
    public static Date parseDate(String str) {
        if (str == null || str.length() == 0)
            return null;
        if (str.indexOf("T") != -1) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .parse(str);
            } catch (ParseException pe) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                            .parse(str);
                } catch (ParseException pe1) {
                    try {
                        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                                .parse(str);
                    } catch (ParseException pe2) {
                        try {
                            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                                    .parse(str);
                        } catch (ParseException pe3) {
                            return null;
                        }
                    }
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

    public static void main(String[] args) {
        String dateTimeString = "2002-08-22T13:58:06";
        Date date = convertStringToDate(dateTimeString);
        System.out.println("\nDateString: " + dateTimeString
                + "\nConvertDateToString: " + convertDateToString(date));
        System.out.println("\nDate: " + convertDateToString(date)
                + "\nConvertDateToString: " + convertDateToString(date));
        System.out.println("\nDate: " + convertDateToDateString(date)
                + "\nConvertDateToDateString: " + convertDateToDateString(date));
        System.out.println("\nDate: " + convertDateToTimeString(date)
                + "\nConvertDateToTimeString: " + convertDateToTimeString(date));        
    }
}