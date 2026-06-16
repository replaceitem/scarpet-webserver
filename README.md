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

You can take a look at the full working example with the html files [here](https://github.com/replaceitem/scarpet-webserver/tree/master/examples).
The example also contains more routes and a simple templating mechanism.

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
    ws_response_set_status(response, 301);
    ws_response_add_header(response, 'Location', '/');
    ''
));

// Using route patterns to make a player parameter in the url
ws_add_route(ws, 'get', '/api/getplayerdata/{player}', _(request, response) -> (
    playername = request~'pathParams':'player';
    p = player(playername);
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
    request_data = {};
    for(global_request_fields, request_data:_ = request~_);
    return(encode_json(request_data));
));

// Custom 404 page
ws_not_found(ws, _(request, response) -> global_404_page);

// SSE (Server-Sent Events) route example, storing all connection in a list
global_sse_connections = [];

ws_sse_add_route(ws, '/api/sse', _(connection) -> (
    global_sse_connections += connection
));

// Function for pruning closed connections from the list.
// Can either be manually called, or run repeatedly using the schedule function.
prune_closed_connections() -> global_sse_connections = filter(global_sse_connections, !_~'closed');

// Listen for block place events and send messages to all connections
__on_player_places_block(player, item_tuple, hand, block) -> (
    n = for(global_sse_connections, ws_sse_send_message(_, 'place', player + ' placed ' + block + ' with their ' + hand));
    print('sent message to ' + n + ' SSE connections');
    prune_closed_connections()
);
```


### Route callback

The callback function for routes should have this signature:

```js
_(request, response) -> (
    // do stuff
    return('html body...')
)
```

The `request` parameter provides a [request value](#request) of all the request details.
You can also use the [example script](#example-syntax) for testing, which has a `/requestdump` route that sends all the request data as json back.

The `response` is a [response value](#response).

The value returned by the function will be the response body (in most cases the html page) to be sent.

### SSE (Server-Sent Events) Route callback

The callback function for SSE routes should have this signature:

```js
_(connection) -> (
    // access request data with connection~'request',
    // set response headers with ws_sse_add_header,
    // save connection for sending messages to with sse_send_message
    global_connection_list += connection
)
```

The `connection` is an [SSE connection value](#sse_connection).

The value returned by the function is ignored and will not be used for anything. If you wish to immediately send data upon connection,
then [ws_sse_send_message](#ws_sse_send_messagesse_connection-optional-event-data-optional-callback)
or [ws_sse_override_response](#ws_sse_override_responsesse_connection-callback) can be called inside this callback.

### Values

This mod adds two new value types:

#### `webserver`

This is a handle to the running webserver, which use to create routes.
This can be retrieved using [`ws_init(id)`](#ws_initid).

#### `request`

This value is provided in route callbacks, and is used to retrieve various request data.
Note that retrieving values from this value needs to be done using the `~` query operator,
but some of those values are maps, which are accessed using `:`.
You can run the example script and send a request to `/requestdump` to get all request data returned for testing.

##### `request~'headers'`

Returns a map with string keys (header names) and string values.
If a header has multiple values, it is a list of strings.

For example, the `Referer` header can be accessed with `request~'headers':'Referer'`

Example header map:

```js
{
    'Accept'-> [
      'text/html',
      'application/xhtml+xml',
      'application/xml;q=0.9',
      '*/*;q=0.8'
    ],
    'Connection' -> 'keep-alive',
    'User-Agent' -> 'Mozilla/5.0 (Windows NT 10.0;Win64;x64;rv:138.0) Gecko/20100101 Firefox/138.0'
}
```

##### `request~'method'`

Returns the [HTTP-Method](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods) of the request.

##### `request~'beginNanoTime'`

Returns the time in nanoseconds when the request was received.

##### `request~'connection'`

Returns a map with information about the connection.

It has the following keys:

* `protocol`: The protocol of the connection (e.g. `HTTP/1.1`)
* `httpVersion`: The HTTP-Version (e.g. `HTTP/1.1`)
* `id`: Returns a unique id for the network connection (within the lifetime of the server)
* `persistent`: Whether the connection is persistent
* `secure`: Whether the connection is secure (using HTTPS)

##### `request~'uri'`

Returns a map with information about the requested URI.

For more information on these fields, see
[Anatomy of a URL](https://developer.mozilla.org/en-US/docs/Learn_web_development/Howto/Web_mechanics/What_is_a_URL#basics_anatomy_of_a_url)
in the MDN web docs.

It has the following keys:

* `scheme`: The scheme of the request (usually `https` or `http`)
* `authority`: The domain and port (e.g. `localhost:8000`, `my.example.com`)
* `host`: The domain
* `post`: The port as a number
* `path`: The path of the request (e.g. `/api/users`)
* `canonicalPath`: The path of the request with all special characters (except /) URL-encoded
* `decodedPath`: The path of the request with all URL-encoded characters decoded
* `param`: The last path parameter or `null`
* `query`: The query parameters in the url (e.g. `search=test&limit=50`)
* `fragment`: Never actually sent to the server, should always be `null`
* `user`: The [user](https://developer.mozilla.org/en-US/docs/Learn_web_development/Howto/Web_mechanics/What_is_a_URL#url_usernames_and_passwords) of the url 
* `asString`: The full URI as a string
* `queryParameters`: The query parameters parsed as map. Accessing one query parameter can be done with `request~'uri':'queryParameters':'search'`

##### `request~'pathParams'`

Returns a map with all path parameters.

When creating an endpoint with a path parameter like this: `/api/getplayerdata/{player}`,
the player parameter in the path can be retrieved like this:

```js
ws_add_route(ws, 'get', '/api/getplayerdata/{player}', _(request, response) -> (
    playername = request~'pathParams':'player';
    ...
));
```

##### `request~'body_string'`

Returns the body of the request as a string.

#### `response`

This value is provided in route callbacks, and is used to assign various response data.

#### `sse_connection`

This is a handle for a connection to an SSE endpoint, which used to send messages or close an active connection.
This can be retrieved using the callback argument of [`ws_sse_add_route`](#ws_sse_add_routewebserver-path-callback).

##### `sse_connection~'request'`

Returns the [request](#request) associated with the SSE connection.

##### `sse_connection~'closed'`

Returns a boolean value indicating whether the SSE connection is currently closed.
This value will be `true` after [sse_close](#ws_sse_closesse_connection-optional-callback) is called,
or after a call to [sse_send_message](#ws_sse_send_messagesse_connection-optional-event-data-optional-callback) fails with an EOF exception.

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

Sets an http status code for the `response`.

#### `ws_response_set_content_type(response, contentType)`

Sets content type for the response.

#### `ws_response_add_header(response, header, value)`

Adds a response header.

#### `ws_sse_add_route(webserver, path, callback)`

Adds an SSE (Server-Sent Events) route to the `webserver`.
The callback can be either a string of the name of a previously declared function, or a lambda function.
See the [SSE route callback](#sse-server-sent-events-route-callback) section for more details.
The `path` uses jetty's [UriTemplatePathSpec](https://eclipse.dev/jetty/javadoc/jetty-12/org/eclipse/jetty/http/pathmap/UriTemplatePathSpec.html),
so you can use [its syntax (Level 1)](https://tools.ietf.org/html/rfc6570).

It supports path parameters like `/sse/{room}`, which can then be retrieved using `connection~'request'~'pathParams':'room'`.

#### `ws_sse_override_response(sse_connection, callback)`

Overrides the default behavior of an SSE connection and gain access to its underlying `request` and `response` via the callback argument.
The callback argument behaves the same as the [route callback](#route-callback) and receives same the `request` and `response` arguments.
The return value of the callback is used as the response body, and the connection is closed once that data is sent, rather than being kept open like a normal SSE connection.
The usual [ws_response_set_status](#ws_response_set_statusresponse-statuscode), [ws_response_set_content_type](#ws_response_set_content_typeresponse-contenttype),
and [ws_response_add_headerresponse](#ws_response_add_headerresponse-header-value) functions will all work as expected inside the callback as well.
The main purpose of this function is for being able to send back error responses from SSE routes under certain scenarios, like for authorization purposes.

This function will only work inside the callback of `ws_add_sse_route`, since no response has been sent yet at that point.
If this is called at a later point after the initial SSE response has already been made, this function will do nothing and return the boolean `false`,
otherwise `true` will be returned to indicate the response was able to be overridden.

#### `ws_sse_add_header(sse_connection, header, value)`

Adds a response header to an SSE connection.

This function will only work if it is called from within the `ws_sse_add_route`, as the response headers are sent after that callback completes.
If this is called at a later point after the initial SSE response has already been made, this function will do nothing and return the boolean `false`,
otherwise `true` will be returned to indicate the response header was added.

#### `ws_sse_send_message(sse_connection, optional event, data, optional callback)`

Sends a message to an existing SSE connection handle, given an optional event name and the event data to send.

A callback can optionally be provided, which will be called when either the message is successfully sent, or an error occurs while sending.
The first argument passed to the callback is the same `sse_connection` that was provided to `ws_sse_send_message`, and the second argument is
a boolean indicating whether the operation was successful (`true` for success, and `false` for failure).

#### `ws_sse_close(sse_connection, optional callback)`

Closes an existing SSE connection.

A callback can optionally be provided, which will be called when either the connection is successfully closed, or an error occurs while closing.
The first argument passed to the callback is the same `sse_connection` that was provided to `ws_sse_close`, and the second argument is
a boolean indicating whether the operation was successful (`true` for success, and `false` for failure).
