package net.replaceitem.scarpetwebserver.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import spark.Service;
import spark.route.Routes;

@Mixin(Service.class)
public interface ServiceAccessor {
    @Accessor(remap = false)
    Routes getRoutes();
}
