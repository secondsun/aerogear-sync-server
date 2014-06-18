module.exports = function(grunt) {
    'use strict'
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
                command: ['cd  ../server-netty', 'mvn exec:exec -Prestserver'].join('&&')
            },
            diffserver: {
                options: {
                    stdout: true
                },
                command: ['cd  ../server-netty', 'mvn exec:exec -Pdiffserver'].join('&&')
            }
        }
    });
    require('load-grunt-tasks')(grunt);
    grunt.registerTask('default', ['uglify']);
};
