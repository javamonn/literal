package io.literal.model;

import android.util.JsonReader;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import io.literal.lib.JsonReaderParser;

public class RangeSelector<TRange extends Selector, TRefinedBy> extends Selector {

    private final TRange startSelector;
    private final TRange endSelector;
    private final TRefinedBy[] refinedBy;

    public RangeSelector(@NotNull TRange startSelector, @NotNull TRange endSelector, TRefinedBy[] refinedBy) {
        super(Selector.Type.RANGE_SELECTOR);
        this.startSelector = startSelector;
        this.endSelector = endSelector;
        this.refinedBy = refinedBy;
    }

    public RangeSelector(@NotNull TRange startSelector, @NotNull TRange endSelector) {
        super(Type.RANGE_SELECTOR);
        this.startSelector = startSelector;
        this.endSelector = endSelector;
        this.refinedBy = null;
    }

    public static <TRange extends Selector, TRefinedBy> RangeSelector<TRange, TRefinedBy> fromJson(JsonReader reader, JsonReaderParser<TRange> parseRangeSelector, JsonReaderParser<TRefinedBy> parseRefinedBySelector) throws IOException {
        TRange startSelector = null;
        TRange endSelector = null;
        ArrayList<TRefinedBy> refinedBy = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            switch (key) {
                case "startSelector":
                    startSelector = parseRangeSelector.invoke(reader);
                    break;
                case "endSelector":
                    endSelector = parseRangeSelector.invoke(reader);
                    break;
                case "refinedBy":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        TRefinedBy item = parseRefinedBySelector != null ? parseRefinedBySelector.invoke(reader) : null;
                        if (item != null) {
                            if (refinedBy == null) {
                                refinedBy = new ArrayList<>();
                            }
                            refinedBy.add(item);
                        }
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (startSelector != null && endSelector != null && refinedBy != null) {
            return new RangeSelector<>(startSelector, endSelector, (TRefinedBy[]) refinedBy.toArray());
        } else if (startSelector != null && endSelector != null) {
            return new RangeSelector<>(startSelector, endSelector);
        }

        return null;
    }

    public TRange getStartSelector() {
        return startSelector;
    }

    public TRange getEndSelector() {
        return endSelector;
    }

    public TRefinedBy[] getRefinedBy() {
        return refinedBy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();

        result.put("type", this.getType().toString());
        result.put("startSelector", this.getStartSelector().toJson());
        result.put("endSelector", this.getEndSelector().toJson());

        if (getRefinedBy() != null) {
            TRefinedBy[] refinedBy = getRefinedBy();
            JSONObject[] refinedByOutput = new JSONObject[refinedBy.length];
            for (int i = 0; i < getRefinedBy().length; i++) {
                if (refinedBy[i] instanceof Selector) {
                    refinedByOutput[i] = ((Selector) refinedBy[i]).toJson();
                } else {
                    throw new JSONException("Expected refinedBy to be instanceof Selector");
                }
            }
            result.put("refinedBy", new JSONArray(refinedByOutput));
        }

        return result;
    }
}
