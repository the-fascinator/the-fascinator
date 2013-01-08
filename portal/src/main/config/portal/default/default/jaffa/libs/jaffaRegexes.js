function jaffaRegexes(jaffaObject) { 
    var regex = {};

    // Inline function parsing
    regex.INLINE_FUNCTION_PARAMETERS = /^([\w$,\s]+)-\>/; // a,b->
    regex.INLINE_FUNCTION_COMPLEX = /;|([^\s\=]\s*\{)/;   // ...{ or ;

    // Any whitespace around a String
    regex.SURROUNDING_WHITESPACE = /^\s+|\s+$/g; // "  abc "

    // URL Processing/parsing
    regex.URL_HAS_PARAMS = /\?/;            // "...?..."
    regex.URL_HAS_SLASH  = /\//;            // ".../..."
    regex.URL_LAST_SLASH = /\/(?=[^\/]*$)/; // ".../.../.../..."

    return regex;
}
