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