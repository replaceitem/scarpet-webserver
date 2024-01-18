package net.replaceitem.scarpetwebserver;

import net.replaceitem.scarpetwebserver.mixin.ServiceAccessor;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.RouteImpl;
import spark.Service;
import spark.route.HttpMethod;
import spark.route.Routes;

import javax.servlet.http.HttpServletResponse;

public class Webserver {
    private final Service service;
    private final String id;
    private Route notFoundRoute;

    public Webserver(Config.WebserverConfig webserverConfig) {
        this.id = webserverConfig.id;
        service = Service.ignite();
        service.initExceptionHandler(this::onInitException);
        service.port(webserverConfig.port);
    }

    public Service getService() {
        return service;
    }

    public void addRoute(String method, String path, ScarpetRoute route) {
        service.addRoute(HttpMethod.get(method.toLowerCase()), RouteImpl.create(path, route));
    }

    public void init() {
        clearRoutes();
        service.init();
        service.after(this::afterFilter);
    }

    public void clearRoutes() {
        Routes routes = ((ServiceAccessor)(Object) service).getRoutes();
        if(routes != null) routes.clear();
        notFoundRoute = null;
    }

    public String getId() {
        return id;
    }

    public void setNotFound(Route route) {
        this.notFoundRoute = route;
    }

    public void close() {
        service.stop();
    }

    private void onInitException(Exception e) {
        ScarpetWebserver.LOGGER.error("Error initializing webserver", e);
    }

    private void afterFilter(Request request, Response response) throws Exception {
        if (response.body() == null && notFoundRoute != null) {
            response.status(HttpServletResponse.SC_NOT_FOUND);
            Object content = notFoundRoute.handle(request, response);
            if(content instanceof String stringContent) response.body(stringContent);
        }
    }
}
