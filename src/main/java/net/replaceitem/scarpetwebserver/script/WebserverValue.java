package net.replaceitem.scarpetwebserver.script;

import carpet.script.value.Value;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import spark.Service;

public class WebserverValue extends Value {

    private final Service service;

    public WebserverValue(Service service) {
        this.service = service;
    }

    public WebserverValue() {
        this.service = Service.ignite();
    }

    public Service getService() {
        return service;
    }

    @Override
    public @NotNull String getTypeString() {
        return "webserver";
    }


    @Override
    public @NotNull String getString() {
        return "webserver(" + this.service.port() + ")";
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
