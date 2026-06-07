package net.replaceitem.scarpetwebserver.script;

import carpet.script.value.Value;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.eclipse.jetty.server.Response;
import org.jetbrains.annotations.NotNull;

public class ResponseValue extends Value {
    
    private final Response response;

    public ResponseValue(Response response) {
        this.response = response;
    }
    
    public static Value of(Response response) {
        return response == null ? NULL : new ResponseValue(response);
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
    public @NotNull Tag toTag(boolean force, RegistryAccess regs) {
        return StringTag.valueOf(this.getString());
    }
}
