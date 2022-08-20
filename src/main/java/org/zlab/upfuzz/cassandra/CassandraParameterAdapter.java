package org.zlab.upfuzz.cassandra;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.zlab.upfuzz.cassandra.CassandraTypes.TEXTType;
import org.zlab.upfuzz.cassandra.cqlcommands.CREATE_KEYSPACE;

public class CassandraParameterAdapter<T>
        implements JsonSerializer<T>, JsonDeserializer<T> {

    @Override
    public T deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject wrapper = (JsonObject) json;
        final JsonElement typeName = get(wrapper, "__gson_type");
        final JsonElement data = get(wrapper, "__gson_data");
        final Type actualType = typeForName(typeName);

        if (actualType.equals(CREATE_KEYSPACE.class)) {
            System.err.println("wow !");
            CassandraState s = new CassandraState();
            return (T) new CREATE_KEYSPACE(s);
        }
        return context.deserialize(data, actualType);
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc,
            JsonSerializationContext context) {
        final JsonObject member = new JsonObject();
        Object parameterType = null;
        Object parameterValue = null;
        try {
            Field valueField = src.getClass().getField("value");
            Field typeField = src.getClass().getField("type");
            parameterValue = valueField.get(src);
            parameterType = typeField.get(src);
        } catch (IllegalArgumentException | IllegalAccessException
                | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        // JsonObject value =
        // context.serialize(parameterType).getAsJsonObject();
        member.add("parameter_type_class", context.serialize(parameterType));
        // member.addProperty("parameter_type_class",
        // src.getClass().getCanonicalName());
        member.addProperty("parameter_value_type_class",
                parameterValue.getClass().getCanonicalName());
        if (parameterValue instanceof TEXTType) {
            System.out.println("text: " + parameterValue);
        }
        member.add("parameter_value", context.serialize(parameterValue));
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
