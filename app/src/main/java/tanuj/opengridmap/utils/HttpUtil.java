package tanuj.opengridmap.utils;

import android.net.Uri;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by Tanuj on 21/9/2015.
 */
public class HttpUtil {
//    private HttpURLConnection conn;

    public static String httpPost(URL url, Map<String, String> params) {
        String response = null;
        int readTimeOut = 10000;
        int connectTimeOut = 10000;

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(readTimeOut);
            conn.setConnectTimeout(connectTimeOut);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String query = getQueryFromParams(params);
            OutputStream outputStream = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream,
                    "UTF-8"));

            writer.write(query);
            writer.flush();
            writer.close();
            outputStream.close();

            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return response;
    }

    public static String httpGet(URL url, Map<String, String> params) {
        String response = null;
        int readTimeOut = 10000;
        int connectTimeOut = 10000;

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(readTimeOut);
            conn.setConnectTimeout(connectTimeOut);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String query = getQueryFromParams(params);
            OutputStream outputStream = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream,
                    "UTF-8"));

            writer.write(query);
            writer.flush();
            writer.close();
            outputStream.close();

            conn.connect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private static String getQueryFromParams(Map<String, String> params) {
        Uri.Builder builder = new Uri.Builder();

        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.appendQueryParameter(param.getKey(), param.getValue());
        }

        return builder.build().getEncodedQuery();
    }
}
