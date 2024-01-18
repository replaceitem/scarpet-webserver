# scarpet-webserver

[<img alt="Available for fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/supported/fabric_vector.svg">](https://fabricmc.net/)
[<img alt="See me on GitHub" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/social/github-singular_vector.svg">](https://github.com/replaceitem)
[<img alt="Chat on Discord" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2.8.0/assets/cozy/social/discord-singular_vector.svg">](https://discord.gg/etTDQAVSgt)

## A [Carpet mod](https://modrinth.com/mod/carpet) extension for running webservers with scarpet

**Requires [Carpet mod](https://modrinth.com/mod/carpet)**

*Documentation can be found on the [Modrinth page](https://modrinth.com/mod/scarpet-webserver)*

## Usage

This mod uses [Spark](https://sparkjava.com/) for running the webserver, and adapts most its syntax from it.
For more details on the function, take a look at the [Spark docs](https://sparkjava.com/documentation).

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
    ws_response_redirect(response, '/');
));

// Using route patterns to make a player parameter in the url
ws_add_route(ws, 'get', 'api/getplayerdata/:playername', _(request, response) -> (
    p = player(request:'params':':playername');
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

### Values

This mod adds two new value types:

#### `webserver`

This is a handle to the running webserver, which use to create routes.
This can be retrieved using [`ws_init(id)`](#wsinitid).

#### `response`

This value is provided in route callbacks, and is used to assign various response data.


### Functions

#### `ws_init(id)`

Returns a [`webserver`](#webserver) value from the config `id`.

#### `ws_add_route(webserver, method, path, callback)`

Adds a route to the `webserver`.
Method can be one of `get`, `post`, `put`, `patch`, `delete`, `head`, `trace`, `connect` and `options`.
The callback can be either a string of the name of a previously declared function, or a lambda function.
The `path` can also contain placeholders (For more details, see the [Spark](https://sparkjava.com/documentation#routes) docs)

#### `ws_not_found(webserver, callback)`

Sets the handler for requests without a matching route.

#### `ws_response_set_status(response, statusCode)`

Sets a http status code for the `response`.

#### `ws_response_redirect(response, location, statusCode?)`

Sends a browser redirect to `location` for the `response`, with an optional `statusCode`.

[Spark equivalent](https://sparkjava.com/documentation#redirects)

#### `ws_response_set_content_type(response, contentType)`

Sets content type for the response.

#### `ws_response_set_header(response, header, value)`

Adds a response header.

#### `ws_response_add_cookie(response, name, value, path?, domain?, maxAge?, secure?, httpOnly?)`

Adds a cookie to the response.

* `name`: String
* `value`: String
* `Path`: Optional String
* `Domain`: Optional stirng
* `maxAge`: Optional number
* `secure`: Optional boolean
* `httpOnly`: Optional boolean

[More details about these parameters](https://docs.oracle.com/javaee%2F7%2Fapi%2F%2F/javax/servlet/http/Cookie.html)