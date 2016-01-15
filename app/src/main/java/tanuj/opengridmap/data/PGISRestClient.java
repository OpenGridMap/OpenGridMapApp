package tanuj.opengridmap.data;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by Tanuj on 2/12/2015.
 */
public class PGISRestClient {
    private static final String SERVER_BASE_URL = "http://vmjacobsen39.informatik.tu-muenchen.de";

    private static final String SUBMISSION_URL = SERVER_BASE_URL + "/submissions/create";

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    private static void post(Context context, String url, StringEntity entity, String contentType, AsyncHttpResponseHandler responseHandler) {
        httpClient.post(context, url, entity, contentType, responseHandler);
    }

    public static void postSubmission(Context context, String json, AsyncHttpResponseHandler responseHandler) throws UnsupportedEncodingException {
        post(context, SUBMISSION_URL, new StringEntity(json),RequestParams.APPLICATION_JSON, responseHandler);
    }
}
