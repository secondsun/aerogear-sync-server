(function() {

    module( 'Sync Engine test' );

    test ( 'Sync.Engine should support creation without the new keyword', function() {
        var engine = Sync.Engine();
        ok( engine , 'Should be no problem not using new when creating' );
    });

    test( 'saveDocument', function() {
        var engine, doc, syncstore = AeroGear.DataManager( { name: 'syncstore', recordId: 'docId' } ).stores.syncstore;
        equal( syncstore.getRecordId(), 'docId', 'RecordId should be docId' );
        engine = Sync.Engine( syncstore );
        engine.addDocument( { docId: 1234, content: { name: 'Fletch' } } );
        doc = syncstore.read( 1234 )[0];
        equal( doc.docId, 1234, 'Document id should match' );
    });

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
