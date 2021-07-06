package io.literal.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.literal.repository.ErrorRepository;

public class HTMLScriptElement implements Parcelable {
    private static final String KEY_TEXT = "TEXT";
    private static final String KEY_ATTRIBUTES = "ATTRIBUTES";

    private final String text;
    private final HashMap<String, String> attributes;

    public HTMLScriptElement(HashMap<String, String> attributes, String text) {
        this.text = text;
        this.attributes = attributes;
    }

    protected HTMLScriptElement(Parcel in) {
        Bundle input = new Bundle();
        input.readFromParcel(in);

        text = input.getString(KEY_TEXT);
        attributes = (HashMap<String, String>) input.getSerializable(KEY_ATTRIBUTES);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle output = new Bundle();
        if (text != null) {
            output.putString(KEY_TEXT, text);
        }
        if (attributes != null) {
            output.putSerializable(KEY_ATTRIBUTES, attributes);
        }

        output.writeToParcel(dest, flags);
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
        HashMap<String, String> attributes = new HashMap<>();
        if (!json.isNull("attributes")) {
            JSONObject jsonAttributes = json.getJSONObject("attributes");
            jsonAttributes.keys().forEachRemaining((key) -> {
                try {
                    attributes.put(key, jsonAttributes.getString(key));
                } catch (JSONException e) {
                    ErrorRepository.captureException(e);
                }
            });
        }

        return new HTMLScriptElement(
                attributes,
                !json.isNull("text") ? json.getString("text") : null
        );
    }

    public String getText() {
        return text;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void appendToStringBuilder(StringBuilder builder) {
        builder.append("<script");
        if (attributes != null && attributes.size() > 0) {
            attributes.entrySet().forEach((entry) -> {
                builder.append(" ");
                builder.append(entry.getKey());
                builder.append("=\"");
                builder.append(entry.getValue());
                builder.append("\"");
            });
        }

        builder.append(">");

        if (text != null && text.length() > 0) {
            builder.append("\n");
            builder.append(text);
            builder.append("\n");
        }

        builder.append("</script>");
    }
}
