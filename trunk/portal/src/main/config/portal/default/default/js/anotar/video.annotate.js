//Video fragments

function clearFragment() {
    player.pause();
    player.getClip().update({duration:player.getClip().fullDuration});
}

function playFragment(start, end) {
    player.pause();
    player.seek(start);
    player.getClip().update({duration:end});
    player.play();
}

function setFragmentUri(start, end) {
    warning = "";
    uri = player.getClip().url + "#";
    if (start != "" && end != "") {
        //Use Normal Play Time as described in http://www.ietf.org/rfc/rfc2326.txt
        uri += "t=npt";
        if (start != "" && start > 0) {
            uri += start + "s"
            if (player.getClip().duration < start) {
                warning += "<li>The requested start-point is greater than the video's duration.</li>";
            }
        }
        if (end != "") {
            uri += "," + end + "s";
            if (player.getClip().duration < end) {
                warning += "<li>The requested end-point is greater than the video's duration.</li>";
            }
        }
    }
    $("#fragmentUri").val(uri);
    
    if (warning != "") {
        $("#warnings").html("<ul>" + warning + "</ul>");
    }
}

/*
Media Fragment objects
*/
function TimeSegment() { 
    type = "npt",
    begin = 0,
    end = 0
}

function MediaFragment() {
    time_segment = null;
    space_segment = null;
    track_segment = null;
    name_segment = null;
}

/*
Does a basic parse of the W3C's Media Fragments URI 1.0 
but only the temporal (t) dimension.

Refer to http://www.w3.org/TR/2009/WD-media-frags-20091217/#naming-syntax
*/
function parseMediaFragmentUri(uri, message_div){

    var media_fragment = new MediaFragment();

    // const not working in IE7/8
//    const re_fragment = /[^?#]*[?#]([^#]*)/;
//    const axissegment = new Array("t", "xywh", "track", "id");
    var re_fragment = /[^?#]*[?#]([^#]*)/;
    var axissegment = new Array("t", "xywh", "track", "id");
    
    var query = uri.split(re_fragment);
    if (query.length <= 1 || query[1] == "") return null;
    
    var params = query[1].split("&");
    
    for (i=0;i<params.length;i++) {
        var keyval = params[i].split("=");
        if (keyval.length < 2) continue;
        
        var key = keyval[0];
        var value = keyval[1];
        if (axissegment.indexOf(key) == -1) continue;
        
        if (key=="t") handleTimeSegment(value);
        
        msg += "<li>Param " + i + "(" + params[i] + "): " + key + " = " + value + "</li>";
    }
    
    //Display test strings
    $("#" + message_div).html("<ul>" + msg + "</ul>");
}

function handleTimeSegment(segment) {
    var segment = new TimeSegment();
    //const re_timeparam = /^(npt|smpte|smpte-25|smpte-30|smpte-30-drop|clock):{0,1}(.*)/;
    var re_timeparam = /^(npt|smpte|smpte-25|smpte-30|smpte-30-drop|clock):{0,1}(.*)/;
    
    var timeparam = re_timeparam.exec(segment);
    if (timeparam == null) return null;
    
    alert(timeparam[0]);
    return segment;
}
