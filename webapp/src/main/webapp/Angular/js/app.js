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

		//How often the interactive frontend should poll the IAF API for new data
		"console.pollerInterval": 30000,

		//How often the interactive frontend should poll during IDLE state
		"console.idle.pollerInterval": 180000,

		//After x minutes the app goes into 'idle' state (use 0 to disable)
		"console.idle.time": 300,

		//After x minutes the user will be forcefully logged out
		"console.idle.timeout": 0,

		//Time format in which to display the time and date.
		"console.dateFormat": "yyyy-MM-dd HH:mm:ss",

		//These will automatically be updated.
		"timeOffset": 0,
		"init": 0,
	});
})();