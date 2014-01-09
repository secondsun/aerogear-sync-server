(function() {

    module('Sync integration test');

    test('create (PUT) document', function() {
        var documentId = uuid();
        var response = httpRequest('PUT', documentId, 'honda');
        equal(response.status, 200, 'Status should be 200');
        equal(response.id, documentId, 'id should match the sent path id parameter.');
        equal(response.content.model, 'honda', 'model property should be honda');
    });

    function httpRequest(method, id, make) {
        var req = new XMLHttpRequest();
        req.open(method, 'http://localhost:8080/' + id, false);
        req.setRequestHeader('Content-Type', 'application/json');
        req.send(JSON.stringify({content: {model: make}}));
        var json = JSON.parse(req.responseText);
        return {status: req.status, id: json.id, rev: json.rev, content: JSON.parse(json.content)};
    };

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
    }

})();
