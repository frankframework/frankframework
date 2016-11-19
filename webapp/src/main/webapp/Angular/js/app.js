(function () {
	angular.module('iaf.beheerconsole', [
        'ui.router',                    // Routing
        'oc.lazyLoad',                  // ocLazyLoad
        'ui.bootstrap',                 // Ui Bootstrap
        'pascalprecht.translate',       // Angular Translate
        'ngIdle',                       // Idle timer
        'ngSanitize'                    // ngSanitize
    ]).constant("appConstants", {
        //Configure these in the server AppConstants!!!
        //The settings here are defaults and will be overwritten upon set in any .properties file.
        




        //Server to connect to, defaults to local server.
        //"server": "http://"+window.location.host+"/iaf-example/",

        //After x minutes the app goes into 'idle' state (use 0 to disable)
        "idleTime": 300,

        //After x minutes the user will be forcefully logged out
        "idleTimeout": 300,

        //Global timeformat
        "format": "yyyy-MM-dd HH:mm:ss",

        //These will automatically be updated.
        "timeOffset": 0,
        "init": 0,
    });
})();