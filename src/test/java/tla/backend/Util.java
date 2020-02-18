package tla.backend;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {

    /**
     * create Date instance from ISO-8601 conforming string
     */
    public static Date date(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}