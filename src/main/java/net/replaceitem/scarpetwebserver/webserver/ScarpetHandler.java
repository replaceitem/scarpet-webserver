package net.replaceitem.scarpetwebserver.webserver;

import carpet.script.CarpetScriptHost;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.script.RequestValue;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.List;

import static carpet.CarpetServer.scriptServer;

public class ScarpetHandler extends Handler.Abstract {
    private final Webserver webserver;
    private final String hostname;
    private final FunctionValue callback;

    public ScarpetHandler(Webserver webserver, String hostname, FunctionValue callback) {
        this.webserver = webserver;
        this.hostname = hostname;
        this.callback = callback;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        try {
            String body = runCallback(request, response);
            Content.Sink.write(response, true, body, callback);
        } catch (Exception e) {
            ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on {}", request.getHttpURI(), e);
            callback.failed(e);
            return true;
        }
        return true;
    }

    private String runCallback(Request request, Response response) throws CommandSyntaxException, IntegrityException, InvalidCallbackException {
        CarpetScriptHost appHost = scriptServer.getAppHostByName(this.hostname);
        //noinspection ConstantValue
        if(appHost == null) {
            webserver.clearRoutes();
            throw new IntegrityException("App " + this.hostname + " not loaded");
        }
        ServerCommandSource commandSource = appHost.scriptServer().server.getCommandSource();
        CarpetScriptHost executingHost = appHost.retrieveOwnForExecution(commandSource);
        List<Value> args = List.of(RequestValue.of(request), ResponseValue.of(response));
        return executingHost.callUDF(commandSource, callback, args).getString();
    }
}
