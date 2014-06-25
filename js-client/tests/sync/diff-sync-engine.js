(function() {

    module( 'Sync Engine test' );

    test ( 'Sync.Engine should support creation without the new keyword', function() {
        var engine = Sync.Engine();
        ok( engine , 'Should be no problem not using new when creating' );
    });

    test( 'save document', function() {
        var engine = Sync.Engine(), doc = { id: 1234, clientId: 'client1', content: { name: 'Fletch' } };
        engine.addDocument( { id: 1234, clientId: 'client1', content: { name: 'Fletch' } } );
        var actualDoc = engine.getDocument( 1234 );
        equal( actualDoc.id, 1234, 'Document id should match' );
    });

    test( 'diff document', function() {
        var engine = Sync.Engine();
        var doc = { id: 1234, clientId: 'client1', content: { name: 'Fletch' } };
        engine.addDocument( doc );

        // update the name field
        doc.content.name = 'Mr.Poon';

        var edits = engine.diff( doc );
        equal ( edits.docId, 1234, 'document id should be 1234');
        equal ( edits.clientId, 'client1', 'clientId should be client1');
        equal ( edits.version, 0, 'version should be zero');
        equal ( edits.checksum, '', 'checksum is currently not implemented.');
        var diffs = edits.diffs;
        ok( diffs instanceof Array, 'diffs should be an array of tuples' );
        ok( diffs.length == 4, 'there should be 4 diff tuples generated');
        equal ( diffs[0].operation, 'UNCHANGED', 'operation should be UNCHANGED');
        equal ( diffs[0].text, '{"name":"', 'should not change the "name" field');
        equal ( diffs[1].operation, 'DELETE' ,'operation should be DELETE');
        equal ( diffs[1].text, 'Fletch', 'Fletch was the name before the update');
        equal ( diffs[2].operation, 'ADD', 'operation should be ADD');
        equal ( diffs[2].text, 'Mr.Poon', 'Mr.Poon is the new name');
        equal ( diffs[3].operation, "UNCHANGED", 'operation should be UNCHANGED');
        equal ( diffs[3].text, '"}', 'closing bracket');
    });

    test( 'patch document', function() {
        var engine = Sync.Engine();
        var doc = { id: 1234, clientId: 'client1', content: {name: 'Fletch' } };
        engine.addDocument( doc );

        // update the name field
        doc.content.name = 'Mr.Poon';

        var edits = engine.diff( doc );
        var patched = engine.patch( edits );
        equal( patched[1][0], true, 'patch should have been successful.' );
        equal( patched[0], '{"name":"Mr.Poon"}', 'name should have been updated' );
    });

})();
