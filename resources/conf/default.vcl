# vcl config for the varnish container used in response-caching feature

vcl 4.1;
import std;

backend default {
    .host = "router";
    .port = "9096";
}

# define who is authorize to purge/invalidate the cache
acl purge {
    "localhost";
    "router";
    "varnish";
    "192.168.0.1";
}

sub vcl_recv {

    #liveness and rediness
    if (req.url == "/varnish-ping") {
        return(synth(200));
    }
        
    if (req.url == "/varnish-ready") {
        return(synth(200));
    }

    if (req.method == "PURGE") {
        if (!client.ip ~ purge) {
        return(synth(405));
        }
        if(!req.http.x-invalidate-pattern) {
        return(purge);
        }
        ban("req.url ~ " + req.http.x-invalidate-pattern 
        + " && req.http.host == " + req.http.host);
        return (synth(200,"Ban added"));
    }

     set req.http.Host = req.http.x-wso2-actual-host;
     set req.url = req.http.x-wso2-request-path;
     set req.url = std.querysort(req.url);

}

sub vcl_miss {
    set req.http.x-wso2-cluster-header = req.http.x-wso2-actual-cluster; 
    return (fetch);   
}

sub vcl_backend_fetch {
    if (bereq.http.x-wso2-request-path) {       
        set bereq.url = bereq.http.x-wso2-request-path;
        set bereq.url = std.querysort(bereq.url);
        unset bereq.http.x-wso2-request-path;
        unset bereq.http.x-wso2-actual-cluster;
    }
}

sub vcl_backend_response {
    # List of status codes that should be marked as uncacheable
    if (beresp.status == 300 || beresp.status == 301 || beresp.status == 302 ||
        beresp.status == 307 || beresp.status == 304 || beresp.status == 404 ||
        beresp.status == 410 || beresp.status == 414) {
        set beresp.uncacheable = true;
    }

    if (bereq.http.x-cache-default-ttl && !beresp.http.Cache-Control) {
        set beresp.ttl = std.duration(bereq.http.x-cache-default-ttl + "s", 120s);
    }
    
    # aloows to respect the must-revalidate cache-control header
    if(beresp.http.Cache-Control ~ "must-revalidate") {
        set beresp.grace = 0s;
    }

    # Determine the appropriate storage
    if (bereq.http.x-cache-partition == "slot1") {
        set beresp.storage = storage.slot1;
    } else if (bereq.http.x-cache-partition == "slot2") {
        set beresp.storage = storage.slot2;
    } else {
        set beresp.storage = storage.default;
    }
    
    if (beresp.storage == storage.default) {
        set beresp.http.x-storage = "default";
    } else{
        set beresp.http.x-storage = bereq.http.x-cache-partition;
    }
}

sub vcl_deliver {
    if (obj.hits > 0) {
        set resp.http.X-Cached-By = "Varnish";
        set resp.http.X-Cache-Info = "Cached under host: " + req.http.Host + "; Request URI: " + req.url;
    }
}



