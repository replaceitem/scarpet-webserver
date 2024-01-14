__config() -> {
    'scope'->'global'
};

global_root_page = join('\n',read_file('index.html','any'));
global_player_element = join('\n',read_file('player.html','any'));


populateTemplate(html, replacementMap) -> (
    for(keys(replacementMap),
        html = replace(html, '\\$' + _ + '\\$', replacementMap:_);
    );
    html
);


on_root(request, response) -> (
    logger(global_player_element);
    logger(map(player('all'), populate_player_html(_)));
    populate(global_root_page, {
        'PLAYERS' -> join('\n',map(player('all'), populate_player_html(_)))
    })
);


populate_player_html(player) -> (
    populate(global_player_element, {
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