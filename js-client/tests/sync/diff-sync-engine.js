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
        engine.addDocument( { id: 1234, clientId: 'client1', content: { name: 'Fletch' } } );
        //engine.addDocument( doc ); cannot be done as content is an object reference. Do I need to clone it before saving in the database?

        // update the name field
        doc.content.name = 'Mr.Poon';

        var diffs = engine.diff( doc );
        console.log(diffs);

        ok( diffs instanceof Array, 'diffs should be an array of tuples' );
        ok( diffs.length == 4, 'there should be 4 diff tuples generated');
        equal ( diffs[0][0], 0, 'action should be 0, which is means to leave as is');
        equal ( diffs[0][1], '{"name":"', 'should not change the "name" field');
        equal ( diffs[1][0], -1, 'action should be -1, which is means to delete');
        equal ( diffs[1][1], 'Fletch', 'Fletch was the name before the update');
        equal ( diffs[2][0], 1, 'name value action should be 1, which is means to add');
        equal ( diffs[2][1], 'Mr.Poon', 'Mr.Poon is the new name');
        equal ( diffs[3][0], 0, 'action should be 0, which is means to leave as is');
        equal ( diffs[3][1], '"}', 'closing bracket');
    });

})();
