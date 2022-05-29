package org.zlab.upfuzz.cassandra;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CassandraInterfaceAdapter<T>
        implements JsonSerializer<T>, JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        final JsonObject wrapper = (JsonObject) json;
        final JsonElement typeName = get(wrapper, "__gson_type");
        final JsonElement data = get(wrapper, "__gson_data");
        final Type actualType = typeForName(typeName);
        return context.deserialize(data, actualType);
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc,
            JsonSerializationContext context) {
        final JsonObject member = new JsonObject();
        member.addProperty("__gson_type", src.getClass().getName());
        member.add("__gson_data", context.serialize(src));
        return member;
    }

    private Type typeForName(final JsonElement typeElem) {
        try {
            return Class.forName(typeElem.getAsString());
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    private JsonElement get(final JsonObject wrapper, String memberName) {
        final JsonElement json = wrapper.get(memberName);
        if (json == null)
            throw new JsonParseException("no '" + memberName
                    + "' member found in what was expected to be an interface wrapper");
        return json;
    }

}
