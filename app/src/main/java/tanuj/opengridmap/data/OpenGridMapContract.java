package tanuj.opengridmap.data;

import android.provider.BaseColumns;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class OpenGridMapContract {
//    public OpenGridMapContract() {}

    public static final class PowerElementEntry implements BaseColumns {
        public static final String TABLE_NAME = "power_element";

        public static final String COLUMN_POWER_ELEMENT_NAME = "name";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_OSM_TAGS = "osm_tags";
    }

    public static final class SubmissionEntry implements BaseColumns {
        public static final String TABLE_NAME = "submission";

        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_SUBMISSION_ID = "submission_payloads_id";
        public static final String COLUMN_CREATED_TIMESTAMP = "created_on";
        public static final String COLUMN_UPDATED_TIMESTAMP = "updated_on";
        public static final String COLUMN_DELETED_TIMESTAMP = "deleted_on";
    }

    public static final class PowerElementSubmissionEntry {
        public static final String TABLE_NAME = "power_element_submission";

        public static final String COLUMN_POWER_ELEMENT_ID = "power_element_id";
        public static final String COLUMN_SUBMISSION_ID = "submission_id";
    }

    public static final class ImageEntry implements BaseColumns {
        public static final String TABLE_NAME = "image";

        public static final String COLUMN_SUBMISSION_ID = "submission_id";
        public static final String COLUMN_SRC = "src";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_BEARING = "bearing";
        public static final String COLUMN_SPEED = "speed";
        public static final String COLUMN_ALTITUDE = "altitude";
        public static final String COLUMN_ACCURACY = "accuracy";
        public static final String COLUMN_NO_OF_SATELLITES = "no_satellites";
        public static final String COLUMN_PROVIDER = "provider";
        public static final String COLUMN_CREATED_TIMESTAMP = "created_on";
        public static final String COLUMN_UPDATED_TIMESTAMP = "updated_on";
        public static final String COLUMN_DELETED_TIMESTAMP = "deleted_on";
    }

    public static final class UploadQueueEntry implements BaseColumns {
        public static final String TABLE_NAME = "upload_queue";

        public static final String COLUMN_SUBMISSION_ID = "submission_id";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_PAYLOADS_UPLOADED = "payloads_uploaded";
        public static final String COLUMN_CREATED_TIMESTAMP = "created_on";
        public static final String COLUMN_UPDATED_TIMESTAMP = "updated_on";
    }
}
