package net.replaceitem.scarpetwebserver.script;

import carpet.script.value.Value;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import spark.Response;

public class ResponseValue extends Value {
    
    private final Response response;

    public ResponseValue(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public @NotNull String getTypeString() {
        return "webserver_response";
    }

    @Override
    public @NotNull String getString() {
        return getTypeString();
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public @NotNull NbtElement toTag(boolean force) {
        return NbtString.of(this.toString());
    }
}
