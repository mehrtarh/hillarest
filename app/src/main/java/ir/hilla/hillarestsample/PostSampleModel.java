package ir.hilla.hillarestsample;

import android.os.Parcel;
import android.os.Parcelable;

import ir.hilla.rest.gson.annotations.HillaSerializedName;

public class PostSampleModel implements Parcelable {

    @HillaSerializedName("origin")
    private String origin;
    @HillaSerializedName("url")
    private String url;
    @HillaSerializedName("data")
    private String data;



    public PostSampleModel(String origin, String url, String data) {
        this.origin = origin;
        this.url = url;
        this.data = data;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.origin);
        dest.writeString(this.url);
        dest.writeString(this.data);
    }

    protected PostSampleModel(Parcel in) {
        this.origin = in.readString();
        this.url = in.readString();
        this.data = in.readString();
    }

    public static final Parcelable.Creator<PostSampleModel> CREATOR = new Parcelable.Creator<PostSampleModel>() {
        @Override
        public PostSampleModel createFromParcel(Parcel source) {
            return new PostSampleModel(source);
        }

        @Override
        public PostSampleModel[] newArray(int size) {
            return new PostSampleModel[size];
        }
    };
}
