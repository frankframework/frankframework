(function () {
	var server; //Try and see if serverurl has been defined, if not try to deduct from local url;
	try {
		server = serverurl;
	}
	catch(e) {
		var path = window.location.pathname;

		if(path.indexOf("/iaf/gui") >= 0)
			path = path.substr(0, path.indexOf("/iaf/gui")+1);
		else
			if(path.indexOf("/", 1) >= 0)
				path = path.substr(0, path.indexOf("/", 1)+1);
		server = path;
	}
	angular.module('iaf.beheerconsole', [
		'ngCookies',                    // Angular Cookies
		'ui.router',                    // Routing
		'ui.bootstrap',                 // Ui Bootstrap
		'ngIdle',                       // Idle timer
		'ngSanitize',                   // ngSanitize
		'angular-ladda',                // Ladda
		'toaster'                       // Toastr
	]).constant("appConstants", {
		//Configure these in the server AppConstants!!!
		//The settings here are defaults and will be overwritten upon set in any .properties file.

		//Server to connect to, defaults to local server.
		"server": server,

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
		"init": -1,
		getString: function(variable) {
			return this[variable];
		},
		getBoolean: function(variable, dfault) {
			if(this[variable] != undefined) return (this[variable] === "true");
			return dfault;
		}
	});
	console.timeEnd("startup");
})();
