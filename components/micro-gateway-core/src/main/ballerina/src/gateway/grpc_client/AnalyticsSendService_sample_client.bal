public function main (string... args) {

    AnalyticsSendServiceClient ep = new("http://localhost:9090");

}

service AnalyticsSendServiceMessageListener = service {

    resource function onMessage(string message) {
        // Implementation goes here.
    }

    resource function onError(error err) {
        // Implementation goes here.
    }

    resource function onComplete() {
        // Implementation goes here.
    }
};

