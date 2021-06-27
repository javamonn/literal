package io.literal.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import io.literal.lib.JsonArrayUtil;
import io.literal.repository.ErrorRepository;
import type.StateInput;
import type.TimeStateInput;
import type.TimeStateType;

public class TimeState extends State {

    private final URI[] cached;
    private final String[] sourceDate;

    public TimeState(@NotNull URI[] cached, @NotNull String[] sourceDate) {
        super(Type.TIME_STATE);
        this.cached = cached;
        this.sourceDate = sourceDate;
    }

    public URI[] getCached() {
        return cached;
    }

    public String[] getSourceDate() {
        return sourceDate;
    }

    public static TimeState fromJson(JSONObject json) throws JSONException {
        String[] cached = JsonArrayUtil.parseJsonStringArray(json.getJSONArray("cached"));
        URI[] parsedCache = Arrays.stream(cached)
                .map((cache) -> {
                    try {
                        return new URI(cache);
                    } catch (URISyntaxException e) {
                        ErrorRepository.captureException(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(URI[]::new);


        return new TimeState(
                parsedCache,
                JsonArrayUtil.parseJsonStringArray(json.getJSONArray("sourceDate"))
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();

        String[] stringifiedCache = Arrays.stream(this.cached).map(URI::toString).toArray(String[]::new);

        result.put("type", this.getType().name());
        result.put("cached", new JSONArray(stringifiedCache));
        result.put("sourceDate", new JSONArray(this.sourceDate));

        return result;
    }

    public StateInput toStateInput() {
        String[] stringifiedCache = Arrays.stream(this.cached).map(URI::toString).toArray(String[]::new);

        return StateInput.builder()
                .timeState(
                        TimeStateInput.builder()
                                .type(TimeStateType.TIME_STATE)
                                .cached(Arrays.asList(stringifiedCache))
                                .sourceDate(Arrays.asList(this.sourceDate))
                                .build()
                )
                .build();
    }
}
