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
	function initializeFrankConsole () {
		Debug.log("initializing Frank!Console", appConstants.init);
		if(appConstants.init < 2) {
			appConstants.init = 1;
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

				$rootScope.instanceName = data.instance.name;
				angular.element(".iaf-info").html(data.framework.name+" "+data.framework.version+": "+data.instance.name+" "+data.instance.version);

				$rootScope.dtapStage = data["dtap.stage"];
				$rootScope.dtapSide = data["dtap.side"];

				if($rootScope.dtapStage == "LOC") {
					Debug.setLevel(3);
				}

				$scope.updateConfigurations(data.configurations);

				//Was it able to retrieve the serverinfo without logging in?
				if(!$scope.loggedin) {
					Idle.setTimeout(false);
				}
				Hooks.call("init", false);
			}, function(message, statusCode, statusText) {
				if(statusCode == 500) {
					$state.go("pages.errorpage");
				}
			});
			Api.Get("environmentvariables", function(data) {
				if(data["Application Constants"]) {
					for (var configName in data["Application Constants"]) {
						var configConstants = data["Application Constants"][configName];
						appConstants = $.extend(appConstants, configConstants);
						break;
					}

					var idleTime = (parseInt(appConstants["console.idle.time"]) > 0) ? parseInt(appConstants["console.idle.time"]) : false;
					if(idleTime > 0) {
						var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
						Idle.setIdle(idleTime);
						Idle.setTimeout(idleTimeout);
					}
					else {
						Idle.unwatch();
					}
					$scope.databaseSchedulesEnabled = (appConstants["loadDatabaseSchedules.active"] === 'true');
					$scope.strutsConsoleEnabled = (appConstants["strutsConsole.enabled"] === 'true');
					$rootScope.$broadcast('appConstants');
				}
			});
		}

		var token = sessionStorage.getItem('authToken');
		$scope.loggedin = (token != null && token != "null") ? true : false;
	};

	Pace.on("done", initializeFrankConsole);
	$scope.$on('initializeFrankConsole', initializeFrankConsole);

	$scope.loggedin = false;

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

	$scope.updateConfigurations = function(configurations) {
		$scope.configurations = new Array();
		for(var i in configurations) {
			var config = configurations[i];
			if(config.name.startsWith("IAF_"))
				$scope.configurations.unshift(config);
			else
				$scope.configurations.push(config);
		}
	}

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
		}).catch(function(error) {
			Debug.error("An error occured while comparing IAF versions", error);
		});
		gTag.event('application.version', appConstants["application.version"]);

		Poller.add("server/warnings", function(configurations) {
			$scope.alerts = []; //Clear all old alerts

			configurations['All'] = {messages:configurations.messages};
			delete configurations.messages;

			configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
			delete configurations.totalErrorStoreCount;

			for(x in configurations.warnings) {
				$scope.addWarning('', configurations.warnings[x]);
			}

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
		}, true, 60000);

		var raw_adapter_data = {};
		var pollerCallback = function(allAdapters) {
			for(adapterName in raw_adapter_data) { //Check if any old adapters should be removed
				if(!allAdapters[adapterName]) {
					delete raw_adapter_data[adapterName];
					delete $rootScope.adapters[adapterName];
					Debug.log("removed adapter ["+adapterName+"]");
				}
			}
			for(adapterName in allAdapters) { //Add new adapter information
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
					adapter.flow = Misc.getServerPath() + 'iaf/api/adapters/' + adapter.name + "/flow";

					$rootScope.adapters[adapter.name] = adapter;

					$scope.updateAdapterSummary();
					Hooks.call("adapterUpdated", adapter);
				}
			}
		};

		//Get base information first, then update it with more details
		Api.Get("adapters", pollerCallback);
		$timeout(function() {
			Poller.add("adapters?expanded=all", pollerCallback, true);
			$scope.$broadcast('loading', false);
		}, 3000);
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

	$scope.hoverFeedback = function(rating) {
		$(".rating i").removeClass("fa-star").addClass("fa-star-o");
		$(".rating i:nth-child(-n+"+ (rating + 1) +")").addClass("fa-star").removeClass("fa-star-o");
	};

	$scope.showStrutsConsoleDisabled = function () {
		SweetAlert.Warning({
			title: "Struts Console Disabled",
			text: "The struts console has been disabled. In order to enable it, set the property [strutsConsole.enabled] to true.",
		});
	}
}])

.controller('LoadingPageCtrl', ['$scope', 'Api', '$state', function($scope, Api, $state) {
	Api.Get("server/health", function() {
		$state.go("pages.status");
	}, function(data, statusCode) {
		if(statusCode == 401) return;

		if(data.status == "SERVICE_UNAVAILABLE") {
			$state.go("pages.status");
		} else {
			$state.go("pages.errorpage");
		}
	});
}])

.controller('ErrorPageCtrl', ['$scope', 'Api', '$state', '$interval', '$rootScope', '$timeout', function($scope, Api, $state, $interval, $rootScope, $timeout) {
	$scope.cooldownCounter = 0;
	$scope.viewStackTrace = false;

	var cooldown = function(data) {
		$scope.cooldownCounter = 60;
		if(data.status == "INTERNAL_SERVER_ERROR") {
			$rootScope.startupError = data.error;
			$scope.stackTrace = data.stacktrace;

			var interval = $interval(function() {
				$scope.cooldownCounter--;
				if($scope.cooldownCounter < 1) {
					$interval.cancel(interval);
					$scope.checkState();
				}
			}, 1000);
		} else if(data.status == "SERVICE_UNAVAILABLE") {
			$state.go("pages.status");
		}
	};

	$scope.checkState = function() {
		Api.Get("server/health", function() {
			$state.go("pages.status");
			$timeout(function() { window.location.reload(); }, 50);
		}, cooldown);
	};

	$scope.checkState();
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

		$http.get(Misc.getServerPath()+"ConfigurationServlet").then(reload, reload).catch(function(error) {
			Debug.error("An error occured while foisting the IbisContext", error);
		});
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
		}).catch(function(error) {
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

.controller('StatusCtrl', ['$scope', 'Hooks', 'Api', 'SweetAlert', 'Poller', '$filter', '$state', 'Misc', '$anchorScroll', '$location',
		function($scope, Hooks, Api, SweetAlert, Poller, $filter, $state, Misc, $anchorScroll, $location) {

	var hash = $location.hash();
	var adapterName = $state.params.adapter;
	if(adapterName == "" && hash != "") { //If the adapter param hasn't explicitly been set
		adapterName = hash;
	} else {
		$location.hash(adapterName);
	}

	$scope.showContent = function(adapter) {
		if(adapter.status == "stopped") {
			return true;
		} else if(adapterName != "" && adapter.name == adapterName) {
			$anchorScroll();
			return true;
		} else {
			return false;
		}
	};

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
		var url = Misc.getServerPath() + 'iaf/api/configurations/';

		if(config == "All")
			url += "?flow=true";
		else {
			url += config + "/flow";
		}

		window.open(url);
	};

	$scope.isConfigStubbed = {};
	$scope.check4StubbedConfigs = function() {
		for(var i in $scope.configurations) {
			var config = $scope.configurations[i];
			$scope.isConfigStubbed[config.name] = config.stubbed;
		}
	};
	$scope.$watch('configurations', $scope.check4StubbedConfigs);

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
	$scope.addThread = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("adapters/" + adapter.name + "/receivers/" + receiver.name, {"action": "incthread"});
	};
	$scope.removeThread = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("adapters/" + adapter.name + "/receivers/" + receiver.name, {"action": "decthread"});
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

.controller('LoginCtrl', ['$scope', 'authService', '$timeout', 'appConstants', 'Alert', '$interval', 'Toastr', 
	function($scope, authService, $timeout, appConstants, Alert, $interval, Toastr) {
	Toastr.error("Unauthorized", "Please authenticate");
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

//** Ctrls **//
.controller('ManageConfigurationsCtrl', ['$scope', 'Api', function($scope, Api) {
	Api.Get("server/info", function(data) {
		$scope.updateConfigurations(data.configurations);
	});
}])

.controller('ManageConfigurationDetailsCtrl', ['$scope', '$state', 'Api', 'Debug', 'Misc', '$interval', 'SweetAlert', 'Toastr', function($scope, $state, Api, Debug, Misc, $interval, SweetAlert, Toastr) {
	$scope.loading = false;

	var promise = $interval(function() {
		update();
	}, 30000);
	$scope.$on('$destroy', function() {
		$interval.cancel(promise);
	});

	$scope.configuration = $state.params.name;
	function update() {
		$scope.loading = true;
		Api.Get("configurations/"+$state.params.name+"/versions", function(data) {
			for(x in data) {
				var configs = data[x];
				if(configs.active) {
					configs.actived = true;
				}
			}

			$scope.versions = data;
			$scope.loading = false;
		});
	};
	update();
	$scope.download = function(config) {
		window.open(Misc.getServerPath() + "iaf/api/configurations/"+config.name+"/versions/"+encodeURIComponent(config.version)+"/download");
	};
	$scope.deleteConfig = function(config) {
		var message = "";
		if(config.version) {
			message = "Are you sure you want to remove version '"+config.version+"'?";
		} else {
			message = "Are you sure?";
		}
		SweetAlert.Confirm({title:message}, function(imSure) {
			if(imSure) {
				Api.Delete("configurations/"+config.name+"/versions/"+encodeURIComponent(config.version), function() {
					Toastr.success("Successfully removed version '"+config.version+"'");
					update();
				});
			}
		});
	};

	$scope.activate = function(config) {
		for(x in $scope.versions) {
			var configs = $scope.versions[x];
			if(configs.version != config.version)
				configs.actived = false;
		}
		Api.Put("configurations/"+config.name+"/versions/"+encodeURIComponent(config.version), {activate:config.active}, function(data) {
			Toastr.success("Successfully changed startup config to version '"+config.version+"'");
		}, function() {
			update();
		});
	};

	$scope.scheduleReload = function(config) {
		Api.Put("configurations/"+config.name+"/versions/"+encodeURIComponent(config.version), {autoreload:config.autoreload}, function(data) {
			Toastr.success("Successfully "+(config.autoreload ? "enabled" : "disabled")+" Auto Reload for version '"+config.version+"'");
		}, function() {
			update();
		});
	};
}])

.controller('UploadConfigurationsCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.datasources = {};

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form.datasource = data.datasources[0];
	});

	$scope.form = {
			datasource:"",
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
		$scope.file = files[0]; //Can only parse 1 file!
	};

	$scope.submit = function() {
		if($scope.file == null) return;

		var fd = new FormData();
		if($scope.form.datasource && $scope.form.datasource != "")
			fd.append("datasource", $scope.form.datasource);
		else 
			fd.append("datasource", $scope.datasources[0]);

		fd.append("encoding", $scope.form.encoding);
		fd.append("multiple_configs", $scope.form.multiple_configs);
		fd.append("activate_config", $scope.form.activate_config);
		fd.append("automatic_reload", $scope.form.automatic_reload);
		fd.append("file", $scope.file, $scope.file.name);

		Api.Post("configurations", fd, function(data) {
			$scope.error = "";
			$scope.result = "Successfully uploaded configuration!";
			$scope.form = {
					datasource: $scope.datasources[0],
					encoding:"",
					multiple_configs:false,
					activate_config:true,
					automatic_reload:false,
			};
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	};

	$scope.reset = function() {
		$scope.result = "";
		$scope.error = "";
		$scope.form = {
				datasource: $scope.datasources[0],
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

.controller('EnvironmentVariablesCtrl', ['$scope', 'Api', 'appConstants', 'Toastr', function($scope, Api, appConstants, Toastr) {
	$scope.updateDynamicParams = false;
	$scope.variables = {};
	$scope.searchFilter = "";

	$scope.selectedConfiguration = null;
	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;
		$scope.configProperties = $scope.appConstants[name];
	};

	$scope.configProperties = [];
	$scope.environmentProperties = [];
	$scope.systemProperties = [];
	$scope.appConstants = [];
	Api.Get("environmentvariables", function(data) {
		var instanceName = null;
		for(var configName in data["Application Constants"]) {
			$scope.appConstants[configName] = convertPropertiesToArray(data["Application Constants"][configName]);
			if(instanceName == null) {
				instanceName = data["Application Constants"][configName]["instance.name"];
			}
		}
		$scope.changeConfiguration(instanceName);
		$scope.environmentProperties = convertPropertiesToArray(data["Environment Variables"]);
		$scope.systemProperties = convertPropertiesToArray(data["System Properties"]);
	});

	function convertPropertiesToArray(propertyList) {
		var tmp = new Array();
		for(var variableName in propertyList) {
			tmp.push({
				key: variableName,
				val: propertyList[variableName]
			});
		}
		return tmp;
	}

	Api.Get("server/log", function(data) {
		$scope.form = data;
	});

	$scope.form = {
		loglevel: "DEBUG",
		logIntermediaryResults: true,
		maxMessageLength: -1,
		errorLevels: ["DEBUG", "INFO", "WARN", "ERROR"],
		enableDebugger: true,
	};

	$scope.changeLoglevel = function(name) {
		$scope.form.loglevel = name;
	};

	$scope.submit = function(formData) {
		$scope.updateDynamicParams = true;
		Api.Put("server/log", formData, function() {
			Api.Get("server/log", function(data) {
				$scope.form = data;
				$scope.updateDynamicParams = false;
				Toastr.success("Successfully updated log configuration!");
			});
		}, function() {
			$scope.updateDynamicParams = false;
		});
	};
}])

.controller('AdapterStatisticsCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', '$timeout', function($scope, Api, $stateParams, SweetAlert, $timeout) {
	var adapterName = $stateParams.name;
	if(!adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.adapterName = adapterName;
	$scope.refreshing = false;

	$scope.hourlyStatistics = {
		labels: [],
		data: [],
	};

	$scope.stats = [];
	$scope.refresh = function() {
		$scope.refreshing = true;
		Api.Get("adapters/"+adapterName+"/statistics", function(data) {
			$scope.stats = data;

			var labels = [];
			var chartData = [];
			for(i in data["hourly"]) {
				var a = data["hourly"][i];
				labels.push(a["time"]);
				chartData.push(a["count"]);
			}
			$scope.hourlyStatistics.labels = labels;
			$scope.hourlyStatistics.data = chartData;

			$timeout(function(){
				$scope.refreshing = false;
			}, 500);
		});
	};

	$scope.dataset = {
		fill: false,
		backgroundColor: "#2f4050",
		borderColor: "#2f4050",
	};
	$scope.options = {
		responsive: true,
		maintainAspectRatio: false,
		scales: {
			yAxes: [{
				display: true,
				scaleLabel: {
					display: true,
					labelString: 'Messages Per Hour'
				},
				ticks: {
					beginAtZero: true,
					suggestedMax: 10
				}
			}]
		},
		tooltips: {
			mode: 'index',
			intersect: false,
			displayColors: false,
		},
		hover: {
			mode: 'nearest',
			intersect: true
		}
	};

	$timeout(function(){
		$scope.refresh();
	}, 1000);
}])

.controller('ErrorStorageBaseCtrl', ['$scope', 'Api', '$state', 'SweetAlert', 'Misc', function($scope, Api, $state, SweetAlert, Misc) {
	$scope.notes = [];
	$scope.addNote = function(type, message, removeQueue) {
		$scope.notes.push({type:type, message: message});
	};
	$scope.closeNote = function(index) {
		$scope.notes.splice(index, 1);
	};
	$scope.closeNotes = function() {
		$scope.notes = [];
	};

	$scope.adapterName = $state.params.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
	$scope.receiverName = $state.params.receiver;
	if(!$scope.receiverName)
		return SweetAlert.Warning("Invalid URL", "No receiver name provided!");

	$scope.base_url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/errorstorage";

	$scope.updateTable = function() {
		var table = $('#datatable').DataTable();
		if(table)
			table.draw();
	};

	$scope.doDeleteMessage = function(message, callback) {
		message.deleting = true;

		Api.Delete($scope.base_url+"/"+message.id, function() {
			if(callback != undefined && typeof callback == 'function')
				callback(message.id);
			$scope.addNote("success", "Successfully deleted message with ID: "+message.id);
			$scope.updateTable();
		}, function() {
			message.deleting = false;
			$scope.addNote("danger", "Unable to delete messages with ID: "+message.id);
			$scope.updateTable();
		}, false);
	};
	$scope.downloadMessage = function(messageId) {
		window.open(Misc.getServerPath() + "iaf/api/"+$scope.base_url+"/"+messageId+"/download");
	};

	$scope.doResendMessage = function(message, callback) {
		message.resending = true;

		Api.Put($scope.base_url+"/"+message.id, false, function() {
			if(callback != undefined && typeof callback == 'function')
				callback(message.id);
			$scope.addNote("success", "Successfully resent message with ID: "+message.id);
			$scope.updateTable();
		}, function(data) {
			message.resending = false;
			$scope.addNote("danger", "Unable to resend message ["+message.id+"]. "+data.error);
			$scope.updateTable();
		}, false);
	};
}])

.controller('AdapterErrorStorageCtrl', ['$scope', 'Api', '$compile', 'Cookies', function($scope, Api, $compile, Cookies) {
	$scope.closeNotes();
	$scope.selectedMessages = [];

	var a =  '<input icheck type="checkbox" ng-model="selectedMessages[message.id]"/>';
		a += '<div ng-show="!selectedMessages[message.id]">';
		a += '<a ui-sref="pages.errorstorage.view({adapter:adapterName,receiver:receiverName,messageId:message.id})" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
		a += '<button ladda="message.resending" data-style="slide-down" title="Resend Message" ng-click="resendMessage(message)" class="btn btn-warning btn-xs" type="button"><i class="fa fa-repeat"></i> Resend</button>';
		a += '<button ladda="message.deleting" data-style="slide-down" title="Delete Message" ng-click="deleteMessage(message)" class="btn btn-danger btn-xs" type="button"><i class="fa fa-times"></i> Delete</button>';
		a += '<button title="Download Message" ng-click="downloadMessage(message.id)" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';
		a += '</div';

	var columns = [
		{ "data": null, defaultContent: a, className: "m-b-xxs storageActions", bSortable: false},
		{ "name": "id", "data": "id", bSortable: false },
		{ "name": "insertDate", "data": "insertDate", className: "date" },
		{ "name": "host", "data": "host", bSortable: false },
		{ "name": "originalId", "data": "originalId", bSortable: false },
		{ "name": "correlationId", "data": "correlationId", bSortable: false },
		{ "name": "comment", "data": "comment", bSortable: false },
		{ "name": "expiryDate", "data": "expiryDate", className: "date", bSortable: false },
		{ "name": "label", "data": "label", bSortable: false },
	];

	var filterCookie = Cookies.get("errorstorageFilter");
	if(filterCookie) {
		for(i in columns) {
			var column = columns[i];
			if(column.name && filterCookie[column.name] === false) {
				column.visible = false;
			}
		}
		$scope.displayColumn = filterCookie;
	} else {
		$scope.displayColumn = {
			id: true,
			insertDate: true,
			host: true,
			originalId: true,
			correlationId: true,
			comment: true,
			expiryDate: true,
			label: true,
		}
	}

	$scope.dtOptions = {
		rowCallback: function(row, data) {
			var row = $(row);// .children("td:first").addClass("m-b-xxs");
			row.children("td.date").each(function(_, element) {
				var time = $(this).text();
				if(time)
					$(element).attr({"to-date": "", "time": time });
			});
			var scope = $scope.$new();
			scope.message = data;
			$scope.selectedMessages[data.id] = false;
			$compile(row)(scope);
		},
		searching: false,
		scrollX: true,
		orderCellsTop: true,
		serverSide: true,
		processing: true,
		paging: true,
		order: [[ 2, 'asc' ]],
		columns: columns,
		sAjaxDataProp: 'messages',
		ajax: function (data, callback, settings) {
			var start = data.start;
			var length = data.length;
			var order = data.order[0];
			var direction = order.dir; // asc or desc

			var url = $scope.base_url+"?max="+length+"&skip="+start+"&sort="+direction;
			var search = $scope.search;
			for(column in search) {
				var value = search[column];
				if(value) {
					url += "&"+column+"="+value;
				}
			}
			Api.Get(url, function(response) {
				response.draw = data.draw;
				response.recordsTotal = response.totalMessages;
				response.recordsFiltered = response.skipMessages + response.messageCount;
				callback(response);
			});
		}
	};

	$scope.search = {
		id: "",
		startDate: "",
		host: "",
		messageId: "",
		correlationId: "",
		comment: "",
		label: "",
	};

	$scope.updateFilter = function(column) {
		Cookies.set("errorstorageFilter", $scope.displayColumn);

		var table = $('#datatable').DataTable();
		if(table) {
			var tableColumn = table.column(column+":name");
			if(tableColumn && tableColumn.length == 1)
				tableColumn.visible( $scope.displayColumn[column] );
			table.draw();
		}
	}

	$scope.resendMessage = $scope.doResendMessage;
	$scope.deleteMessage = $scope.doDeleteMessage;

	$scope.selectAll = function() {
		for(i in $scope.selectedMessages) {
			$scope.selectedMessages[i] = true;
		}
	}
	$scope.unselectAll = function() {
		for(i in $scope.selectedMessages) {
			$scope.selectedMessages[i] = false;
		}
	}

	$scope.messagesResending = false;
	$scope.messagesDeleting = false;
	function getFormData() {
		var messageIds = [];
		for(i in $scope.selectedMessages) {
			if($scope.selectedMessages[i]) {
				messageIds.push(i);
				$scope.selectedMessages[i] = false;//unset the messageId
			}
		}

		var fd = new FormData();
		fd.append("messageIds", messageIds);
		return fd;
	}
	$scope.resendMessages = function() {
		$scope.messagesResending = true;
		Api.Post($scope.base_url, getFormData(), function() {
			$scope.messagesResending = false;
			$scope.addNote("success", "Successfully resent messages");
			$scope.updateTable();
		}, function(data) {
			$scope.messagesResending = false;
			$scope.addNote("danger", "Something went wrong, unable to resend all messages!");
			$scope.updateTable();
		});
	}
	$scope.deleteMessages = function() {
		$scope.messagesDeleting = true;
		Api.Delete($scope.base_url, getFormData(), function() {
			$scope.messagesDeleting = false;
			$scope.addNote("success", "Successfully deleted messages");
			$scope.updateTable();
		}, function(data) {
			$scope.messagesDeleting = false;
			$scope.addNote("danger", "Something went wrong, unable to delete all messages!");
			$scope.updateTable();
		});
	}
}])

.controller('AdapterViewStorageIdCtrl', ['$scope', 'Api', '$state', 'SweetAlert', function($scope, Api, $state, SweetAlert) {
	$scope.message = {};
	$scope.closeNotes();

	$scope.message.id = $state.params.messageId;
	if(!$scope.message.id)
		return SweetAlert.Warning("Invalid URL", "No message id provided!");

	Api.Get($scope.base_url+"/"+$scope.message.id, function(data) {
		$scope.message.data = data;
	}, function(_, statusCode, statusText) {
		if(statusCode == 500) {
			SweetAlert.Warning("An error occured while opening the message", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		} else {
			SweetAlert.Warning("Message not found", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		}
		$state.go("pages.errorstorage.list", {adapter:$scope.adapterName, receiver:$scope.receiverName});
	}, {responseType:'text', transformResponse: function(data) {
		return data;
	}});

	$scope.resendMessage = function(message) {
		$scope.doResendMessage(message, function(messageId) {
			//Go back to the error storage list if successful
			$state.go("pages.errorstorage.list", {adapter:$scope.adapterName, receiver:$scope.receiverName});
		});
	};

	$scope.deleteMessage = function(message) {
		$scope.doDeleteMessage(message, function(messageId) {
			//Go back to the error storage list if successful
			$state.go("pages.errorstorage.list", {adapter:$scope.adapterName, receiver:$scope.receiverName});
		});
	};
}])

.controller('MessageLogBaseCtrl', ['$scope', 'Misc', '$state', 'SweetAlert', function($scope, Misc, $state, SweetAlert) {
	$scope.adapterName = $state.params.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
	$scope.receiverName = $state.params.receiver;
	if(!$scope.receiverName)
		return SweetAlert.Warning("Invalid URL", "No receiver name provided!");

	var base_url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/messagelog";
	$scope.downloadMessage = function(messageId) {
		window.open(Misc.getServerPath() + "iaf/api/"+base_url+"/"+messageId+"/download");
	};

	$scope.updateTable = function() {
		var table = $('#datatable').DataTable();
		if(table)
			table.draw();
	};
}])

.controller('AdapterMessageLogListCtrl', ['$scope', 'Api', '$compile', function($scope, Api, $compile) {
	var base_url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/messagelog";

	var a =  '<a ui-sref="pages.messagelog.view({adapter:adapterName,receiver:receiverName,messageId:message.id})" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
		a += '<button title="Download Message" ng-click="downloadMessage(message.id)" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';

	var columns = [
		{ "data": null, defaultContent: a, className: "m-b-xxs", bSortable: false},
		{ "data": "id", bSortable: false },
		{ "data": "insertDate", className: "date" },
		{ "data": "type", bSortable: false },
		{ "data": "host", bSortable: false },
		{ "data": "originalId", bSortable: false },
		{ "data": "correlationId", bSortable: false },
		{ "data": "comment", bSortable: false },
		{ "data": "expiryDate", className: "date", bSortable: false },
		{ "data": "label", bSortable: false },
	];

	$scope.dtOptions = {
		rowCallback: function(row, data) {
			var row = $(row);// .children("td:first").addClass("m-b-xxs");
			row.children("td.date").each(function(_, element) {
				var time = $(this).text();
				if(time)
					$(element).attr({"to-date": "", "time": time });
			});
			var scope = $scope.$new();
			scope.message = data;
			$compile(row)(scope);
		},
		searching: false,
		scrollX: true,
		orderCellsTop: true,
		serverSide: true,
		processing: true,
		paging: true,
		order: [[ 2, 'desc' ]],
		columns: columns,
		sAjaxDataProp: 'messages',
		ajax: function (data, callback, settings) {
			var start = data.start;
			var length = data.length;
			var order = data.order[0];
			var direction = order.dir; // asc or desc

			var url = base_url+"?max="+length+"&skip="+start+"&sort="+direction;
			var search = $scope.search;
			for(column in search) {
				var value = search[column];
				if(value) {
					url += "&"+column+"="+value;
				}
			}
			Api.Get(url, function(response) {
				response.draw = data.draw;
				response.recordsTotal = response.totalMessages;
				response.recordsFiltered = response.skipMessages + response.messageCount;
				callback(response);
			});
		}
	};

	$scope.search = {
		id: "",
		startDate: "",
		type: "",
		host: "",
		messageId: "",
		correlationId: "",
		comment: "",
		label: "",
	};
}])

.controller('AdapterMessageLogViewCtrl', ['$scope', 'Api', '$state', 'SweetAlert', function($scope, Api, $state, SweetAlert) {
	$scope.message = {};

	$scope.message.id = $state.params.messageId;
	if(!$scope.message.id)
		return SweetAlert.Warning("Invalid URL", "No message id provided!");

	var url = "adapters/"+$scope.adapterName+"/receivers/"+$scope.receiverName+"/messagelog/"+$scope.message.id;
	Api.Get(url, function(data) {
		$scope.message.data = data;
	}, function(_, statusCode, statusText) {
		if(statusCode == 500) {
			SweetAlert.Warning("An error occured while opening the message", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		} else {
			SweetAlert.Warning("Message not found", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		}
		$state.go("pages.messagelog.list", {adapter:$scope.adapterName, receiver:$scope.receiverName});
	}, {responseType:'text', transformResponse: function(data) {
		return data;
	}});
}])

.controller('PipeMessageLogBaseCtrl', ['$scope', 'Misc', '$state', 'SweetAlert', function($scope, Misc, $state, SweetAlert) {
	$scope.adapterName = $state.params.adapter;
	if(!$scope.adapterName)
		return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
	$scope.pipeName = $state.params.pipe;
	if(!$scope.pipeName)
		return SweetAlert.Warning("Invalid URL", "No pipe name provided!");

	var base_url = "adapters/"+$scope.adapterName+"/pipes/"+$scope.pipeName+"/messagelog";
	$scope.downloadMessage = function(messageId) {
		window.open(Misc.getServerPath() + "iaf/api/"+base_url+"/"+messageId+"/download");
	};

	$scope.updateTable = function() {
		var table = $('#datatable').DataTable();
		if(table)
			table.draw();
	};
}])

.controller('PipeMessageLogListCtrl', ['$scope', 'Api', '$compile', function($scope, Api, $compile) {
	var base_url = "adapters/"+$scope.adapterName+"/pipes/"+$scope.pipeName+"/messagelog";

	var a =  '<a ui-sref="pages.pipemessagelog.view({adapter:adapterName,pipe:pipeName,messageId:message.id})" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
		a += '<button title="Download Message" ng-click="downloadMessage(message.id)" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';

	var columns = [
		{ "data": null, defaultContent: a, className: "m-b-xxs", bSortable: false},
		{ "data": "id", bSortable: false },
		{ "data": "insertDate", className: "date" },
		{ "data": "type", bSortable: false },
		{ "data": "host", bSortable: false },
		{ "data": "originalId", bSortable: false },
		{ "data": "correlationId", bSortable: false },
		{ "data": "comment", bSortable: false },
		{ "data": "expiryDate", className: "date", bSortable: false },
		{ "data": "label", bSortable: false },
	];

	$scope.dtOptions = {
		rowCallback: function(row, data) {
			var row = $(row);// .children("td:first").addClass("m-b-xxs");
			row.children("td.date").each(function(_, element) {
				var time = $(this).text();
				if(time)
					$(element).attr({"to-date": "", "time": time });
			});
			var scope = $scope.$new();
			scope.message = data;
			$compile(row)(scope);
		},
		searching: false,
		scrollX: true,
		orderCellsTop: true,
		serverSide: true,
		processing: true,
		paging: true,
		order: [[ 2, 'desc' ]],
		columns: columns,
		sAjaxDataProp: 'messages',
		ajax: function (data, callback, settings) {
			var start = data.start;
			var length = data.length;
			var order = data.order[0];
			var direction = order.dir; // asc or desc

			var url = base_url+"?max="+length+"&skip="+start+"&sort="+direction;
			var search = $scope.search;
			for(column in search) {
				var value = search[column];
				if(value) {
					url += "&"+column+"="+value;
				}
			}
			Api.Get(url, function(response) {
				response.draw = data.draw;
				response.recordsTotal = response.totalMessages;
				response.recordsFiltered = response.skipMessages + response.messageCount;
				callback(response);
			});
		}
	};

	$scope.search = {
		id: "",
		startDate: "",
		type: "",
		host: "",
		messageId: "",
		correlationId: "",
		comment: "",
		label: "",
	};
}])

.controller('PipeMessageLogViewCtrl', ['$scope', 'Api', '$state', 'SweetAlert', function($scope, Api, $state, SweetAlert) {
	$scope.message = {};

	$scope.message.id = $state.params.messageId;
	if(!$scope.message.id)
		return SweetAlert.Warning("Invalid URL", "No message id provided!");

	var url = "adapters/"+$scope.adapterName+"/pipes/"+$scope.pipeName+"/messagelog/"+$scope.message.id;
	Api.Get(url, function(data) {
		$scope.message.data = data;
	}, function(_, statusCode, statusText) {
		if(statusCode == 500) {
			SweetAlert.Warning("An error occured while opening the message", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		} else {
			SweetAlert.Warning("Message not found", "message id ["+$scope.message.id+"] error ["+statusText+"]");
		}
		$state.go("pages.pipemessagelog.list", {adapter:$scope.adapterName, pipe:$scope.pipeName});
	}, {responseType:'text', transformResponse: function(data) {
		return data;
	}});
	
}])

.controller('WebservicesCtrl', ['$scope', 'Api', 'Misc', function($scope, Api, Misc) {
	$scope.rootURL = Misc.getServerPath() + 'rest/';
	Api.Get("webservices", function(data) {
		$.extend($scope, data);
	});
}])

.controller('SecurityItemsCtrl', ['$scope', 'Api', '$rootScope', function($scope, Api, $rootScope) {
	$scope.sapSystems = [];
	$scope.serverProps;
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

.controller('SchedulerCtrl', ['$scope', 'Api', 'Poller', '$state', 'SweetAlert', function($scope, Api, Poller, $state, SweetAlert) {
	$scope.jobs = {};
	$scope.scheduler = {};

	Poller.add("schedules", function(data) {
		$.extend($scope, data);
	}, true, 5000);

	$scope.start = function() {
		Api.Put("schedules", {action: "start"});
	};

	$scope.pause = function() {
		Api.Put("schedules", {action: "pause"});
	};

	$scope.pause = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/job/"+jobName, {action: "pause"});
	};

	$scope.resume = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/job/"+jobName, {action: "resume"});
	};

	$scope.remove = function(jobGroup, jobName) {
		SweetAlert.Confirm({title:"Please confirm the deletion of '"+jobName+"'"}, function(imSure) {
			if(imSure) {
				Api.Delete("schedules/"+jobGroup+"/job/"+jobName);
			}
		});
	};

	$scope.trigger = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/job/"+jobName, {action: "trigger"});
	};

	$scope.edit = function(jobGroup, jobName) {
		$state.go('pages.edit_schedule', {name:jobName,group:jobGroup});
	};
}])

.controller('AddScheduleCtrl', ['$scope', 'Api', 'Misc', function($scope, Api, Misc) {
	$scope.state = [];
	$scope.addAlert = function(type, message) {
		$scope.state.push({type:type, message: message});
	};

	$scope.form = {
			name:"",
			adapter:"",
			receiver:"",
			cron:"",
			interval:-1,
			message:"",
			locker:false,
			lockkey:"",
			persistent:true,
	};

	$scope.submit = function() {
		var fd = new FormData();
		$scope.state = [];

		fd.append("name", $scope.form.name);
		fd.append("adapter", $scope.form.adapter);
		fd.append("receiver", $scope.form.receiver);
		fd.append("cron", $scope.form.cron);
		fd.append("interval", $scope.form.interval);
		fd.append("persistent", $scope.form.persistent);
		fd.append("message", $scope.form.message);
		fd.append("locker", $scope.form.locker);
		fd.append("lockkey", $scope.form.lockkey);

		Api.Post("schedules", fd, function(data) {
			$scope.addAlert("success", "Successfully added schedule!");
			$scope.form = {
					name:"",
					adapter:"",
					receiver:"",
					cron:"",
					interval:-1,
					message:"",
					locker:false,
					lockkey:"",
					persistent:true,
			};
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.addAlert("warning", error);
		}, false);
	};

}])

.controller('EditScheduleCtrl', ['$scope', 'Api', 'Misc', '$stateParams', '$state', function($scope, Api, Misc, $stateParams, $state) {
	$scope.state = [];
	$scope.addAlert = function(type, message) {
		$scope.state.push({type:type, message: message});
	};
	var url ="schedules/"+$stateParams.group+"/job/"+$stateParams.name;
	$scope.editMode = true;

	$scope.form = {
			name:"",
			adapter:"",
			receiver:"",
			cron:"",
			interval:-1,
			message:"",
			locker:false,
			lockkey:"",
			persistent:true,
	};

	Api.Get(url, function(data) {
		$scope.form = {
				name: data.name,
				adapter: data.adapter,
				receiver: data.receiver,
				cron: data.triggers[0].cronExpression,
				interval: -1,
				message: data.message,
				locker: data.locker,
				lockkey: data.lockkey,
				persistent: true,
		};
	});

	$scope.submit = function(form) {
		var fd = new FormData();
		$scope.state = [];

		fd.append("name", $scope.form.name);
		fd.append("adapter", $scope.form.adapter);
		fd.append("receiver", $scope.form.receiver);
		fd.append("cron", $scope.form.cron);
		fd.append("interval", $scope.form.interval);
		fd.append("persistent", $scope.form.persistent);
		fd.append("message", $scope.form.message);
		fd.append("locker", $scope.form.locker);
		fd.append("lockkey", $scope.form.lockkey);

		Api.Put(url, fd, function(data) {
			$scope.addAlert("success", "Successfully edited schedule!");
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.addAlert("warning", error);
		}, false);
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
		});
	};

	$scope.closeFile = function () {
		$scope.viewFile = false;
		$state.transitionTo('pages.logging', {directory: $scope.directory});
	};

	$scope.download = function (file) {
		var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + file.path;
		window.open(url, "_blank");
	};

	$scope.alert = false;
	var openDirectory = function (directory) {
		var url = "logging";
		if(directory) {
			url = "logging?directory="+directory;
		}

		Api.Get(url, function(data) {
			$scope.alert = false;
			$.extend($scope, data);
			$scope.path = data.directory;
			if(data.count > 500) {
				$scope.alert = "Total number of items ("+data.count+") exceeded maximum number, only showing first 500 items!";
			}
		}, function(data) {
			$scope.alert = (data) ? data.error : "An unknown error occured!";
		}, false);
	};

	$scope.open = function(file) {
		if(file.type == "directory") {
			$state.transitionTo('pages.logging', {directory: file.path});
		} else {
			$state.transitionTo('pages.logging', {directory: $scope.directory, file: file.name}, { notify: false, reload: false });
		}
	};

	//This is only false when the user opens the logging page
	var directory = ($stateParams.directory && $stateParams.directory.length > 0) ? $stateParams.directory : false;
	//The file param is only set when the user copy pastes an url in their browser
	if($stateParams.file && $stateParams.file.length > 0) {
		var file = $stateParams.file;

		$scope.directory = directory;
		$scope.path = directory+"/"+file;
		openFile({path: directory+"/"+file, name: file});
	}
	else {
		openDirectory(directory);
	}
}])

.controller('IBISstoreSummaryCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.datasources = {};

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form = {datasource: data.datasources[0]};
	});

	$scope.submit = function(formData) {
		if(!formData) formData = {};

		if(!formData.datasource) formData.datasource = $scope.datasources[0] || false;

		Api.Post("jdbc/summary", JSON.stringify(formData), function(data) {
			$scope.error = "";
			$.extend($scope, data);
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
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

		Api.Post("jms/message", fd, function(returnData) {
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

.controller('ExecuteJdbcQueryCtrl', ['$scope', 'Api', '$timeout', '$state', 'Cookies', function($scope, Api, $timeout, $state, Cookies) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;
	$scope.form = {};

	var executeQueryCookie = Cookies.get("executeQuery");
	if(executeQueryCookie) {
		$scope.form.query = executeQueryCookie.query;
		//Maybe also prefill datasource and result type?
	}

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form.datasource = data.datasources[0];
		$scope.form.resultType = data.resultTypes[0];
	});

	$scope.submit = function(formData) {
		$scope.processingMessage = true;
		if(!formData || !formData.query) {
			$scope.error = "Please specify a datasource, resulttype and query!";
			$scope.processingMessage = false;
			return;
		}
		if(!formData.datasource) formData.datasource = $scope.datasources[0] || false;
		if(!formData.resultType) formData.resultType = $scope.resultTypes[0] || false;

		Cookies.set("executeQuery", formData);

		Api.Post("jdbc/query", JSON.stringify(formData), function(returnData) {
			$scope.error = "";
			if(returnData == undefined || returnData == "") {
				returnData = "Ok";
			}
			$scope.result = returnData;
			$scope.processingMessage = false;
		}, function(errorData) {
			var error = (errorData.error) ? errorData.error : "";
			$scope.error = error;
			$scope.result = "";
			$scope.processingMessage = false;
		}, false);
	};

	$scope.reset = function() {
		$scope.form.query = "";
		$scope.result = "";
	};
}])

.controller('BrowseJdbcTablesCtrl', ['$scope', 'Api', '$timeout', '$state', function($scope, Api, $timeout, $state) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;

	Api.Get("jdbc", function(data) {
		$scope.datasources = data.datasources;
		$scope.form = {datasource: data.datasources[0]};
	});
	$scope.submit = function(formData) {
		$scope.processingMessage = true;
		if(!formData || !formData.table) {
			$scope.error = "Please specify a datasource and table name!";
			$scope.processingMessage = false;
			return;
		}
		if(!formData.datasource) formData.datasource = $scope.datasources[0] || false;
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
			$scope.processingMessage = false;
		}, function(errorData) {
			var error = (errorData.error) ? errorData.error : "";
			$scope.error = error;
			$scope.query = "";
			$scope.processingMessage = false;
		}, false);
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
		if(formData.message && formData.message != "") {
			var encoding = (formData.encoding && formData.encoding != "") ? ";charset="+formData.encoding : "";
			fd.append("message", new Blob([formData.message], {type: "text/plain"+encoding}), 'message');
		}
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify an adapter!");
			return;
		}
		if(!formData.message && !$scope.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}

		$scope.processingMessage = true;
		Api.Post("test-pipeline", fd, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		}, function(returnData) {
			$scope.result = "";
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
		if(formData.message && formData.message != "") {
			var encoding = (formData.encoding && formData.encoding != "") ? ";charset="+formData.encoding : "";
			fd.append("message", new Blob([formData.message], {type: "text/plain"+encoding}), 'message');
		}
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if(!formData.adapter) {
			$scope.addNote("warning", "Please specify a service!");
			return;
		}
		if(!formData.message && !$scope.file) {
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
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		});
	};
}]);
