package org.zlab.upfuzz.cassandra;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.zlab.upfuzz.FetchCollectionLambda;

public class LambdaInterfaceAdapter
        implements JsonSerializer<FetchCollectionLambda>,
        JsonDeserializer<FetchCollectionLambda> {

    @Override
    public FetchCollectionLambda deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context)
            throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(FetchCollectionLambda src, Type typeOfSrc,
            JsonSerializationContext context) {
        JsonObject empty = new JsonObject();
        return empty;
    }
}
