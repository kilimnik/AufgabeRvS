package edu.udo.cs.rvs;

import java.util.Date;

/**
 * Klasse welche einen String in ein Datum konvertiert
 */
public class DateFormatter {

    /**
     * Konvertiert einen String in ein Datum Objekt
     *
     * @param dateString String des Datums
     *
     * @return Datum Objekt erzeugt aus dem String
     */
    @SuppressWarnings("deprecation")
    public static Date parseDate(String dateString){
        Date date = new Date();

        dateString = dateString.replace(":", " ");
        String[] dateParts = dateString.split(" ");

        date.setDate(Integer.parseInt(dateParts[1]));
        date.setMonth(decodeMonth(dateParts[2]));
        date.setYear(Integer.parseInt(dateParts[3])-1900);
        date.setHours(Integer.parseInt(dateParts[4]));
        date.setMinutes(Integer.parseInt(dateParts[5]));
        date.setSeconds(Integer.parseInt(dateParts[6]));

        return date;
    }

    /**
     * Wandelt einen Monats String zu einer Zahl des Monats
     *
     * @param month String des Monats (3 Zeichern lang)
     * @return Zahl 0-11 (0: Januar, 11: Dezember)
     */
    private static int decodeMonth(String month){
        switch (month){
            case "Jan":
                return 0;
            case "Feb":
                return 1;
            case "Mar":
                return 2;
            case "Apr":
                return 3;
            case "May":
                return 4;
            case "Jun":
                return 5;
            case "Jul":
                return 6;
            case "Aug":
                return 7;
            case "Sep":
                return 8;
            case "Oct":
                return 9;
            case "Nov":
                return 10;
            case "Dec":
                return 11;
        }

        return -1;
    }
}
