(function() {

    module( 'Sync Engine test' );

    test ( 'Sync.Engine should support creation without the new keyword', function() {
        var engine = Sync.Engine();
        ok( engine , 'Should be no problem not using new when creating' );
    });

    test( 'saveDocument', function() {
        var engine, doc, syncstore = AeroGear.DataManager( { name: 'syncstore', recordId: 'id' } ).stores.syncstore;
        equal( syncstore.getRecordId(), 'id', 'RecordId should be docId' );
        engine = Sync.Engine( syncstore );
        engine.addDocument( { id: 1234, clientId: 'client1', content: { name: 'Fletch' } } );
        doc = syncstore.read( 1234 )[0];
        equal( doc.docId, 1234, 'Document id should match' );
    });

})();
