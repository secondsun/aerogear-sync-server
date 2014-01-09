(function() {

    module('Sync integration test');

    test('create (PUT) document', function() {
        var req = new XMLHttpRequest();
        req.open('PUT', 'http://localhost:8080/' + uuid(), false);
        req.setRequestHeader('Content-Type', 'application/json');
        req.send(JSON.stringify({content: {model: 'honda'}}));
        equal(req.status, 200, 'PUT response status should be 200');
    });

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
    }

})();
