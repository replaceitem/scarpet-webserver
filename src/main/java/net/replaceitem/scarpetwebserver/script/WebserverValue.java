package net.replaceitem.scarpetwebserver.script;

import carpet.script.value.Value;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.replaceitem.scarpetwebserver.webserver.Webserver;
import org.jetbrains.annotations.NotNull;

public class WebserverValue extends Value {

    private final Webserver webserver;

    public WebserverValue(Webserver webserver) {
        this.webserver = webserver;
    }

    public Webserver getWebserver() {
        return webserver;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WebserverValue webserverValue && webserverValue.webserver.getId().equals(this.webserver.getId());
    }

    @Override
    public int hashCode() {
        return webserver.hashCode();
    }

    @Override
    public @NotNull String getTypeString() {
        return "webserver";
    }


    @Override
    public @NotNull String getString() {
        return "webserver(" + this.webserver.getId() + ")";
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public @NotNull NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        return NbtString.of(this.toString());
    }
}
