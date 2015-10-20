package tanuj.opengridmap.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class TimestampUtils {
    public static Timestamp getTimestampFromString(String time){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");;
        Date date;
        Timestamp timestamp = null;
        try {
            date = formatter.parse(time);
            timestamp = new Timestamp(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }
}
