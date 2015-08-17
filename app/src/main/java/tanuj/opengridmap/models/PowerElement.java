package tanuj.opengridmap.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tanuj on 08.05.15.
 */
public class PowerElement implements Parcelable{
    private int id;
    private String name;
    private int imageId;
    private String description;

    public PowerElement() {}

    public PowerElement(String name, int id, int imageId) {
        this.id = id;
        this.name = name;
        this.imageId = imageId;
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

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getImageId() {
        return imageId;
    }

    public String getDescription() {
        return description;
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

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
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
}
