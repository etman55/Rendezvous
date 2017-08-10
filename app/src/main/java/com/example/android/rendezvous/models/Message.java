package com.example.android.rendezvous.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Etman on 8/5/2017.
 */
public class Message implements Parcelable {

    private String message;
    private String type;
    private long time_stamp;
    private boolean seen;
    private String from;

    public Message() {
    }

    public Message(String message, String type, long time_stamp, boolean seen, String from) {
        this.message = message;
        this.type = type;
        this.time_stamp = time_stamp;
        this.seen = seen;
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTime_stamp() {
        return time_stamp;
    }

    public void setTime_stamp(long time_stamp) {
        this.time_stamp = time_stamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    protected Message(Parcel in) {
        message = in.readString();
        type = in.readString();
        time_stamp = in.readLong();
        seen = in.readByte() != 0x00;
        from = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(message);
        dest.writeString(type);
        dest.writeLong(time_stamp);
        dest.writeByte((byte) (seen ? 0x01 : 0x00));
        dest.writeString(from);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}