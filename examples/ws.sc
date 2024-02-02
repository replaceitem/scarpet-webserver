__config() -> {
    'scope'->'global'
};

read_file_raw_any(file) -> join('\n',read_file(file,'any'));

global_root_page = read_file_raw_any('index.html');
global_player_element = read_file_raw_any('player.html');
global_404_page = read_file_raw_any('404.html');


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
    ws_response_set_status(response, 300);
    ws_response_add_header(response, 'Location', '/');
    ''
));

// Using route patterns to make a player parameter in the url
ws_add_route(ws, 'get', '/api/getplayerdata/{player}', _(request, response) -> (
    playername = request:'pathParams':'player';
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
    return(encode_json(request));
));

// Custom 404 page
ws_not_found(ws, _(request, response) -> global_404_page);