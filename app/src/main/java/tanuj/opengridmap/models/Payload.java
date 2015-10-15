package tanuj.opengridmap.models;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 13/10/2015.
 */
public class Payload {
    private long submissionId;

    private long imageId;

    private JSONObject payloadJSON;

    public Payload(long submissionId, long imageId, JSONObject payloadJSON) {
        this.submissionId = submissionId;
        this.imageId = imageId;
        this.payloadJSON = payloadJSON;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public long getImageId() {
        return imageId;
    }

    public String getPayloadEntity() {
        return payloadJSON.toString();
    }

    public void renewPayloadToken(Context context, String idToken) {
        try {
            payloadJSON.put(context.getString(R.string.json_key_id_token), idToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
