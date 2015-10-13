package tanuj.opengridmap.models;

/**
 * Created by Tanuj on 13/10/2015.
 */
public class Payload {
    private long submissionId;

    private long imageId;

    private String payloadEntity;

    public Payload(long submissionId, long imageId, String payloadEntity) {
        this.submissionId = submissionId;
        this.imageId = imageId;
        this.payloadEntity = payloadEntity;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public long getImageId() {
        return imageId;
    }

    public String getPayloadEntity() {
        return payloadEntity;
    }
}
