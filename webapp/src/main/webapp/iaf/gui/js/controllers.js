/**
 * MainCtrl - controller
 * Used on all pages except login/logout
 *
 */
angular.module('iaf.beheerconsole')
.controller('MainCtrl', ['$scope', '$rootScope', 'appConstants', 'Api', 'Hooks', '$state', '$location', 'Poller', 'Notification', 'dateFilter', '$interval', 'Idle', '$http', 'Misc', '$uibModal', 'Session', 'Debug', 'SweetAlert', '$timeout',
	function($scope, $rootScope, appConstants, Api, Hooks, $state, $location, Poller, Notification, dateFilter, $interval, Idle, $http, Misc, $uibModal, Session, Debug, SweetAlert, $timeout) {
	$scope.loading = true;
	$rootScope.adapters = {};
	Pace.on("done", function() {
		if(appConstants.init == 0) {
			Api.Get("server/info", function(data) {
				appConstants.init = 2;
				if(!($location.path().indexOf("login") >= 0)) {
					Idle.watch();
					angular.element("body").removeClass("gray-bg");
					angular.element(".main").show();
					angular.element(".loading").hide();
				}

				var serverTime = Date.parse(new Date(data.serverTime).toUTCString());
				var localTime  = Date.parse(new Date().toUTCString());
				appConstants.timeOffset = serverTime - localTime;
				//setTime(appConstants.timeOffset);

				function updateTime() {
					var serverDate = new Date();
					serverDate.setTime(serverDate.getTime() - appConstants.timeOffset);
					$scope.serverTime = dateFilter(serverDate, appConstants["console.dateFormat"]);
				}
				$interval(updateTime, 1000);
				updateTime();

				angular.element(".iaf-info").html("IAF " + data.version + ": " + data.name );

				$scope.configurations = data.configurations;

				//Was it able to retrieve the serverinfo without logging in?
				if(!$scope.loggedin) {
					Idle.setTimeout(false);
				}
				Hooks.call("init", false);
			}, function(message, statusCode, statusText) {
				if(statusCode == 500) {
					$timeout(function(){
						angular.element(".main").show();
						angular.element(".loading").hide();
					}, 100);
					$state.go("initError");
				}
			});
			appConstants.init = 1;
			Api.Get("environmentvariables", function(data) {
				if(data["Application Constants"]) {
					appConstants = $.extend(appConstants, data["Application Constants"]);
					Hooks.call("appConstants", appConstants);
					var idleTime = (parseInt(appConstants["console.idle.time"]) > 0) ? parseInt(appConstants["console.idle.time"]) : false;
					if(idleTime > 0) {
						var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
						Idle.setIdle(idleTime);
						Idle.setTimeout(idleTimeout);
					}
					else {
						Idle.unwatch();
					}
					if(appConstants["otap.stage"] == "LOC") {
						Debug.setLevel(3);
					}
				}
			});
		}
	});

	var token = sessionStorage.getItem('authToken');
	$scope.loggedin = (token != null && token != "null") ? true : false;

	$scope.reloadRoute = function() {
		$state.reload();
	};

	$scope.alerts = [];

	$scope.addAlert = function(type, message) {
		var exists = false;
		for(alert in $scope.alerts) {
			if( $scope.alerts[alert].message == message)
				exists = true;
		}
		if(!exists)
			$scope.alerts.push({type: type, message: message});
	};

	$scope.closeAlert = function(index) {
		$scope.alerts.splice(index, 1);
	};

	$scope.adapterSummary = {
		started:0,
		stopped:0,
		starting:0,
		stopping:0,
		error:0
	};
	$scope.receiverSummary = {
		started:0,
		stopped:0,
		starting:0,
		stopping:0,
		error:0
	};
	$scope.messageSummary = {
		info:0,
		warn:0,
		error:0
	};

	Hooks.register("init:once", function() {
		/* Check IAF version */
		var Xrate = Session.get("X-RateLimit");
		if(Xrate == undefined || Xrate.reset < parseInt(new Date().getTime()/1000)) {
			$http.get("https://api.github.com/rate_limit", {headers:{"Authorization": undefined}}).then(function(response) {
				if(!response  || !response.data) return false;
	
				var GithubXrate = {
					limit: parseInt(response.headers("X-RateLimit-Limit")) || 0,
					remaining: parseInt(response.headers("X-RateLimit-Remaining")) || 0,
					reset: parseInt(response.headers("X-RateLimit-Reset")) || 0,
					time: parseInt(new Date().getTime()/1000),
				};
				console.log("Fetching X-RateLimit from api.GitHub.com...", GithubXrate);
				Session.set("X-RateLimit", GithubXrate);
			});

			$http.get("https://api.github.com/repos/ibissource/iaf/releases", {headers:{"Authorization": undefined}}).then(function(response) {
				if(!response  || !response.data) return false;

				var release = response.data[0]; //Not sure what ID to pick, smallest or latest?
				handleGitHubResponse(release);
			});
		}
		else {
			var release = Session.get("IAF-Release");
			if(release != null) {
				Notification.add('fa-exclamation-circle', "IAF update available!", false, function() {
					$location.path("iaf-update");
				});
			}
		}
		function handleGitHubResponse(release) {
			var newVersion = (release.tag_name.substr(0, 1) == "v") ? release.tag_name.substr(1) : release.tag_name;
			var currentVersion = appConstants["application.version"];
			var version = Misc.compare_version(newVersion, currentVersion);
			console.log("Checking IAF version with remote...");
			console.log("Comparing version: '"+currentVersion+"' with latest release: '"+newVersion+"'.");
			Session.remove("IAF-Release");

			if(version > 0) {
				Session.set("IAF-Release", release);
				Notification.add('fa-exclamation-circle', "IAF update available!", false, function() {
					$location.path("iaf-update");
				});
			}
		};

		Api.Get("server/warnings", function(warnings) {
			for(i in warnings) {
				var warning = warnings[i];
				var type = "warning";
				if(warning.type && (warning.type == "exception" || warning.type == "severe")) {
					type = "danger";
				}
				$scope.addAlert(type, warning.message);
			}
		});

		Api.Get("adapters", function(allAdapters) {
			Hooks.call("adaptersLoaded", allAdapters);

			$rootScope.adapters = allAdapters;
			$scope.displayAdapters = [];
			for(adapter in allAdapters) {
				$scope.adapterSummary[allAdapters[adapter].state] += 1;
				Poller.add("adapters/" + adapter + "?expanded=all", function(data) {
					var oldAdapterData = $rootScope.adapters[data.name];
					if(oldAdapterData != data) {
						data.status = "started";

						for(x in data.receivers) {
							if(data.receivers[x].started == false)
								data.status = 'warning';
						}
						data.hasSender = false;
						for(x in data.pipes) {
							if(data.pipes[x].sender) {
								data.hasSender = true;
							}
						}
						for(i in data.messages) {
							var level = data.messages[i].level;
							if(level != "INFO")
								data.status = 'warning';
						}
						if(!data.started)
							data.status = "stopped";

						$rootScope.adapters[data.name] = data;

						$scope.updateAdapterSummary(true);
						Hooks.call("adapterUpdated", data);
					}
				}, true);
			}
		}, function() {
			$scope.addAlert('danger', "An error occured while trying to load adapters!");
		});
	});

	var lastUpdated = 0;
	var timeout = null;
	$scope.updateAdapterSummary = function(executeTimeout) {
		var updated = (new Date().getTime());
		if(updated - 3000 < lastUpdated && executeTimeout) { //3 seconds
			clearTimeout(timeout);
			timeout = setTimeout(updateAdapterSummary, 1000);
			return;
		}
		
		var adapterSummary = {
			started:0,
			stopped:0,
			starting:0,
			stopping:0,
			error:0
		};
		var receiverSummary = {
			started:0,
			stopped:0,
			starting:0,
			stopping:0,
			error:0
		};
		var messageSummary = {
			info:0,
			warn:0,
			error:0
		};
		
		var allAdapters = $rootScope.adapters;
		var name = $rootScope.selectedConfiguration;
		
		// Loop trough adapters
		for(x in allAdapters){
			var adapter = allAdapters[x];
			
			// Only adapters for active config
			if(adapter.configuration == name || name == 'All'){
				adapterSummary[adapter.status]++;
				
				// Loop trough receivers of adapter
				for(y in adapter.receivers){
					receiverSummary[adapter.receivers[y].state]++;
				}
				// Loop trough messages of adapter
				for(z in adapter.messages) {
					messageSummary[adapter.messages[z].level.toLowerCase()]++;
				}
			};
		};

		$scope.adapterSummary = adapterSummary;
		$scope.receiverSummary = receiverSummary;
		$scope.messageSummary = messageSummary;
		lastUpdated = updated;
	};
	//$interval(updateAdapterSummary, 2000);

	Hooks.register("adapterUpdated:once", function(adapter) {
		if($location.hash()) {
			angular.element("#"+$location.hash())[0].scrollIntoView();
		}
		$scope.loading = false;
	});
	Hooks.register("adapterUpdated", function(adapter) {
		var name = adapter.name;
		if(name.length > 20)
			name = name.substring(0, 17) + "...";
		if(adapter.started == true) {
			for(x in adapter.receivers) {
				if(adapter.receivers[x].started == false) {
					Notification.add('fa-exclamation-circle', "Receiver '"+name+"' stopped!", false, function() {
						$location.path("status");
						$location.hash(adapter.name);
					});
				}
			}
		}
		else {
			Notification.add('fa-exclamation-circle', "Adapter '"+name+"' stopped!", false, function() {
				$location.path("status");
				$location.hash(adapter.name);
			});
		}
	});

	$scope.resetNotificationCount = function() { Notification.resetCount(); };
	$scope.$watch(function () { return Notification.getCount(); }, function () {
		$scope.notificationCount = Notification.getCount();
		$scope.notificationList = Notification.getLatest(5);
	});

	$scope.$on('IdleStart', function () {
		Poller.getAll().changeInterval(appConstants["console.idle.pollerInterval"]);

		var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
		if(!idleTimeout) return;

		SweetAlert.Warning({
			title: "Idle timer...",
			text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
			showConfirmButton: false,
			showCloseButton: true
		});
	});

	$scope.$on('IdleWarn', function (e, time) {
		var minutes = Math.floor(time/60);
		var seconds = Math.round(time%60);
		if(minutes < 10) minutes = "0" + minutes;
		if(seconds < 10) seconds = "0" + seconds;
		var elm = angular.element(".swal2-container").find(".idleTimer");
		elm.text(minutes + ":" + seconds);
	});

	$scope.$on('IdleTimeout', function () {
		SweetAlert.Info({
			title: "Idle timer...",
			text: "You have been logged out due to inactivity.",
			showCloseButton: true
		});
		$location.path("logout");
	});

	$scope.$on('IdleEnd', function () {
		var elm = angular.element(".swal2-container").find(".swal2-close");
		elm.click();

		Poller.getAll().changeInterval(appConstants["console.pollerInterval"]);
	});

	$scope.openInfoModel = function () {
		$uibModal.open({
			templateUrl: 'views/information.html',
//            size: 'sm',
			controller: 'InformationCtrl',
		});
	};

	$scope.sendFeedback = function (rating) {
		if(!appConstants["console.feedbackURL"])
			return;

		$(".rating i").each(function(i, e) {
			$(e).addClass("fa-star-o").removeClass("fa-star");
		});
		$uibModal.open({
			templateUrl: 'views/feedback.html',
			controller: 'FeedbackCtrl',
			resolve: {rating: function() { return rating; }},
		});
	};
}])

.controller('InformationCtrl', ['$scope', '$uibModalInstance', 'Api', function($scope, $uibModalInstance, Api) {
	Api.Get("server/info", function(data) {
		$.extend( $scope, data );
	});
	$scope.close = function () {
		$uibModalInstance.close();
	};
}])

.controller('errorController', ['$scope', 'Api', 'Debug', '$http', 'Misc', '$state', '$timeout', function($scope, Api, Debug, $http, Misc, $state, $timeout) {
	var timeout = null;
	$scope.retry = function() {
		$scope.retryInit = true;
		angular.element('.retryInitBtn i').addClass('fa-spin');

		$http.get(Misc.getServerPath()+"ConfigurationServlet").then(reload, reload);
	};
	function reload() {
		window.location.reload();
		$timeout.cancel(timeout);
		$timeout(function() {
			angular.element(".main").show();
			angular.element(".loading").hide();
		}, 100);
	}
	timeout = $timeout(function(){$scope.retry();}, 60000);
}])

.controller('FeedbackCtrl', ['$scope', '$uibModalInstance', '$http', 'rating', '$timeout', 'appConstants', 'SweetAlert', function($scope, $uibModalInstance, $http, rating, $timeout, appConstants, SweetAlert) {
	var URL = appConstants["console.feedbackURL"];
	$scope.form = {rating: rating, name: "", feedback: ""};

	$timeout(function() {
		while(rating >= 0) {
			setRate(rating);
			rating--;
		}
	}, 150);

	$scope.setRating = function (ev, i) {
		resetRating();
		$scope.form.rating = i;
		var j = i;
		while(j >= 0) {
			setRate(j);
			j--;
		}
	};
	function setRate(i) {
		$(".rating i.rating"+i).removeClass("fa-star-o");
		$(".rating i.rating"+i).addClass("fa-star");
	};
	function resetRating() {
		$(".rating i").each(function(i, e) {
			$(e).addClass("fa-star-o").removeClass("fa-star");
		});
	};

	$scope.submit = function (form) {
		form.rating++;
		$http.post(URL, form, {headers:{"Authorization": undefined}}).then(function(response) {
			if(response && response.data && response.data.result && response.data.result == "ok")
				SweetAlert.Success("Thank you for sending us feedback!");
			else
				SweetAlert.Error("Oops, something went wrong...", "Please try again later!");
		}, function() {
			SweetAlert.Error("Oops, something went wrong...", "Please try again later!");
		});
		$uibModalInstance.close();
	};

	$scope.close = function () {
		$uibModalInstance.close();
	};
}])

.filter('adapterFilter', function() {
	return function(adapters, $scope) {
		if(!adapters || adapters.length < 1) return [];
		var r = {};
		for(adapterName in adapters) {
			var adapter = adapters[adapterName];

			if((adapter.configuration == $scope.selectedConfiguration || $scope.selectedConfiguration == "All") && $scope.filter[adapter.status])
				r[adapterName] = adapter;
		}
		return r;
	};
})

.controller('StatusCtrl', ['$state','$rootScope','$scope', 'Hooks', 'Api', 'SweetAlert', 'Poller', '$filter', function($state, $rootScope, $scope, Hooks, Api, SweetAlert, Poller, $filter) {
	this.filter = {
		"started": true,
		"stopped": true,
		"warning": true
	};
	$scope.filter = this.filter;
	$scope.applyFilter = function(filter) {
		$scope.filter = filter;
	};
	$scope.reload = false;
	$rootScope.selectedConfiguration = "All";

	$scope.collapseAll = function() {
		$(".adapters").each(function(i,e) {
			var ibox = $(e);
			var icon = ibox.find(".ibox-tools").find('i:first');
			var content = ibox.find('div.ibox-content');
			content.slideUp(200);
			icon.removeClass('fa-chevron-down').addClass('fa-chevron-up');
		});
	};
	$scope.expandAll = function() {
		$(".adapters").each(function(i,e) {
			var ibox = $(e);
			var icon = ibox.find(".ibox-tools").find('i:first');
			var content = ibox.find('div.ibox-content');
			content.slideDown(200);
			icon.addClass('fa-chevron-down').removeClass('fa-chevron-up');
		});
	};
	$scope.stopAll = function() {
		var adapters = Array();
		for(adapter in $filter('adapterFilter')($scope.adapters, $scope)) {
			adapters.push(adapter);
		}
		Api.Put("adapters", {"action": "stop", "adapters": adapters});
	};
	$scope.startAll = function() {
		var adapters = Array();
		for(adapter in $filter('adapterFilter')($scope.adapters, $scope)) {
			adapters.push(adapter);
		}
		Api.Put("adapters", {"action": "start", "adapters": adapters});
	};
	$scope.reloadConfiguration = function() {
		$scope.reload = true;
		if($scope.selectedConfiguration == "All") return;

		Poller.getAll().stop();
		Api.Put("configurations/"+$scope.selectedConfiguration, {"action": "reload"}, function() {
			$scope.reload = false;
			Poller.getAll().start();
		});
	};
	$scope.fullReload = function() {
		$scope.reload = true;
		Poller.getAll().stop();
		Api.Put("configurations", {"action": "reload"}, function() {
			$scope.reload = false;
			Poller.getAll().start();
		});
	};
	$scope.showReferences = function() {
		SweetAlert.Info("Method not yet implemented!");
	};

	$scope.changeConfiguration = function(name) {
		$rootScope.selectedConfiguration = name;
		$scope.updateAdapterSummary(false);
		$state.transitionTo('pages.status', {configuration: name}, { notify: false, reload: false });
	};
	if($state.params.configuration != "All")
		$scope.changeConfiguration($state.params.configuration);


	$scope.startAdapter = function(adapter) {
		adapter.state = 'starting';
		Api.Put("adapters/" + adapter.name, {"action": "start"});
	};
	$scope.stopAdapter = function(adapter) {
		adapter.state = 'stopping';
		Api.Put("adapters/" + adapter.name, {"action": "stop"});
	};
	$scope.startReceiver = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("adapters/" + adapter.name + "/receivers/" + receiver.name, {"action": "start"});
	};
	$scope.stopReceiver = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("adapters/" + adapter.name + "/receivers/" + receiver.name, {"action": "stop"});
	};
}])

.controller('LoadingCtrl', ['$scope', function($scope) {
	$scope.$on('loading', function(event, loading) { $scope.loading = loading; });
}])

.controller('LogoutCtrl', ['$scope', 'Poller', 'authService', 'Idle', function($scope, Poller, authService, Idle) {
	Poller.getAll().remove();
	Idle.unwatch();
	authService.logout();
}])

.controller('LoginCtrl', ['$scope', 'authService', '$timeout', 'appConstants', 'Alert', '$interval', 
	function($scope, authService, $timeout, appConstants, Alert, $interval) {
	/*$interval(function() {
		$scope.notifications = Alert.get(true);
	}, 200);*/
	$timeout(function() {
		$scope.notifications = Alert.get();
		angular.element(".main").show();
		angular.element(".loading").hide();
		angular.element("body").addClass("gray-bg");
	}, 500);
	authService.loggedin(); //Check whether or not the client is logged in.
	$scope.credentials = {};
	$scope.login = function(credentials) {
		authService.login(credentials.username, credentials.password);
	};
}])

.controller('NotificationsCtrl', ['$scope', 'Api', '$stateParams', 'Hooks', 'Notification', 
	function($scope, Api, $stateParams, Hooks, Notification) {
	if($stateParams.id > 0) {
		$scope.notification = Notification.get($stateParams.id);
	}
	else {
		$scope.text = ("Showing a list with all notifications!");
	}

	Hooks.register("adapterUpdated:2", function(adapter) {
		console.warn("What is the scope of: ", adapter);
	});
}])

.controller('TranslateCtrl', ['$scope', '$translate', function($scope, $translate) {
	$scope.changeLanguage = function (langKey) {
		$translate.use(langKey);
		$scope.language = langKey;
	};
}])


//** Ctrls **//

.controller('ManageConfigurationDetailsCtrl', ['$scope', '$state', 'Api', 'Debug', 'Misc', function($scope, $state, Api, Debug, Misc) {
	$scope.state = [];
	$scope.addNote = function(type, message) {
		$scope.state.push({type:type, message: message});
	};

	$scope.configuration = $state.params.name;
	function update() {
		Api.Get("configurations/manage/"+$state.params.name, function(data) {
			$scope.versions = data;
		});
	};
	update();
	$scope.download = function(config) {
		window.open(Misc.getServerPath() + "iaf/api/configurations/download/"+config.name+"?version="+config.version);
	};
	$scope.activate = function(config) {
		$scope.state = [];
		Api.Get("configurations/manage/"+config.name+"/activate/"+config.version, function(data) {
			$scope.addNote("success", "Successfully changed version '"+config.version+"' to active.");
			update();
		}, function() {
			$scope.addNote("danger", "An error occured while changing active configuration");
		});
	};
}])

.controller('UploadConfigurationsCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.jmsRealms = {};

	Api.Get("jdbc", function(data) {
		$scope.form.realm = $scope.jmsRealms[0];
		$.extend($scope, data);
	});

	$scope.form = {
			realm:"",
			name:"",
			version:"",
			encoding:"",
			multiple_configs:false,
			activate_config:true,
			automatic_reload:false,
	};

	$scope.file = null;
	$scope.handleFile = function(files) {
		if(files.length == 0) {
			$scope.file = null;
			return;
		}
		var name = files[0].name.replace(/^.*[\\\/]/, '');
		var i = name.lastIndexOf(".");
		if(i > -1)
			name = name.substring(0, i);

		var nameL = name.split("-"); //Explode the name "Test_Configuration-001-SNAPSHOT_20171122-1414.jar"
		var splitOn = nameL.length -3; //(4) ["Test_Configuration", "001", "SNAPSHOT_20171122", "1414"]
		if((nameL[nameL.length -2]).indexOf("SNAPSHOT")) {
			splitOn +=1;
		}
		$scope.form.name = nameL.splice(0, splitOn).join("-"); //split nameL on index SPLITON and join the values with "-"
		$scope.form.version = nameL.join("-"); //Join the remaining array with "-"
		$scope.file = files[0]; //Can only parse 1 file!
	};

	$scope.submit = function() {
		if($scope.file == null) return;

		var fd = new FormData();
		if($scope.form.realm && $scope.form.realm != "")
			fd.append("realm", $scope.form.realm);
		else 
			fd.append("realm", $scope.jmsRealms[0]);

		fd.append("name", $scope.form.name);
		fd.append("version", $scope.form.version);
		fd.append("encoding", $scope.form.encoding);
		fd.append("multiple_configs", $scope.form.multiple_configs);
		fd.append("activate_config", $scope.form.activate_config);
		fd.append("automatic_reload", $scope.form.automatic_reload);
		fd.append("file", $scope.file, $scope.file.name);

		Api.Post("configurations", fd, { 'Content-Type': undefined }, function(data) {
			$scope.error = "";
			$scope.result = "Successfully uploaded configuration!";
			$scope.form = {
					realm: $scope.jmsRealms[0],
					name:"",
					version:"",
					encoding:"",
					multiple_configs:false,
					activate_config:true,
					automatic_reload:false,
			};
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		});
	};

	$scope.reset = function() {
		$scope.result = "";
		$scope.error = "";
		$scope.form = {
				realm: $scope.jmsRealms[0],
				name:"",
				version:"",
				encoding:"",
				multiple_configs:false,
				activate_config:true,
				automatic_reload:false,
		};
	};
}])

.controller('ShowConfigurationCtrl', ['$scope', 'Api', function($scope, Api) {
	this.configurationRadio = 'true';
	$scope.selectedConfiguration = "All";
	$scope.loadedConfiguration = true;

	$scope.loadedConfig = function(bool) {
		$scope.loadedConfiguration = (bool == "true") ? true : false;
		getConfiguration();
	};

	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;
		getConfiguration();
	};

	getConfiguration = function() {
		var uri = "configurations";
		if($scope.selectedConfiguration != "All") uri += "/" + $scope.selectedConfiguration;
		if($scope.loadedConfiguration) uri += "?loadedConfiguration=true";
		Api.Get(uri, function(data) {
			$scope.configuration = data;
		});
	};
	getConfiguration();
}])

.controller('EnvironmentVariablesCtrl', ['$scope', 'Api', 'appConstants', '$timeout', function($scope, Api, appConstants, $timeout) {
	$scope.state = [];
	$scope.variables = [];
	Api.Get("environmentvariables", function(data) {
		$scope.variables = data;
	});
	Api.Get("server/log", function(data) {
		$scope.form = data;
	});

	$scope.form = {
		loglevel: "DEBUG",
		logIntermediaryResults: true,
		maxMessageLength: -1,
		errorLevels: ["DEBUG", "INFO", "WARN", "ERROR"],
	};

	$scope.changeLoglevel = function(name) {
		$scope.form.loglevel = name;
	};

	$scope.submit = function(formData) {
		$scope.state = [{type:"info", message: "Updating log configuration..."}];
		Api.Put("server/log", formData, function() {
			Api.Get("server/log", function(data) {
				$scope.form = data;
				$scope.state = [{type:"success", message: "Successfully updated log configuration!"}];
			});
		});
		$timeout(function(){
			$scope.state = [];
		}, 5000);
	};
}])

.controller('AdapterStatisticsCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', function($scope, Api, $stateParams, SweetAlert) {
	var adapterName = $stateParams.name;
	if(!adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.adapterName = adapterName;

	$scope.stats = [];
	Api.Get("adapters/"+adapterName+"/statistics", function(data) {
		$scope.stats = data;
	});
}])

.controller('WebservicesCtrl', ['$scope', 'Api', 'Misc', function($scope, Api, Misc) {
	$scope.rootURL = Misc.getServerPath() + 'rest/';
	Api.Get("webservices", function(data) {
		$.extend($scope, data);
	});
}])

.controller('SecurityItemsCtrl', ['$scope', 'Api', '$rootScope', function($scope, Api, $rootScope) {
	$scope.sapSystems = [];
	$scope.serverProps = {};
	$scope.authEntries = [];
	$scope.jmsRealms = [];
	$scope.securityRoles = [];
	$scope.certificates = [];
	for(a in $rootScope.adapters) {
		var adapter = $rootScope.adapters[a];
		if(adapter.pipes) {
			for(p in adapter.pipes) {
				var pipe = adapter.pipes[p];
				if(pipe.certificate)
					$scope.certificates.push({
						adapter: a,
						pipe: p.name,
						certificate: pipe.certificate
					});
			}
		}
	}

	Api.Get("securityitems", function(data) {
		$.extend($scope, data);
	});
}])

.controller('SchedulerCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.jobs = {};
	$scope.scheduler = {};
	update();

	function update() {
		Api.Get("schedules", function(data) {
			$.extend($scope, data);
		});
	};

	$scope.start = function() {
		Api.Put("schedules", {action: "start"}, update);
	};

	$scope.pause = function() {
		Api.Put("schedules", {action: "pause"}, update);
	};

	$scope.remove = function(jobGroup, jobName) {
		Api.Delete("schedules/"+jobGroup+"/"+jobName, update);
	};

	$scope.trigger = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/"+jobName, null, update);
	};
}])

.controller('LoggingCtrl', ['$scope', 'Api', 'Misc', '$timeout', function($scope, Api, Misc, $timeout) {
	$scope.viewFile = false;

	Api.Get("logging", function(data) {
		$.extend($scope, data);
	});

	$scope.view = function (file, as) {
		var resultType = "";
		var params = "";
		switch (as) {
		case "stats":
			resultType = "html";
			params += "&stats=true";
			break;

		case "log4j":
			resultType = "html";
			params += "&log4j=true";

		default:
			resultType = as;
			break;
		}

		var URL = Misc.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + file.path + params;
		if(resultType == "xml") {
			window.open(URL, "_blank");
			return;
		}

		$scope.viewFile = URL;
		$scope.loading = true;
		$timeout(function() {
			var iframe = angular.element("iframe");
			$scope.origDirectory = $scope.directory;
			$scope.directory = file.path;

			iframe[0].onload = function() {
				$scope.loading = false;
				var iframeBody = $(iframe[0].contentWindow.document.body);
				iframeBody.css({"background-color": "rgb(243, 243, 244)"});
				iframe.css({"height": iframeBody.height() + 50});
			};
		}, 50);
	};

	$scope.closeFile = function () {
		$scope.viewFile = false;
		$scope.directory = $scope.origDirectory;
	};

	$scope.download = function (file) {
		var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + file.path;
		window.open(url, "_blank");
	};

	$scope.openDirectory = function (file) {
		Api.Get("logging?directory="+file.path, function(data) {
			$.extend($scope, data);
		});
	};
}])

.controller('IBISstoreSummaryCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.jmsRealms = {};

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
	});

	$scope.submit = function(formData) {
		if(!formData) formData = {};

		if(!formData.realm) formData.realm = $scope.jmsRealms[0] || false;

		Api.Post("jdbc/summary", JSON.stringify(formData), function(data) {
			$scope.error = "";
			$.extend($scope, data);
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		});
	};

	$scope.reset = function() {
		$scope.result = "";
		$scope.error = "";
	};
}])

.controller('SendJmsMessageCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"]; 
	Api.Get("jms", function(data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	$scope.submit = function(formData) {
		if(!formData) return;

		var fd = new FormData();
		if(formData.realm && formData.realm != "")
			fd.append("realm", formData.realm);
		else 
			fd.append("realm", $scope.jmsRealms[0]);
		if(formData.destination && formData.destination != "")
			fd.append("destination", formData.destination);
		if(formData.type && formData.type != "")
			fd.append("type", formData.type);
		else 
			fd.append("type", $scope.destinationTypes[0]);
		if(formData.replyTo && formData.replyTo != "")
			fd.append("replyTo", formData.replyTo);
		if(formData.persistent && formData.persistent != "")
			fd.append("persistent", formData.persistent);

		if(!formData.message && !formData.file) {
			$scope.error = "Please specify a file or message!";
			return;
		}

		if(formData.message && formData.message != "")
			fd.append("message", formData.message);
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);

		Api.Post("jms/message", fd, { 'Content-Type': undefined }, function(returnData) {
			//?
		}, function(errorData, status, errorMsg) {
			$scope.error = (errorData.error) ? errorData.error : errorMsg;
		});
	};

	$scope.reset = function() {
		$scope.error = "";
		if(!$scope.form) return;
		if($scope.form.destination)
			$scope.form.destination = "";
		if($scope.form.replyTo)
			$scope.form.replyTo = "";
		if($scope.form.message)
			$scope.form.message = "";
		if($scope.form.persistent)
			$scope.form.persistent = "";
		if($scope.form.type)
			$scope.form.type = $scope.destinationTypes[0];
	};
}])

.controller('BrowseJmsQueueCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"]; 
	Api.Get("jms", function(data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	$scope.submit = function(formData) {
		if(!formData || !formData.destination) {
			$scope.error = "Please specify a jms realm and destination!";
			return;
		}
		if(!formData.realm) formData.realm = $scope.jmsRealms[0] || false;
		if(!formData.type) formData.type = $scope.destinationTypes[0] || false;

		Api.Post("jms/browse", JSON.stringify(formData), function(returnData, status) {
			$scope.error = "";
			console.log("status", status);
		}, function(errorData, status, errorMsg) {
			$scope.error = (errorData.error) ? errorData.error : errorMsg;
		});
	};

	$scope.reset = function() {
		$scope.error = "";
		if(!$scope.form) return;
		if($scope.form.destination)
			$scope.form.destination = "";
		if($scope.form.rowNumbersOnly)
			$scope.form.rowNumbersOnly = "";
		if($scope.form.type)
			$scope.form.type = $scope.destinationTypes[0];
	};
}])

.controller('ExecuteJdbcQueryCtrl', ['$scope', 'Api', '$timeout', '$state', function($scope, Api, $timeout, $state) {
	$scope.jmsRealms = {};
	$scope.resultTypes = {};
	$scope.error = "";

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
	});

	$scope.submit = function(formData) {
		if(!formData || !formData.query) {
			$scope.error = "Please specify a jms realm, resulttype and query!";
			return;
		}
		if(!formData.realm) formData.realm = $scope.jmsRealms[0] || false;
		if(!formData.resultType) formData.resultType = $scope.resultTypes[0] || false;

		Api.Post("jdbc/query", JSON.stringify(formData), function(returnData) {
			$scope.error = "";
			$scope.result = returnData;
		}, function(errorData, status, errorMsg) {
			var error = (errorData.error) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		});
	};

	$scope.reset = function() {
		$scope.form.query = "";
		$scope.result = "";
	};
}])

.controller('BrowseJdbcTablesCtrl', ['$scope', 'Api', '$timeout', '$state', function($scope, Api, $timeout, $state) {
	$scope.jmsRealms = {};
	$scope.resultTypes = {};
	$scope.error = "";
	var orderedGroups = ['rare', 'uncommon', 'common'];
	$scope.groupComparator = function(item) {
		return orderedGroups.indexOf(item.group);
	};
	Api.Get("jdbc", function(data) {
		$scope.jmsRealms = data.jmsRealms;
	});
	$scope.submit = function(formData) {
		if(!formData || !formData.table) {
			$scope.error = "Please specify a jms realm and table name!";
			return;
		}
		if(!formData.realm) formData.realm = $scope.jmsRealms[0] || false;
		if(!formData.resultType) formData.resultType = $scope.resultTypes[0] || false;

		Api.Post("jdbc/browse", JSON.stringify(formData), function(returnData) {
			$scope.error = "";
			$scope.query = returnData.query;
			$timeout(function(){
				var thead = angular.element("table.jdbcBrowse thead");
				var tbody = angular.element("table.jdbcBrowse tbody");
				thead = thead.empty().append("<tr></tr>").find("tr");
				tbody.empty();
				var index = [];
				thead.append("<th>RNUM</th>");
				index.push("RNUM");
				for(x in returnData.fielddefinition) {
					index.push(x);
					if(formData.minRow == undefined && formData.maxRow == undefined)
						x = x + " " + returnData.fielddefinition[x];
					thead.append("<th>"+x+"</th>");
				}
				for(x in returnData.result) {
					var tableRow = $("<tr></tr>");
					var row = returnData.result[x];
					for(def in row) {
						var p = "";
						if(returnData.result.length == 1 && def.indexOf("LENGTH ") == 0) {
							def.replace("LENGTH ", "");
							p = " (length)";
						}
						tableRow.append("<td>"+row[def] + p+"</td>");
					}
					tbody.append(tableRow);
				};
			}, 100);
		}, function(errorData, status, errorMsg) {
			var error = (errorData.error) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.query = "";
		});
	};
	$scope.reset = function() {
		$scope.result = "";
	};
}])

.controller('ShowMonitorsCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.monitors = [];
	$scope.enabled = false;
	$scope.destinations = [];
	Api.Get("monitors", function(data) {
		$scope.enabled = data.enabled;
		$scope.monitors = data.monitors;
		$scope.destinations = data.destinations;
	});
}])

.controller('TestPipelineCtrl', ['$scope', 'Api', 'Alert', '$interval', function($scope, Api, Alert, $interval) {
	$scope.state = [];
	$scope.file = null;
	$scope.addNote = function(type, message, removeQueue) {
		$scope.state.push({type:type, message: message});
	};
	$scope.handleFile = function(files) {
		if(files.length == 0) {
			$scope.file = null;
			return;
		}
		$scope.file = files[0]; //Can only parse 1 file!
	};
	$scope.submit = function(formData) {
		$scope.result = "";
		$scope.state = [];
		if(!formData) return;

		var fd = new FormData();
		if(formData.adapter && formData.adapter != "")
			fd.append("adapter", formData.adapter);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if(formData.message && formData.message != "")
			fd.append("message", formData.message);
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);
		
		if(!formData) {
			$scope.addNote("warning", "Please specify an adapter and message!");
			return;
		}
		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify an adapter!");
			return;
		}
		if(!formData.message && !formData.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}
		Api.Post("test-pipeline", fd, { 'Content-Type': undefined }, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
		}, function(returnData) {
			$scope.addNote("danger", returnData.state);
			$scope.result = (returnData.result);
		});
	};
}])

.controller('TestServiceListenerCtrl', ['$scope', 'Api', 'Alert', '$interval', function($scope, Api, Alert, $interval) {
	$scope.state = [];
	$scope.file = null;
	$scope.addNote = function(type, message, removeQueue) {
		$scope.state.push({type:type, message: message});
	};
	$scope.handleFile = function(files) {
		if(files.length == 0) {
			$scope.file = null;
			return;
		}
		$scope.file = files[0]; //Can only parse 1 file!
	};
	$scope.submit = function(formData) {
		$scope.result = "";
		$scope.state = [];
		if(!formData) return;

		var fd = new FormData();
		if(formData.service && formData.service != "")
			fd.append("service", formData.service);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if(formData.message && formData.message != "")
			fd.append("message", formData.message);
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);
		
		if(!formData) {
			$scope.addNote("warning", "Please specify a service and message!");
			return;
		}
		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify a service!");
			return;
		}
		if(!formData.message && !formData.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}
		Api.Post("test-servicelistener", fd, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
		}, function(returnData) {
			$scope.addNote("danger", returnData.state);
			$scope.result = (returnData.result);
		});
	};
}]);