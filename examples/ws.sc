__config() -> {
    'scope'->'global'
};

read_file_raw_any(file) -> join('\n',read_file(file,'any'));

global_root_page = read_file_raw_any('index.html');
global_player_element = read_file_raw_any('player.html');
global_404_page = read_file_raw_any('404.html');
global_postreply_template = read_file_raw_any('postreply.html');


populateTemplate(html, replacementMap) -> (
    for(keys(replacementMap),
        html = replace(html, '\\$' + _ + '\\$', replacementMap:_);
    );
    html
);


on_root(request, response) -> (
    populateTemplate(global_root_page, {
        'PLAYERS' -> join('\n',map(player('all'), populate_player_html(_)))
    })
);


populate_player_html(player) -> (
    populateTemplate(global_player_element, {
        'UUID' -> player~'uuid',
        'PLAYERNAME' -> player
    });
);

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

global_request_fields = [
    'headers',
    'method',
    'beginNanoTime',
    'connection',
    'uri',
    'pathParams',
    'body_string'
];

// Returns the request data directly for testing/debugging
ws_add_route(ws, 'get', '/requestdump', _(request, response) -> (
    ws_response_set_content_type(response, 'application/json');
    request_data = {};
    for(global_request_fields, request_data:_ = request~_);
    return(encode_json(request_data));
));

ws_add_route(ws, 'post', '/postreply', _(request, response) -> (
    return(populateTemplate(global_postreply_template, {'BODY' -> request~'body_string'}));
));

// Custom 404 page
ws_not_found(ws, _(request, response) -> global_404_page);

// SSE (Server-Sent Events) route example, storing all connection in a list
global_sse_connections = [];

ws_sse_add_route(ws, '/api/sse', _(connection) -> (
    // Basic authorization token example
    if(connection~'request'~'headers':'Authorization' != 'Bearer abcd1234', return(
        ws_sse_override_response(connection, _(request, response) -> (
            ws_response_set_status(response, 401);
            'Invalid token provided\n'
        ))
    ));
    ws_sse_add_header(connection, 'X-Custom', 'hello');
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
