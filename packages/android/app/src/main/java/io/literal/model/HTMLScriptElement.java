package io.literal.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class HTMLScriptElement implements Parcelable {
    private final String src;
    private final String text;
    private final String type;

    public HTMLScriptElement(String src, String text, String type) {
        this.src = src;
        this.text = text;
        this.type = type;
    }

    protected HTMLScriptElement(Parcel in) {
        src = in.readString();
        text = in.readString();
        type = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(src);
        dest.writeString(text);
        dest.writeString(type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HTMLScriptElement> CREATOR = new Creator<HTMLScriptElement>() {
        @Override
        public HTMLScriptElement createFromParcel(Parcel in) {
            return new HTMLScriptElement(in);
        }

        @Override
        public HTMLScriptElement[] newArray(int size) {
            return new HTMLScriptElement[size];
        }
    };

    public static HTMLScriptElement fromJSON(JSONObject json) throws JSONException {
        return new HTMLScriptElement(
                !json.isNull("src") ? json.getString("src") : null,
                !json.isNull("text") ? json.getString("text") : null,
                !json.isNull("type") ? json.getString("type") : null
        );
    }

    public String getSrc() {
        return src;
    }
    public String getText() {
        return text;
    }
    public String getType() { return type; }

    public void appendToStringBuilder(StringBuilder builder) {
        if (text == null) {
            return;
        }

        if (type != null && type.length() > 0) {
            builder.append("<script type=\"");
            builder.append(type);
            builder.append("\">\n");
        } else {
            builder.append("<script>\n");
        }
        builder.append(text);
        builder.append("\n</script>");
    }
}
