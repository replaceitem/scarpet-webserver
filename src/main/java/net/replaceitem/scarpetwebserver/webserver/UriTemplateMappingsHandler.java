package net.replaceitem.scarpetwebserver.webserver;

import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.http.pathmap.MatchedResource;
import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.Dumpable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class UriTemplateMappingsHandler extends Handler.AbstractContainer {
    
    public static final String PATH_PARAMETER_ATTRIBUTE = "path_parameters";

    private static final Logger LOG = LoggerFactory.getLogger(UriTemplateMappingsHandler.class);

    private final PathMappings<Handler> mappings = new PathMappings<>();

    public UriTemplateMappingsHandler() {
        super(true);
    }

    @Override
    public List<Handler> getHandlers() {
        return mappings.streamResources().map(MappedResource::getResource).toList();
    }

    public void addMapping(UriTemplatePathSpec uriTemplatePathSpec, Handler handler) {
        //if (isStarted())
        //    throw new IllegalStateException("Cannot add mapping: " + this);

        // check that self isn't present
        if (handler == this)
            throw new IllegalStateException("Unable to addHandler of self: " + handler);

        // check for loops
        if (handler instanceof Handler.Container container && container.getDescendants().contains(this))
            throw new IllegalStateException("loop detected: " + handler);

        // add new mapping and remove any old
        Handler old = mappings.get(uriTemplatePathSpec);
        mappings.put(uriTemplatePathSpec, handler);
        updateBean(old, handler);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        Dumpable.dumpObjects(out, indent, this, mappings);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String pathInContext = Request.getPathInContext(request);
        MatchedResource<Handler> matchedResource = mappings.getMatched(pathInContext);
        if (matchedResource == null)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("No mappings matched {}", pathInContext);
            return false;
        }
        Handler handler = matchedResource.getResource();
        if (LOG.isDebugEnabled())
            LOG.debug("Matched {} to {} -> {}", pathInContext, matchedResource.getPathSpec(), handler);
        if(matchedResource.getPathSpec() instanceof UriTemplatePathSpec uriTemplatePathSpec) request.setAttribute(PATH_PARAMETER_ATTRIBUTE, uriTemplatePathSpec.getPathParams(pathInContext));
        boolean handled = handler.handle(request, response, callback);
        if (LOG.isDebugEnabled())
            LOG.debug("Handled {} {} by {}", handled, pathInContext, handler);
        return handled;
    }
}
