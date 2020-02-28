package tla.backend;

import java.util.Date;

import tla.backend.es.model.ModelConfig;

public class Util {

    /**
     * create Date instance from ISO-8601 conforming string
     */
    public static Date date(String date) {
        try {
            return ModelConfig.DATE_FORMAT.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}