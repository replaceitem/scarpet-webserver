# scarpet-webserver

[<img alt="Available for fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/supported/fabric_vector.svg">](https://fabricmc.net/)
[<img alt="See me on GitHub" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/social/github-singular_vector.svg">](https://github.com/replaceitem)
[<img alt="Available on Modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/available/modrinth_vector.svg">](https://modrinth.com/mod/scarpet-webserver)
[<img alt="Chat on Discord" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/social/discord-singular_vector.svg">](https://discord.gg/etTDQAVSgt)

## A [Carpet mod](https://modrinth.com/mod/carpet) extension for running webservers with scarpet

**Requires [Carpet mod](https://modrinth.com/mod/carpet)**

*Warning: Only use this mod with scripts you trust or made yourself. Use this at your own risk.*

This project uses and includes [Jetty 12](https://eclipse.dev/jetty/) in builds for the webserver ([License](https://github.com/jetty/jetty.project/blob/jetty-12.0.x/LICENSE))

## Usage

To use a webserver in scarpet, it has to be defined in the `scarpet-webserver.json` config file first.
When the mod first loads, it creates a sample config, where you can define multiple webservers. Each one has a unique `id` and a `port`.
An example config could look like this:

```json
{
  "webservers": [
    {
      "id": "myserver",
      "port": 80
    }
  ]
}
```

This server can then be used in a script.

### Example syntax

You can take a look at the full working example with the html files [here](https://github.com/replaceitem/scarpet-webserver/tree/master/examples)

```js
// Initialize the webserver with id 'test' (Defined in the config)
ws = ws_init('test');

// Handle the root path (Callback given as a function name)
ws_add_route(ws, 'get', '/', 'on_root');

// Changing content type for an api 
ws_add_route(ws, 'get', '/api/players', _(request, response) -> (
    ws_response_set_content_type(response, 'application/json');
    encode_json({'players'->player('all')});
));

// Example for redirecting /redirect to /
ws_add_route(ws, 'get', '/redirect', _(request, response) -> (
    ws_response_add_header(response, 'Location', '/');
    ws_response_set_status(response, 300);
));

// Using route patterns to make a player parameter in the url
ws_add_route(ws, 'get', '/api/getplayerdata/{playername}', _(request, response) -> (
    p = player(request:'pathParams':'playername');
    ws_response_set_content_type(response, 'application/json');
    if(p == null,
        ws_response_set_status(response, 400);
    return(encode_json({'error'->'Invalid player'}));
    );
    return(encode_json(parse_nbt(p~'nbt')));
));

// Returns the request data directly for testing/debugging
ws_add_route(ws, 'get', '/requestdump', _(request, response) -> (
    ws_response_set_content_type(response, 'application/json');
    return(encode_json(request));
));

// Custom 404 page
ws_not_found(ws, _(request, response) -> global_404_page);
```


### Route callback

The callback function for routes should have this signature:

```js
_(request, response) -> (
    // do stuff
    return('html body...')
)
```

The `request` parameter provides a map of all the request details.
You can also use the [example script](#example-syntax) for testing, which has a `/requestdump` route that sends all the request data as json back.

The `response` is a [response value](#response).

The value returned by the function will be the response body (in most cases the html page) to be sent.

### Values

This mod adds two new value types:

#### `webserver`

This is a handle to the running webserver, which use to create routes.
This can be retrieved using [`ws_init(id)`](#ws_initid).

#### `response`

This value is provided in route callbacks, and is used to assign various response data.


### Functions

#### `ws_init(id)`

Returns a [`webserver`](#webserver) value from the config `id`.
This also clears all routes and starts it, if it isn't already.

#### `ws_add_route(webserver, method, path, callback)`

Adds a route to the `webserver`.
`method` is the http method, like `get` or `post`.
The callback can be either a string of the name of a previously declared function, or a lambda function.
See the [route callback](#route-callback) section for more details.
The `path` uses jetty's [UriTemplatePathSpec](https://eclipse.dev/jetty/javadoc/jetty-12/org/eclipse/jetty/http/pathmap/UriTemplatePathSpec.html),
so you can use [its syntax (Level 1)](https://tools.ietf.org/html/rfc6570).

It supports path parameters like `/shop/{product}`, which can then be retrieved using `request:'pathParams':'product'`.

#### `ws_not_found(webserver, callback)`

Sets the handler for requests without a matching route.

#### `ws_response_set_status(response, statusCode)`

Sets a http status code for the `response`.

#### `ws_response_set_content_type(response, contentType)`

Sets content type for the response.

#### `ws_response_add_header(response, header, value)`

Adds a response header.