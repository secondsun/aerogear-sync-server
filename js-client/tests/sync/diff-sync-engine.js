(function() {

    module( 'Sync Engine test' );

    test ( 'Sync.Engine should support creation without the new keyword', function() {
        var engine = Sync.Engine();
        ok( engine , 'Should be no problem not using new when creating' );
    });

    test( 'saveDocument', function() {
        var engine = Sync.Engine();
        engine.addDocument( { docId: 1234, content: { name: 'Fletch' } } );
        ok( true );
    });

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
