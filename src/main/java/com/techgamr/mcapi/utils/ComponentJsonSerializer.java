package com.techgamr.mcapi.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class ComponentJsonSerializer extends JsonSerializer<Component> {
    @Override
    public void serialize(Component value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        // Write the parsed node directly
        gen.writeTree(Utils.componentToJson(value));
    }
}
