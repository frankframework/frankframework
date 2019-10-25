/**
 * MainCtrl - controller
 * Used on all pages except login/logout
 *
 */
angular.module('iaf.beheerconsole')
.controller('MainCtrl', ['$scope', '$rootScope', 'appConstants', 'Api', 'Hooks', '$state', '$location', 'Poller', 'Notification', 'dateFilter', '$interval', 'Idle', '$http', 'Misc', '$uibModal', 'Session', 'Debug', 'SweetAlert', '$timeout', 'gTag',
	function($scope, $rootScope, appConstants, Api, Hooks, $state, $location, Poller, Notification, dateFilter, $interval, Idle, $http, Misc, $uibModal, Session, Debug, SweetAlert, $timeout, gTag) {
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
					$rootScope.otapStage = appConstants["otap.stage"];
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

	$scope.addAlert = function(type, configuration, message) {
		$scope.alerts.push({
			type: type,
			configuration: configuration,
			message: message
		});
	};
	$scope.addWarning = function(configuration, message) {
		$scope.addAlert("warning", configuration, message);
	};
	$scope.addException = function(configuration, message) {
		$scope.addAlert("danger", configuration, message);
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
		console.log("Checking IAF version with remote...");
		$http.get("https://ibissource.org/iaf/releases/").then(function(response) {
			if(!response  || !response.data) return false;
			var release = response.data[0]; //Not sure what ID to pick, smallest or latest?

			var newVersion = (release.tag_name.substr(0, 1) == "v") ? release.tag_name.substr(1) : release.tag_name;
			var currentVersion = appConstants["application.version"];
			var version = Misc.compare_version(newVersion, currentVersion);
			console.log("Comparing version: '"+currentVersion+"' with latest release: '"+newVersion+"'.");
			Session.remove("IAF-Release");

			if(version > 0) {
				Session.set("IAF-Release", release);
				Notification.add('fa-exclamation-circle', "IAF update available!", false, function() {
					$location.path("iaf-update");
				});
			}
		});
		gTag.event('application.version', appConstants["application.version"]);

		Api.Get("server/warnings", function(configurations) {
			configurations['All'] = {messages:configurations.messages};
			delete configurations.messages;

			configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
			delete configurations.totalErrorStoreCount;

			for(i in configurations) {
				var configuration = configurations[i];
				if(configuration.exception)
					$scope.addException(i, configuration.exception);
				if(configuration.warnings) {
					for(x in configuration.warnings) {
						$scope.addWarning(i, configuration.warnings[x]);
					}
				}

				configuration.messageLevel = "INFO";
				for(x in configuration.messages) {
					var level = configuration.messages[x].level;
					if(level == "WARN" && configuration.messageLevel != "ERROR")
						configuration.messageLevel = "WARN";
					if(level == "ERROR")
						configuration.messageLevel = "ERROR";
				}
			}

 			$scope.messageLog = configurations;
		});

		var raw_adapter_data = {};
		Poller.add("adapters?expanded=all", function(allAdapters) {
			for(adapterName in allAdapters) {
				var adapter = allAdapters[adapterName];

				if(raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
					raw_adapter_data[adapter.name] = JSON.stringify(adapter);

					adapter.status = "started";

					for(x in adapter.receivers) {
						var adapterReceiver = adapter.receivers[x];
						if(adapterReceiver.started == false)
							adapter.status = 'warning';

						if(adapterReceiver.hasErrorStorage && adapterReceiver.errorStorageCount > 0)
							adapter.status = 'stopped';
					}
					adapter.hasSender = false;
					for(x in adapter.pipes) {
						if(adapter.pipes[x].sender) {
							adapter.hasSender = true;
						}
					}
/*					//If last message is WARN or ERROR change adapter status to warning.
					if(adapter.messages.length > 0 && adapter.status != 'stopped') {
						var message = adapter.messages[adapter.messages.length -1];
						if(message.level != "INFO")
							adapter.status = 'warning';
					}
*/
					if(!adapter.started)
						adapter.status = "stopped";

					//Add flow diagrams
					adapter.flow = Misc.getServerPath() + 'rest/showFlowDiagram/' + adapter.name;

					$rootScope.adapters[adapter.name] = adapter;

					$scope.updateAdapterSummary();
					Hooks.call("adapterUpdated", adapter);
				}
			}
		}, true);
	});

	var lastUpdated = 0;
	var timeout = null;
	$scope.updateAdapterSummary = function(configurationName) {
		var updated = (new Date().getTime());
		if(updated - 3000 < lastUpdated && !configurationName) { //3 seconds
			clearTimeout(timeout);
			timeout = setTimeout($scope.updateAdapterSummary, 1000);
			return;
		}
		if(configurationName == undefined)
			configurationName = $state.params.configuration;

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
		for(adapterName in allAdapters) {
			var adapter = allAdapters[adapterName];

			if(adapter.configuration == configurationName || configurationName == 'All') { // Only adapters for active config
				adapterSummary[adapter.state]++;
				for(i in adapter.receivers) {
					receiverSummary[adapter.receivers[i].state.toLowerCase()]++;
				}
				for(i in adapter.messages) {
					var level = adapter.messages[i].level.toLowerCase();
					messageSummary[level]++;
				}
			}
		}

		$scope.adapterSummary = adapterSummary;
		$scope.receiverSummary = receiverSummary;
		$scope.messageSummary = messageSummary;
		lastUpdated = updated;
	};

	Hooks.register("adapterUpdated:once", function() {
		if($location.hash()) {
			angular.element("#"+$location.hash())[0].scrollIntoView();
		}
		$scope.$broadcast('loading', false);
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
	
	$scope.openOldGui = function() {
		location.href = Misc.getServerPath();
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

.filter('configurationFilter', function() {
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

.filter('searchFilter', function() {
	return function(adapters, $scope) {
		if(!adapters || adapters.length < 1) return [];

		if(!$scope.searchText || $scope.searchText.length == 0) return adapters;
		var searchText = $scope.searchText.toLowerCase();

		var r = {};
		for(adapterName in adapters) {
			var adapter = adapters[adapterName];

			if(JSON.stringify(adapter).replace(/"/g, '').toLowerCase().indexOf(searchText) > -1)
				r[adapterName] = adapter;
		}
		return r;
	};
})

.controller('StatusCtrl', ['$scope', 'Hooks', 'Api', 'SweetAlert', 'Poller', '$filter', '$state', 'Misc',
		function($scope, Hooks, Api, SweetAlert, Poller, $filter, $state, Misc) {

	this.filter = {
		"started": true,
		"stopped": true,
		"warning": true
	};
	$scope.filter = this.filter;
	$scope.applyFilter = function(filter) {
		$scope.filter = filter;
		$scope.updateQueryParams();
	};
	if($state.params.filter != "") {
		var filter = $state.params.filter.split("+");
		for(f in $scope.filter) {
			$scope.filter[f] = (filter.indexOf(f) > -1);
		}
	}
	$scope.searchText = "";
	if($state.params.search != "") {
		$scope.searchText = $state.params.search;
	}

	$scope.reload = false;
	$scope.selectedConfiguration = "All";

	$scope.updateQueryParams = function() {
		var filterStr = [];
		for(f in $scope.filter) {
			if($scope.filter[f])
				filterStr.push(f);
		}
		var transitionObj = {};
		transitionObj.filter = filterStr.join("+");
		if($scope.selectedConfiguration != "All")
			transitionObj.configuration = $scope.selectedConfiguration;
		if($scope.searchText.length > 0)
			transitionObj.search = $scope.searchText;

		$state.transitionTo('pages.status', transitionObj, { notify: false, reload: false });
	};

	$scope.collapseAll = function() {
		$(".adapters").each(function(i,e) {
			var a = $(e).find("div.ibox-title");
			angular.element(a).scope().showContent = false;
		});
	};
	$scope.expandAll = function() {
		$(".adapters").each(function(i,e) {
			setTimeout(function() {
				var a = $(e).find("div.ibox-title");
				angular.element(a).scope().showContent = true;
			}, i * 10);
		});
	};
	$scope.stopAll = function() {
		var adapters = Array();
		for(adapter in $filter('configurationFilter')($scope.adapters, $scope)) {
			adapters.push(adapter);
		}
		Api.Put("adapters", {"action": "stop", "adapters": adapters});
	};
	$scope.startAll = function() {
		var adapters = Array();
		for(adapter in $filter('configurationFilter')($scope.adapters, $scope)) {
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
		var config = $scope.selectedConfiguration;
		if(config == "All")
			config = "*All*";

		var url = Misc.getServerPath() + 'rest/showFlowDiagram?configuration=' + config;
		window.open(url);
	};

	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;
		$scope.updateAdapterSummary(name);
		$scope.updateQueryParams();
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

.controller('InfoBarCtrl', ['$scope', function($scope) {
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

.filter('variablesFilter', [function() {
	return function(variables, filterText) {
		var returnArray = new Array();

		filterText = filterText.toLowerCase();
		for(i in variables) {
			var variable = variables[i];
			if(JSON.stringify(variable).toLowerCase().indexOf(filterText) > -1) {
				returnArray.push(variable);
			}
		}

		return returnArray;
	};
}])

.controller('EnvironmentVariablesCtrl', ['$scope', 'Api', 'appConstants', '$timeout', function($scope, Api, appConstants, $timeout) {
	$scope.state = [];
	$scope.variables = {};
	$scope.searchFilter = "";

	Api.Get("environmentvariables", function(data) {
		for(propertyListType in data) {
			var propertyList = data[propertyListType];
			var tmp = new Array();

			for(variableName in propertyList) {
				tmp.push({
					key: variableName,
					val: propertyList[variableName]
				});
			}

			$scope.variables[propertyListType] = tmp;
		}
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

.controller('AdapterErrorStorageCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', function($scope, Api, $stateParams, SweetAlert) {
	$scope.notes = [];
	$scope.addNote = function(type, message, removeQueue) {
		$scope.notes.push({type:type, message: message});
	};
	$scope.closeNote = function(index) {
		$scope.notes.splice(index, 1);
	};

	$scope.adapterName = $stateParams.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.receiverName = $stateParams.receiver;
	if(!$scope.receiverName)
		return SweetAlert.Warning("Receiver not found!");
	$scope.count = $stateParams.count || 0;

	//TODO
	$scope.messages = [];
	var base_url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/errorstorage";
	function getErrorStoreMessages() {
		Api.Get(base_url, function(data) {
			$.extend($scope, data);
		});
	}
	getErrorStoreMessages();

	$scope.deleteMessage = function(message) {
		message.deleting = true;

		Api.Delete(base_url+"/"+message.id, function() {
			for(x in $scope.messages) {
				if($scope.messages[x].id == message.id) {
					$scope.messages.splice(x, 1);
				}
			}
		}, function() {
			message.deleting = false;
			$scope.addNote("danger", "Unable to delete messages with ID: "+message.id);
		});
	};

	$scope.resendMessage = function(message) {
		message.resending = true;

		Api.Put(base_url+"/"+message.id, false, function() {
			for(x in $scope.messages) {
				if($scope.messages[x].id == message.id) {
					$scope.messages.splice(x, 1);
				}
			}
		}, function() {
			message.resending = false;
			$scope.addNote("danger", "Unable to resend messages with ID: "+message.id);
		});
	};
}])

.controller('AdapterMessageLogCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', function($scope, Api, $stateParams, SweetAlert) {
	$scope.adapterName = $stateParams.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.receiverName = $stateParams.receiver;
	if(!$scope.receiverName)
		return SweetAlert.Warning("Receiver not found!");
	$scope.totalMessages = $stateParams.count || 0;

	//TODO
	$scope.messages = [];
	var url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/messagelog";
	Api.Get(url, function(data) {
		$.extend($scope, data);
	});
}])

.controller('PipeMessageLogCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', function($scope, Api, $stateParams, SweetAlert) {
	$scope.pipeName = $stateParams.pipe;
	if(!$scope.pipeName)
		return SweetAlert.Warning("Pipe not found!");
	$scope.adapterName = $stateParams.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.totalMessages = $stateParams.count || 0;

	//TODO
	$scope.messages = [];
	var url = "adapters/"+$scope.adapterName+"/pipes/"+$scope.pipeName+"/messagelog";
	Api.Get(url, function(data) {
		$.extend($scope, data);
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

.controller('LoggingCtrl', ['$scope', 'Api', 'Misc', '$timeout', '$state','$stateParams', function($scope, Api, Misc, $timeout, $state, $stateParams) {
	$scope.viewFile = false;

	var getFileType = function (fileName){
		if(fileName.indexOf('-stats_') >= 0)
			return 'stats';
		else if(fileName.indexOf('_xml.log') >= 0)
			return 'log4j';
		else if(fileName.indexOf('-stats_') >= 0 || fileName.indexOf('_xml.log') >= 0)
			return 'xml';
		else if(fileName.indexOf('-stats_') < 0 && fileName.indexOf('_xml.log') < 0)
			return 'html';
	};

	var openFile = function (file) {
		var resultType = "";
		var params = "";
		var as = getFileType(file.name);
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

			iframe[0].onload = function() {
				$scope.loading = false;
				var iframeBody = $(iframe[0].contentWindow.document.body);
				iframeBody.css({"background-color": "rgb(243, 243, 244)"});
				iframe.css({"height": iframeBody.height() + 50});
			};
		}, 50);
		$state.transitionTo('pages.logging', {directory: $scope.directory, file: file.name}, { notify: false, reload: false });
	};

	$scope.closeFile = function () {
		$scope.viewFile = false;
		$state.transitionTo('pages.logging', {directory: $scope.directory});
	};

	$scope.download = function (file) {
		var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + file.path;
		window.open(url, "_blank");
	};

	var openDirectory = function (directory) {
		var url = "logging";
		if(directory) {
			url = "logging?directory="+directory;
		}

		Api.Get(url, function(data) {
			$.extend($scope, data);
			$state.transitionTo('pages.logging', {directory: data.directory}, { notify: false, reload: false });
		});
	};

	$scope.open = function(file) {
		if(file.type == "directory")
			openDirectory(file.path);
		else
			openFile(file);
	};

	//This is only false when the user opens the logging page
	var directory = ($stateParams.directory && $stateParams.directory.length > 0) ? $stateParams.directory : false;
	//The file param is only set when the user copy pastes an url in their browser
	if($stateParams.file && $stateParams.file.length > 0) {
		var file = $stateParams.file;

		$scope.directory = directory;
		openFile({path: directory+"/"+file, name: file});
	}
	else {
		openDirectory(directory);
	}
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

		$scope.columnNames = [{
			id: 0,
			name: "RNUM",
			desc: "Row Number"
		}];
		var columnNameArray = ["RNUM"];
		$scope.result = [];

		Api.Post("jdbc/browse", JSON.stringify(formData), function(returnData) {
			$scope.error = "";
			$scope.query = returnData.query;

			var i = 0;
			for(x in returnData.fielddefinition) {
				$scope.columnNames.push({
					id: i++,
					name: x,
					desc: returnData.fielddefinition[x]
				});
				columnNameArray.push(x);
			}

			for(x in returnData.result) {
				var row = returnData.result[x];
				var orderedRow = [];
				for(columnName in row) {
					var index = columnNameArray.indexOf(columnName);
					var value = row[columnName];

					if(index == -1 && columnName.indexOf("LENGTH ") > -1) {
						value += " (length)";
						index = columnNameArray.indexOf(columnName.replace("LENGTH ", ""));
					}
					orderedRow[index] = value;
				}
				$scope.result.push(orderedRow);
			}
		}, function(errorData, status, errorMsg) {
			var error = (errorData.error) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.query = "";
		});
	};
	$scope.reset = function() {
		$scope.query = "";
		$scope.error = "";
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

.controller('TestPipelineCtrl', ['$scope', 'Api', 'Alert', function($scope, Api, Alert) {
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
	$scope.processingMessage = false;

	$scope.submit = function(formData) {
		$scope.result = "";
		$scope.state = [];
		if(!formData) {
			$scope.addNote("warning", "Please specify an adapter and message!");
			return;
		}

		var fd = new FormData();
		if(formData.adapter && formData.adapter != "")
			fd.append("adapter", formData.adapter);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if(formData.message && formData.message != "")
			fd.append("message", formData.message);
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify an adapter!");
			return;
		}
		if(!formData.message && !formData.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}

		$scope.processingMessage = true;
		Api.Post("test-pipeline", fd, { 'Content-Type': undefined }, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		}, function(returnData) {
			$scope.addNote("danger", returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		});
	};
}])

.controller('TestServiceListenerCtrl', ['$scope', 'Api', 'Alert', function($scope, Api, Alert) {
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
	$scope.processingMessage = false;

	$scope.submit = function(formData) {
		$scope.result = "";
		$scope.state = [];
		if(!formData) {
			$scope.addNote("warning", "Please specify a service and message!");
			return;
		}

		var fd = new FormData();
		if(formData.service && formData.service != "")
			fd.append("service", formData.service);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if(formData.message && formData.message != "")
			fd.append("message", formData.message);
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify a service!");
			return;
		}
		if(!formData.message && !formData.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}

		$scope.processingMessage = true;
		Api.Post("test-servicelistener", fd, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		}, function(returnData) {
			$scope.addNote("danger", returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		});
	};
}])

.controller('LarvaCtrl', ['$scope', '$compile', 'Api', 'Alert', 'Poller', function($scope, $compile, Api, Alert, Poller) {
	$scope.lastMessageArrived = 0;
	$scope.messages = {};
	$scope.tests = [];
	$scope.displayMessages = [];
	$scope.lastAction = null;
	$scope.lowestLogLevel = null;
	$scope.archivedMessages = {};

	/**
	 * Adds a new message that acts as an alert.
	 * @param level representing the class for panel. Possible options are "panel-danger, panel-info"
	 * @param message Test containing the message
	 */
	$scope.addDisplayMessage = function(level, message) {
		$scope.displayMessages.push({
			"message": message,			"level": level
		})
	};

	/**
	 * Removes the message displayed under the form-above the table.
	 * @param message to be removed.
	 */
	$scope.removeDisplayMessage = function(message) {
		var i = 0;
		while(i<$scope.displayMessages.length) {
			if($scope.displayMessages[i]["message"] === message) {
				$scope.displayMessages.splice(i, 1);
				i--;
			}
			i++;
		}
	};

	$scope.assignDisplayMessageClass = function(message) {
		return message.level;
	};

	/**
	 * Creates the options and defaults based on the data on the server.
	 */
	$scope.setForm = function() {
		// Check for the various File API support.
		if (window.File && window.FileReader && window.FileList && window.Blob) {
			$scope.showFileSelector = true;
			// Great success! All the File APIs are supported.
		} else {
			$scope.showFileSelector = false;
			console.log("HTML 5 File API is not supported by this browser.");
		}

		Api.Get("larva/params", function (data) {
			$scope.logLevels = data["logLevels"][0];
			var scenarios = [];
			var a;
			console.log(data);
			console.log("Default Root Directory: " + data["defaultRootDirectory"]);

			for(key in data["scenarios"][0]) {
				scenarios.push({"name": data["scenarios"][0][key], "value": key});
			}
			var rootDirectories = [];
			for(key in data["rootDirectories"][0]) {
				rootDirectories.push({"name": key, "value": data["rootDirectories"][0][key]});
			}

			scenarios.sort(function (a1, b) {
				return a1[Object.keys(a1)[0]].localeCompare(b[Object.keys(b)[0]]);
			});

			$scope.formLogLevel = data["selectedLogLevel"][0];
			$scope.formRootDirectories = data["defaultRootDirectory"][0];
			$scope.formScenarios = scenarios[0]["value"];
			$scope.scenarios = scenarios;
			$scope.rootDirectories = rootDirectories;
		});
	};

	/**
	 * Groups the messages wrt their test names. Then sets the $scope.messages
	 * @param data that contains the messages.
	 */
	$scope.setMessages = function(data) {
		console.log("Inside poll callback.");
		if(data.length === 0)
			return;

		data.sort(function (a, b) {
			return a["timestamp"] - b["timestamp"];
		});
		console.log(data);
		$scope.lastMessageArrived = data[data.length - 1]["timestamp"];

		data.forEach(function (element) {
			if(element["logLevel"] === "Test Properties") {
				console.log(element);

				var testIndex = $scope.tests.map(function (value) { return value["name"] }).indexOf(element["name"]);
				if (testIndex === -1){
					var test = {"name": element["name"], "status": element["messages"]["status"]? element["messages"]["status"] : "RUNNING", "directory":element["messages"]["directory"]? element["messages"]["directory"] : "directory"};
					$scope.tests.push(test);
				}else {
					$scope.tests[testIndex]["status"] = element["messages"]["status"] ? element["messages"]["status"] : $scope.tests[testIndex]["status"];
					$scope.tests[testIndex]["directory"] = element["messages"]["directory"] ? element["messages"]["directory"] : $scope.tests[testIndex]["directory"];
				}
			}else if(element["logLevel"] === "Total" && !$scope.file && Poller.get("LarvaMessagePoller")) {
				Poller.remove("LarvaMessagePoller");
				console.log("Stop Poller.");
				$scope.addDisplayMessage(
					(element["messages"]["Message"].includes("failed")) ? "panel-danger" : "panel-info",
					element["messages"]["Message"]);
			}

			if(element["logLevel"] === "Errors") {
				$scope.addDisplayMessage("panel-danger", element["messages"]["Message"]);
			}

			if(!$scope.messages[element["name"]]){
				$scope.messages[element["name"]] = [];
			}
			$scope.messages[element["name"]].push(element);
		});

		console.log("Finishing poll callback");
	};

	/**
	 * Starts polling for new messages using the Poller service.
	 */
	$scope.startPolling = function () {
		console.log("Inside start poller");
		var generator = {
			/**
			 * @return {string}
			 */
			"LarvaMessagePoller": function() {
			console.log("Inside URI Generator.");
			var toreturn = "larva/messages/" + ($scope.lastMessageArrived + 1);
			console.log(toreturn);
			return toreturn;
		}};

		Poller.add(generator, $scope.setMessages, true, 500);
	};

	/**
	 * Assigns a class to the test object in the table.
	 * @param test Test object containing the status.
	 * @returns {string} representing the class for bootstrap.
	 */
	$scope.assignClass = function(test) {
		switch (test.status) {
			case "RUNNING":
				return "";
			case "OK":
				return "success";
			case "AUTOSAVED":
				return "info";
			case "ERROR":
				return "danger";
		}
		return "danger";
	};

	/**
	 * Submits the form data to the server to start running the selected tests.
	 */
	$scope.submit = function () {
		$scope.file = undefined;
		console.log("Inside submit");
		var fd = new FormData();

		if($scope.formTimeout !== "")
			fd.append("timeout", $scope.formTimeout);

		if($scope.formLogLevel !== "")
			fd.append("logLevel", $scope.formLogLevel);

		if($scope.formWaitcleanup !== "")
			fd.append("waitBeforeCleanup", $scope.formWaitcleanup);

		if($scope.formThreads !== "")
			fd.append("numberOfThreads", $scope.formThreads);

		if($scope.formScenarios === ""){
			$scope.addDisplayMessage("panel-warning", "Make sure scenarios are selected.");
			return;
		}
		fd.append("scenario", $scope.formScenarios);
		if($scope.formRootDirectories === ""){
			$scope.addDisplayMessage("panel-warning", "Make sure root directory is selected.");
			return;
		}
		fd.append("rootDirectory", $scope.formRootDirectories);

		var success = function() {
			$scope.lastMessageArrived = 0;
			$scope.messages = {};
			$scope.tests = [];
			$scope.archivedMessages = {};
			console.log("Started testing with success.");
			$scope.lastAction = "Run";
			$scope.lowestLogLevel = $scope.formLogLevel;
			$scope.startPolling();
		};
		var error = function(errorData, status, errorMsg) {
			console.log("Error Data:\n" + errorData);
			$scope.lastAction = null;
			$scope.addDisplayMessage("panel-danger",  status + ": Could not execute tests. " + errorMsg);
		};
		console.log("Calling POST");
		Api.Post("larva/execute", fd,  { 'Content-Type': undefined }, success, error);
	};

	/**
	 * Uploads the file when a user selects a json file containing messages.
	 * @param event representing the user selecting a json file.
	 */
	$scope.fileChanged = function(event) {
		console.log("Inside file changed");
		$scope.file = event.target.files[0];
		$scope.loadFile();
	};

	/**
	 * Uploads the json file that is in the $scope.file variable.
	 */
	$scope.loadFile = function() {
		console.log("Inside load file");
		if(!$scope.file)
			$scope.addDisplayMessage("danger", "You need to select a json file!");

		var fileReader = new FileReader();
		fileReader.onload = function(e) {
			$scope.lastAction = "Upload";
			console.log(fileReader.result);
			var fileData= JSON.parse(fileReader.result);
			console.log(fileData);
			var data = [];
			for(test in fileData) {
				data = data.concat(fileData[test]);
			}
			console.log(data);
			$scope.setMessages(data);
			$scope.archivedMessages = $scope.messages;
			$scope.setLowestLogLevel(data);
			$scope.formLogLevel = $scope.lowestLogLevel;
			console.log($scope.lowestLogLevel + " lowest log level");
		}
		fileReader.readAsText(this.file);
	};

	/**
	 * Sets the $scope.lowestLogLevel the lowest log level contained in the data.
	 * @param data array of messages.
	 */
	$scope.setLowestLogLevel = function(data) {
		var length = $scope.logLevels.length;
		$scope.lowestLogLevel = length;
		data.forEach(function (element) {
			if($scope.logLevels.indexOf(element["logLevel"]) < $scope.lowestLogLevel) {
				$scope.lowestLogLevel = $scope.logLevels.indexOf(element["logLevel"]);
			}
		});
		if($scope.lowestLogLevel === length) {
			console.error("Could not get the lowest log level.");
			$scope.lowestLogLevel = -1;
		}
		$scope.lowestLogLevel = $scope.logLevels[$scope.lowestLogLevel];
	}

	/**
	 * Created a diff by line based on given text.
	 * @param text1 First string to be compared to.
	 * @param text2 Second string to be compared.
	 * @returns {Diff object based on Google's Diff-Match-Patch} the diff object.
	 */
	$scope.diff_lineMode = function (text1, text2) {
		var dmp = new diff_match_patch();
		var a = dmp.diff_linesToChars_(text1, text2);
		var lineText1 = a.chars1;
		var lineText2 = a.chars2;
		var lineArray = a.lineArray;
		var diffs = dmp.diff_main(lineText1, lineText2, false);
		dmp.diff_charsToLines_(diffs, lineArray);
		return diffs;
	}

	/**
	 * Translates the given diff to text containing spans for highlights.
	 * @param diff The input diff object based on Google's Diff-Match-Patch package.
	 * @returns {string} The output string containing the text and highlights.
	 */
	$scope.diff2text = function(diff) {
		var current = 0;
		var text = "";
		diff.forEach(function (word) {
			if (word[0] === current) {
				text += word[1];
				return;
			}
			if(current !== 0)
				text += '</span>';
			current = word[0];
			var textClass = "";
			switch (word[0]) {
				case 1:
					textClass = "text-info";
					break;
				case -1:
					textClass = "text-danger";
					break;
			}
			if(word[0] !== 0)
				text += '<span class="' + textClass + '">';
			text += word[1];
		});
		return text;
	}

	/**
	 * Returns the properties required for displaying the message in the modal.
	 * The returned header is the text to be displayed inside header.
	 * The returned body is the text to be displayed inside panel body.
	 * The returned class is to be used for bootstrap.
	 * @param message Message to be displayed.
	 * @returns {{header: string, body: string, class: string}}
	 */
	$scope.messageDisplayProperties = function(message) {
		var panelClass = "panel-default";
		var bodyText = "";
		var headerText = "";
		switch (message["logLevel"]) {
			case "Debug":
				bodyText = message["messages"]["Message"];
				break;
			case "Step Passed/Failed":
			case "Scenario Passed/Failed":
				headerText = message["messages"]["Message"];
				panelClass = (message["messages"]["Message"].includes("pass")) ? "panel-primary" : "panel-danger";
				break;
			case "Pipeline Messages Prepared For Diff":
			case "Pipeline Messages":
				headerText = (message["messages"]["Step Display Name"]) ? message["messages"]["Step Display Name"] : message["messages"]["Message"];
				bodyText = message["messages"]["Pipeline Message"];
				break;
			case "Wrong Pipeline Messages":
				headerText = (message["messages"]["Step Display Name"]) ? message["messages"]["Step Display Name"] : message["messages"]["Message"];
				bodyText = message["messages"]["Message"] + '\n' + message["messages"]["Pipeline Message"];
				break;
			case "Scenario Failed":
				panelClass = "panel-danger";
				headerText = message["messages"]["Message"]
				break;
			default:
				bodyText = JSON.stringify(message);
		}
		return {
			"class": panelClass,
			"header": jQuery('<div />').text(headerText).html(),
			"body": jQuery('<div />').text(bodyText).html()
		};
	};

	/**
	 * Accepts the wrong pipeline message that was prepared for diff.
	 * Used as a onClick function for save buttons.
	 * @param $event event of button being clicked.
	 * @param testName Name of the test that had the message
	 * @param index Index of the message
	 */
	$scope.savePipelineMessage = function($event, testName, index) {
		var button = angular.element($event.currentTarget);
		var body = button.parent().next();
		Api.Post("larva/save", JSON.stringify({"content": $scope.messages[testName][index]["messages"]["Pipeline Message"], "filepath": $scope.messages[testName][index]["messages"]["Filepath"]}), function (data) {
			var panel = '<div class="panel panel-info"><div class="panel-heading">New pipeline output has been saved.</div></div>';
			body.prepend(panel);
			button.remove();
		}, function () {
			var panel = '<div class="panel panel-danger"><div class="panel-heading">There has been an error!</div></div>';
			body.prepend(panel);
		});
	}

	/**
	 * Creates a panel for displaying the wrong pipeline messages for the diff.
	 * @param div The div to be populated.
	 * @param message The wrong pipeline messages that is prepared for diff.
	 * @param testName Name of the test that failed.
	 * @param index index of the message. It is used for creating a unique button for accepting the diff.
	 */
	$scope.createErrorMessagesForDiff = function(div, message, testName, index) {
		var diff = $scope.diff_lineMode(message["messages"]["Pipeline Message Expected"], message["messages"]["Pipeline Message"]);
		var bodyText = jQuery('<div />').text($scope.diff2text(diff)).html();
		var headerText = jQuery('<div />').text("Diff for " + message["messages"]["Step Display Name"]).html();

		bodyText = bodyText.replace(/\&lt\;span class\=\"(\w+)-(\w+)\"&gt\;/g, '<span class=\"$1-$2\">');
		bodyText = bodyText.replace(/&lt;\/span&gt;/g, "</span>");

		var button = '<button type="button" class="label label-info pull-right" ng-click="savePipelineMessage($event, \'' + testName + '\',' + index + ')">Save</button>';
		button = $compile(button)($scope);
		var header = '<div class="panel-heading" id="larva-test-details-' + index + '">' + headerText + '</div>';
		var body = '<div class="panel-body"><pre lang="xml">' + bodyText + '</pre></div>';
		div.append('<div class="panel panel-danger">' + header + body + '</div>');
		$('#larva-test-details-' + index).append(button);
	}

	/**
	 * Sets the body of the modal #test-details-body with the messages related to the given test.
	 * @param test The test object to be used for populating the modal.
	 */
	$scope.testDetails = function (test) {
		$('#modalTestTitle').text(test.name);
		$('#test-details-body').empty();
		$scope.messages[test.name].sort(function(a,b) {
			a["timestamp"] - b["timestamp"];
		});
		$scope.messages[test.name].forEach(function(element, index) {
			if(["Total", "Test Properties", ""].indexOf(element["logLevel"]) > -1)	return;

			if(element['logLevel'] === 'Wrong Pipeline Messages with Diff') {
				$scope.createErrorMessagesForDiff($('#test-details-body'), element, test.name, index);
				return;
			}

			var properties = $scope.messageDisplayProperties(element);

			var header = (properties["header"] !== "") ? '<div class="panel-heading">' + properties["header"] + '</div>' : '';
			var body = (properties["body"] !== "") ? '<div class="panel-body"><pre lang="xml">' + properties["body"] + '</pre></div>' : '';

			$('#test-details-body').append('<div class="panel ' + properties['class'] + '">' + header + body + '</div>');
		});
		$('#test-details').modal();
	};

	/**
	 * Downloads the current messages as a json file.
	 * The name is formatted as:
	 * "Larva Log yyyy-mm-dd hh:mm:ss.json"
	 */
	$scope.downloadMessages = function() {
		var dataStr = JSON.stringify($scope.messages);
		var dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);

		var today = new Date();
		var date = today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate();
		var time = today.getHours() + ":" + today.getMinutes() + ":" + today.getSeconds();
		var dateTime = date+' '+time;

		var exportFileDefaultName = 'Larva Log ' + dateTime + '.json';

		var linkElement = document.createElement('a');
		linkElement.setAttribute('href', dataUri);
		linkElement.setAttribute('download', exportFileDefaultName);
		linkElement.click();
	};

	/**
	 * Sets the scenarios select based on the rootDirectory selected.
	 * It is used as an onchange function for rootDirectory select.
	 */
	$scope.getScenarios = function() {
		Api.Post(
			"larva/scenarios",
			JSON.stringify({"rootDirectory": $scope.formRootDirectories}),
			function (data) {
				var scenarios = [];
				for(key in data["scenarios"]) {
					scenarios.push({"name": data["scenarios"][key], "value": key});
				}

				scenarios.sort(function (a1, b) {
					return a1[Object.keys(a1)[0]].localeCompare(b[Object.keys(b)[0]]);
				});
				$scope.scenarios = scenarios;
			}
			)
	}

	/**
	 * OnChange function for logLevels select. It checks if the last messages came
	 * from running the messages or uploading them and filters the messages based
	 * on that accordingly.
	 */
	$scope.updateLogLevel = function() {
		console.log("Inside the message with last action " + $scope.lastAction);
		if($scope.lastAction === "Run") {
			if($scope.logLevels.indexOf($scope.lowestLogLevel) <= $scope.logLevels.indexOf($scope.formLogLevel)) {
				console.log("FormLogLevel is bigger! nicee ");
				if ($scope.archivedMessages === null || Object.keys($scope.archivedMessages).length === 0) {
					$scope.archivedMessages = $scope.messages;
				}else {
					$scope.messages = $scope.archivedMessages;
				}
				$scope.flattenAndFilterMessages($scope.formLogLevel);
			}else {
				console.log("Starting new callback for messages ");
				Api.Post("larva/messages",
					JSON.stringify({"logLevel": $scope.formLogLevel}),
					function (data) {
						$scope.messages = {};
						$scope.tests = [];
						$scope.setMessages(data);
						$scope.lowestLogLevel = $scope.formLogLevel;
					},
					function (error) {
						$scope.addDisplayMessage("panel-danger", "Error retrieving logs: " + error);
					});
			}
		}else if($scope.lastAction === "Upload") {
			var logLevel = $scope.formLogLevel;
			if($scope.logLevels.indexOf($scope.lowestLogLevel) > $scope.logLevels.indexOf($scope.formLogLevel)) {
				logLevel = $scope.lowestLogLevel;
				$scope.addDisplayMessage("panel-warning", "The selected log level is lower than the uploaded file contains. Using the lowest possible log level [" + logLevel + "] instead.");
			}
			$scope.messages = $scope.archivedMessages;
			$scope.flattenAndFilterMessages(logLevel);
		}else if($scope.lastAction !== null) {
			$scope.addDisplayMessage("panel-danger", "Unexpected State: The frontend application is inconsistent! Please refresh the page!");
		}
	}

	/**
	 * Flattens and filters the current messages based on the given log level.
	 * @param logLevel A string representing the log level to be used for filtering. It needs to be included inside $scope.logLevels
	 * @returns {boolean} False if logLevel requirement is not met. True, when the filterin is finished.
	 */
	$scope.flattenAndFilterMessages = function(logLevel) {
		var logLevelIndex = $scope.logLevels.indexOf(logLevel);
		console.log("Got log level "+logLevel+"  " +logLevelIndex);
		if(logLevelIndex < 0) {
			console.error("Given log level is not in the possible log levels. Exiting filtering.");
			return false;
		}
		// Flatten messages
		var data = []
		for(test in $scope.messages) {
			var testMessages = $scope.messages[test];
			data = data.concat(testMessages);
		}
		// Filter
		// Not using data.foreach because it cant handle modifications.
		for(var i = 0; i < data.length; i++) {
			if($scope.logLevels.indexOf(data[i]["logLevel"]) < logLevelIndex) {
				data.splice(i, 1);
				i--;
			}
		}
		console.log("HERES THE FILTERED DATA");
		console.log(data);
		$scope.messages = {};
		$scope.tests = [];
		$scope.setMessages(data);
		return true;
	}

	// Generate form and set default values.
	$scope.setForm();
	$scope.formThreads = 1;
	$scope.formWaitcleanup = 100;
	$scope.formTimeout = -1;
}]);