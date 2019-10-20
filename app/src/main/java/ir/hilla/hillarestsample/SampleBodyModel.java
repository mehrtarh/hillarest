package ir.hilla.hillarestsample;

import android.os.Parcel;
import android.os.Parcelable;

import ir.hilla.rest.gson.annotations.HillaSerializedName;

public class SampleBodyModel implements Parcelable {

    @HillaSerializedName("body1")
    private String body1;
    @HillaSerializedName("body2")
    private String body2;



    public SampleBodyModel(String body1, String body2) {
        this.body1 = body1;
        this.body2 = body2;
    }

    public String getBody1() {
        return body1;
    }

    public void setBody1(String body1) {
        this.body1 = body1;
    }

    public String getBody2() {
        return body2;
    }

    public void setBody2(String body2) {
        this.body2 = body2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.body1);
        dest.writeString(this.body2);
    }

    protected SampleBodyModel(Parcel in) {
        this.body1 = in.readString();
        this.body2 = in.readString();
    }

    public static final Parcelable.Creator<SampleBodyModel> CREATOR = new Parcelable.Creator<SampleBodyModel>() {
        @Override
        public SampleBodyModel createFromParcel(Parcel source) {
            return new SampleBodyModel(source);
        }

        @Override
        public SampleBodyModel[] newArray(int size) {
            return new SampleBodyModel[size];
        }
    };
}
