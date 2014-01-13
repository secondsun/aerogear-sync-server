module.exports = function(grunt) {
    'use strict';
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        uglify: {
            options: {
                banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
            },
            build: {
            }
        },
        qunit: {
            files: ['tests/sync/diff-sync-engine.html']
        },
        shell: {
            restserver: {
                options: {
                    stdout: true
                },
                command: ['cd  ../restsync/server-netty', 'mvn exec:exec'].join('&&')
            },
            diffserver: {
                options: {
                    stdout: true
                },
                command: ['cd  ../diffsync/server-netty', 'mvn exec:exec'].join('&&')
            }
        }
    });
    require('load-grunt-tasks')(grunt);
    grunt.registerTask('default', ['uglify']);
};
