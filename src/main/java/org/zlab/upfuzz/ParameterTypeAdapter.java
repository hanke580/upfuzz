package org.zlab.upfuzz;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ParameterTypeAdapter<T>
        implements JsonSerializer<T>, JsonDeserializer<T> {

    @Override
    public T deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject wrapper = (JsonObject) json;
        final JsonElement typeName = get(wrapper, "type_class");
        final JsonElement data = get(wrapper, "type_value");
        final Type actualType = typeForName(typeName);

        // if (actualType.equals(CREATE_KEYSPACE.class)) {
        // System.err.println("wow !");
        // CassandraState s = new CassandraState();
        // return (T) new CassandraCommand.CREATE_KEYSPACE(s);
        // }
        return context.deserialize(data, actualType);
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc,
            JsonSerializationContext context) {
        final JsonObject member = new JsonObject();
        member.addProperty("type_class", src.getClass().getName());
        // member.addProperty("type_value", src.getClass().getName());
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
            throw new JsonParseException(
                    "no '" + memberName +
                            "' member found in what was expected to be an interface wrapper");
        return json;
    }
}
