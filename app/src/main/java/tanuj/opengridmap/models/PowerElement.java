package tanuj.opengridmap.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tanuj on 08.05.15.
 */
public class PowerElement implements Parcelable{
    private long id;
    private String name;
    private int imageId;
    private int description;
    private String osmTags;

    public PowerElement(long id, String name, int imageId, String osmTags) {
        this.id = id;
        this.name = name;
        this.imageId = imageId;
        this.osmTags = osmTags;
    }

    public PowerElement(long id, String name, String osmTags) {
        this.id = id;
        this.name = name;
        this.osmTags = osmTags;
    }

    public PowerElement(Parcel parcel) {
        this.id = parcel.readInt();
        this.name = parcel.readString();
        this.imageId = parcel.readInt();
    }

    public static final Creator<PowerElement> CREATOR = new Creator<PowerElement>() {
        @Override
        public PowerElement createFromParcel(Parcel in) {
            return new PowerElement(in);
        }

        @Override
        public PowerElement[] newArray(int size) {
            return new PowerElement[size];
        }
    };

    public PowerElement(long id, String name, int imageId, String osmTags, int description) {
        this(id, name, imageId, osmTags);
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getImageId() {
        return imageId;
    }

    public String getDeviceDescription(Context context) {
        return context.getString(description);
    }
    public String getDescription() {
        return null;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

//    public void setDescription(String description) {
//        this.description = description;
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeInt(this.imageId);
    }

    public static final Parcelable.Creator<PowerElement> POWER_ELEMENT_CREATOR = new Parcelable.Creator<PowerElement>() {

        @Override
        public PowerElement createFromParcel(Parcel source) {
            return new PowerElement(source);
        }

        @Override
        public PowerElement[] newArray(int size) {
            return new PowerElement[size];
        }
    };

    public String getOsmTags() {
        return osmTags;
    }
}
