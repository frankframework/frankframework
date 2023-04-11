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
	$scope.serverInfo = {};
	function initializeFrankConsole () {
		if(appConstants.init === -1) {
			appConstants.init = 0;
			Debug.log("Initializing Frank!Console");
		} else if(appConstants.init === 0) {
			Debug.log("Cancelling 2nd initialization attempt");
			Pace.stop();
			return ;
		} else {
			Debug.info("Loading Frank!Console", appConstants.init);
		}

		if(appConstants.init === 0) { //Only continue if the init state was -1
			appConstants.init = 1;
			Api.Get("server/info", function(data) {
				$scope.serverInfo = data;

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
				$rootScope.userName = data["userName"];

				if($rootScope.dtapStage == "LOC") {
					Debug.setLevel(3);
				}

				//Was it able to retrieve the serverinfo without logging in?
				if(!$scope.loggedin) {
					Idle.setTimeout(false);
				}

				Api.Get("server/configurations", function(data) {
					$scope.updateConfigurations(data);
				});
				Hooks.call("init", false);
			}, function(message, statusCode, statusText) {
				if(statusCode == 500) {
					$state.go("pages.errorpage");
				}
			});
			Api.Get("environmentvariables", function(data) {
				if(data["Application Constants"]) {
					appConstants = $.extend(appConstants, data["Application Constants"]["All"]); //make FF!Application Constants default

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
					$rootScope.$broadcast('appConstants');
				}
			});
		}

		var token = sessionStorage.getItem('authToken');
		$scope.loggedin = (token != null && token != "null") ? true : false;
	};

	Pace.on("done", initializeFrankConsole);
	$scope.$on('initializeFrankConsole', initializeFrankConsole);
	$timeout(initializeFrankConsole, 250);

	$scope.loggedin = false;

	$scope.reloadRoute = function() {
		$state.reload();
	};

	$scope.alerts = [];

	$scope.addAlert = function(type, configuration, message) {
		var line = message.match(/line \[(\d+)\]/);
		var isValidationAlert = message.indexOf("Validation") !== -1;
		var link = (line && !isValidationAlert) ? {name: configuration, '#': 'L' + line[1]} : undefined;
		$scope.alerts.push({
			link: link,
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
		$rootScope.$broadcast('configurations', $scope.configurations);
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
	$scope.getProcessStateIcon = function(processState){
		switch(processState) {
			case "Available":
				return "fa-server";
			case "InProcess":
				return "fa-gears";
			case "Done":
				return "fa-sign-in";
			case "Error":
				return "fa-times-circle";
			case "Hold":
				return "fa-pause-circle";
		}
	};
	$scope.getProcessStateIconColor = function(processState){
		switch(processState) {
			case "Available":
				return "success";
			case "InProcess":
				return "success";
			case "Done":
				return "success";
			case "Error":
				return "danger";
			case "Hold":
				return "warning";
		}
	};

	Hooks.register("init:once", function() {
		/* Check IAF version */
		console.log("Checking IAF version with remote...");
		$http.get("https://ibissource.org/iaf/releases/?q="+Misc.getUID($scope.serverInfo)).then(function(response) {
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
			$scope.serverInfo = null;
		}).catch(function(error) {
			Debug.error("An error occured while comparing IAF versions", error);
			$scope.serverInfo = null;
		});

		Poller.add("server/warnings", function(configurations) {
			$scope.alerts = []; //Clear all old alerts

			configurations['All'] = {messages:configurations.messages};
			delete configurations.messages;

			configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
			delete configurations.totalErrorStoreCount;

			for(let x in configurations.warnings) {
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
						if(adapterReceiver.state != 'started')
							adapter.status = 'warning';

						if(adapterReceiver.transactionalStores) {
							let store = adapterReceiver.transactionalStores["ERROR"];
							if(store && store.numberOfMessages > 0) {
								adapter.status = 'warning';
							}
						}
					}
					if(adapter.receiverReachedMaxExceptions){
						adapter.status = 'warning';
					}
					adapter.hasSender = false;
					adapter.sendersMessageLogCount=0;
					adapter.senderTransactionalStorageMessageCount=0;
					for(x in adapter.pipes) {
						let pipe = adapter.pipes[x];
						if(pipe.sender) {
							adapter.hasSender = true;
							if(pipe.hasMessageLog) {
								let count = parseInt(pipe.messageLogCount);
								if (!Number.isNaN(count)){
									if(pipe.isSenderTransactionalStorage) {
										adapter.senderTransactionalStorageMessageCount += count;
									} else {
										adapter.sendersMessageLogCount += count;
									}
								}
							}
						}
					}
/*					//If last message is WARN or ERROR change adapter status to warning.
					if(adapter.messages.length > 0 && adapter.status != 'stopped') {
						var message = adapter.messages[adapter.messages.length -1];
						if(message.level != "INFO")
							adapter.status = 'warning';
					}
*/
					if(adapter.state != "started") {
						adapter.status = "stopped";
					}

					$rootScope.adapters[adapter.name] = adapter;

					$scope.updateAdapterSummary();
					Hooks.call("adapterUpdated", adapter);
//					$scope.$broadcast('adapterUpdated', adapter);
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
			exception_starting:0,
			exception_stopping:0,
			error:0
		};
		var receiverSummary = {
			started:0,
			stopped:0,
			starting:0,
			stopping:0,
			exception_starting:0,
			exception_stopping:0,
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
		if($location.path() == "/status" && $location.hash()) {
			var el = angular.element("#"+$location.hash());
			if(el && el[0]) {
				el[0].scrollIntoView();
			}
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
		if(data.status == "error" || data.status == "INTERNAL_SERVER_ERROR") {
			$rootScope.startupError = data.error;
			$scope.stackTrace = data.stackTrace;

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

.controller('InformationCtrl', ['$scope', '$uibModalInstance', '$uibModal', 'Api', '$timeout', function($scope, $uibModalInstance, $uibModal, Api, $timeout) {
	$scope.error = false;
	Api.Get("server/info", function(data) {
		$.extend( $scope, data );
	}, function() {
		$scope.error = true;
	});

	$scope.close = function () {
		$uibModalInstance.close();
	};

	$scope.openCookieModel = function () {
		$uibModalInstance.close(); //close the current model

		$timeout(function() {
			$uibModal.open({
				templateUrl: 'views/common/cookieModal.html',
				size: 'lg',
				backdrop: 'static',
				controller: 'CookieModalCtrl',
			});
		});
	}
}])

.controller('FlowDiagramModalCtrl', ['$scope', '$uibModalInstance', 'xhr', function($scope, $uibModalInstance, xhr) {
	$scope.adapter = xhr.adapter;
	$scope.flow = xhr.data;

	$scope.close = function () {
		$uibModalInstance.close();
	};
}])

.controller('CookieModalCtrl', ['$scope', 'GDPR', 'appConstants', '$rootScope', '$uibModalInstance', function($scope, GDPR, appConstants, $rootScope, $uibModalInstance) {
	$scope.cookies = GDPR.defaults;

	$rootScope.$on('appConstants', function() {
		$scope.cookies = {
				necessary: true,
				personalization: appConstants.getBoolean("console.cookies.personalization", true),
				functional: appConstants.getBoolean("console.cookies.functional", true)
		};
	});

	$scope.consentAllCookies = function() {
		$scope.savePreferences({
			necessary: true,
			personalization: true,
			functional: true
		});
	};

	$scope.close = function() {
		$uibModalInstance.close();
	}

	$scope.savePreferences = function(cookies) {
		GDPR.setSettings(cookies);
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

			if((adapter.configuration == $scope.selectedConfiguration || $scope.selectedConfiguration == "All") && ($scope.filter == undefined || $scope.filter[adapter.status]))
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

.controller('StatusCtrl', ['$scope', 'Hooks', 'Api', 'SweetAlert', 'Poller', '$filter', '$state', 'Misc', '$anchorScroll', '$location', '$http',
		function($scope, Hooks, Api, SweetAlert, Poller, $filter, $state, Misc, $anchorScroll, $location, $http) {

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
		let compiledAdapterList = Array();
		let adapters = $filter('configurationFilter')($scope.adapters, $scope);
		for(adapter in adapters) {
			let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration+"/"+adapter);
		}
		Api.Put("adapters", {"action": "stop", "adapters": compiledAdapterList});
	};
	$scope.startAll = function() {
		let compiledAdapterList = Array();
		let adapters = $filter('configurationFilter')($scope.adapters, $scope);
		for(adapter in adapters) {
			let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration+"/"+adapter);
		}
		Api.Put("adapters", {"action": "start", "adapters": compiledAdapterList});
	};
	$scope.reloadConfiguration = function() {
		if($scope.selectedConfiguration == "All") return;

		$scope.isConfigReloading[$scope.selectedConfiguration] = true;

		Poller.getAll().stop();
		Api.Put("configurations/"+$scope.selectedConfiguration, {"action": "reload"}, function() {
			startPollingForConfigurationStateChanges(function() {
				Poller.getAll().start();
			});
		});
	};
	$scope.reloading = false;
	$scope.fullReload = function() {
		$scope.reloading = true;
		Poller.getAll().stop();
		Api.Put("configurations", {"action": "reload"}, function() {
			$scope.reloading = false;
			startPollingForConfigurationStateChanges(function() {
				Poller.getAll().start();
			});
		});
	};

	function startPollingForConfigurationStateChanges(callback) {
		Poller.add("server/configurations", function(configurations) {
			$scope.updateConfigurations(configurations);

			var ready = true;
			for(var i in configurations) {
				var config = configurations[i];
				//When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
				if(config.state != "STARTED" && !(config.state == "STOPPED" && config.exception != null)) {
					ready = false;
					break;
				}
			}
			if(ready) { //Remove poller once all states are STARTED
				Poller.remove("server/configurations");
				if(callback != null && typeof callback == "function") callback();
			}
		}, true);
	}

	$scope.showReferences = function() {
		window.open($scope.configurationFlowDiagram);
	};
	$scope.configurationFlowDiagram = null;
	$scope.updateConfigurationFlowDiagram = function(configurationName) {
		var url = Misc.getServerPath() + 'iaf/api/configurations/';
		if(configurationName == "All") {
			url += "?flow=true";
		} else {
			url += configurationName + "/flow";
		}
		$http.get(url).then(function(data) {
			let status = (data && data.status) ? data.status : 204;
			if(status == 200) {
				$scope.configurationFlowDiagram = url;
			}
		});
	}

	$scope.$on('appConstants', function() {
		$scope.updateConfigurationFlowDiagram($scope.selectedConfiguration);
	});

	$scope.isConfigStubbed = {};
	$scope.isConfigReloading = {};
	$scope.check4StubbedConfigs = function() {
		for(var i in $scope.configurations) {
			var config = $scope.configurations[i];
			$scope.isConfigStubbed[config.name] = config.stubbed;
			$scope.isConfigReloading[config.name] = config.state == "STARTING" || config.state == "STOPPING"; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
		}
	};
	$scope.$watch('configurations', $scope.check4StubbedConfigs);

	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;
		$scope.updateAdapterSummary(name);
		$scope.updateQueryParams();
		$scope.updateConfigurationFlowDiagram(name);
	};
	if($state.params.configuration != "All")
		$scope.changeConfiguration($state.params.configuration);


	$scope.startAdapter = function(adapter) {
		adapter.state = 'starting';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name), {"action": "start"});
	};
	$scope.stopAdapter = function(adapter) {
		adapter.state = 'stopping';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name), {"action": "stop"});
	};
	$scope.startReceiver = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), {"action": "start"});
	};
	$scope.stopReceiver = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), {"action": "stop"});
	};
	$scope.addThread = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), {"action": "incthread"});
	};
	$scope.removeThread = function(adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/"+adapter.configuration+"/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), {"action": "decthread"});
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

.controller('LoginCtrl', ['$scope', 'authService', '$timeout', 'Alert',
	function($scope, authService, $timeout, Alert) {
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
	Api.Get("server/configurations", function(data) {
		$scope.updateConfigurations(data);
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

.controller('UploadConfigurationsCtrl', ['$scope', 'Api', 'appConstants', function($scope, Api, appConstants) {
	$scope.datasources = {};
	$scope.form = {};

	$scope.$on('appConstants', function() {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
	});

	$scope.form = {
			datasource:"",
			encoding:"",
			multiple_configs:false,
			activate_config:true,
			automatic_reload:false,
	};

	$scope.file = null;

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
			$scope.result = "";
			for(pair in data){
				if(data[pair] == "loaded"){
					$scope.result += "Successfully uploaded configuration ["+pair+"]<br/>";
				} else {
					$scope.error += "Could not upload configuration from the file ["+pair+"]: "+data[pair]+"<br/>";
				}
			}

			$scope.form = {
					datasource: $scope.datasources[0],
					encoding:"",
					multiple_configs:false,
					activate_config:true,
					automatic_reload:false,
			};
			if($scope.file != null) {
				angular.element(".form-file")[0].value = null;
				$scope.file = null;
			}
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

.controller('ShowConfigurationCtrl', ['$scope', 'Api', '$state', '$location', function($scope, Api, $state, $location) {
	$scope.selectedConfiguration = ($state.params.name != '') ? $state.params.name : "All";
	$scope.loadedConfiguration = ($state.params.loaded != undefined && $state.params.loaded == false);

	$scope.update = function() {
		getConfiguration();
	};

	var anchor = $location.hash();
	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;
		$location.hash(''); //clear the hash from the url
		anchor = null; //unset hash anchor
		getConfiguration();
	};

	$scope.updateQueryParams = function() {
		var transitionObj = {};
		if($scope.selectedConfiguration != "All")
			transitionObj.name = $scope.selectedConfiguration;
		if(!$scope.loadedConfiguration)
			transitionObj.loaded = $scope.loadedConfiguration;

		$state.transitionTo('pages.configuration', transitionObj, { notify: false, reload: false });
	};

	$scope.clipboard = function() {
		if($scope.configuration) {
			var el = document.createElement('textarea');
			el.value = $scope.configuration;
			el.setAttribute('readonly', '');
			el.style.position = 'absolute';
			el.style.left = '-9999px';
			document.body.appendChild(el);
			el.select();
			document.execCommand('copy');
			document.body.removeChild(el);
		}
	}

	getConfiguration = function() {
		$scope.updateQueryParams();
		var uri = "configurations";
		if($scope.selectedConfiguration != "All") uri += "/" + $scope.selectedConfiguration;
		if($scope.loadedConfiguration) uri += "?loadedConfiguration=true";
		Api.Get(uri, function(data) {
			$scope.configuration = data;

			if(anchor) {
				$location.hash(anchor);
			}
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

.controller('EnvironmentVariablesCtrl', ['$scope', 'Api', 'appConstants', function($scope, Api, appConstants) {
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
		$scope.changeConfiguration("All");
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
}])

.controller('AdapterStatisticsCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', '$timeout', '$filter', 'appConstants', 'Debug', 'Misc', function($scope, Api, $stateParams, SweetAlert, $timeout, $filter, appConstants, Debug, Misc) {
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
	var defaults = {"name": "Name", "count": "Count", "min": "Min", "max": "Max", "avg": "Average", "stdDev": "StdDev", "sum": "Sum", "first": "First", "last": "Last"};
	$scope.statisticsTimeBoundaries = angular.copy(defaults);
	$scope.statisticsSizeBoundaries = angular.copy(defaults);
	function populateBoundaries() {
		var timeBoundaries = appConstants["Statistics.boundaries"].split(",");
		var sizeBoundaries = appConstants["Statistics.size.boundaries"].split(",");
		var percBoundaries = appConstants["Statistics.percentiles"].split(",");

		var publishPercentiles   = appConstants["Statistics.percentiles.publish"] == "true";
		var publishHistograms    = appConstants["Statistics.histograms.publish"] == "true";
		var calculatePercentiles = appConstants["Statistics.percentiles.internal"] == "true";
		var displayPercentiles = publishPercentiles || publishHistograms || calculatePercentiles;

		Debug.info("appending Statistic.boundaries", timeBoundaries, sizeBoundaries, percBoundaries);

		for(i in timeBoundaries) {
			var j = timeBoundaries[i];
			$scope.statisticsTimeBoundaries[j+"ms"] = "< " + j + "ms";
		}
		for(i in sizeBoundaries) {
			var j = sizeBoundaries[i];
			$scope.statisticsSizeBoundaries[j+"B"] = "< " + j + "B";
		}
		if (displayPercentiles) {
			for(i in percBoundaries) {
				var j = "p"+percBoundaries[i];
				$scope.statisticsTimeBoundaries[j] = j;
				$scope.statisticsSizeBoundaries[j] = j;
			}
		}
	};
	if(appConstants["Statistics.boundaries"]) {
		populateBoundaries(); //AppConstants already loaded
	}
	else {
		$scope.$on('appConstants', populateBoundaries); //Wait for appConstants trigger to load
	}

	$scope.statisticsNames = [];
	$scope.refresh = function() {
		$scope.refreshing = true;
		Api.Get("adapters/"+Misc.escapeURL(adapterName)+"/statistics", function(data) {
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

.controller('StorageBaseCtrl', ['$scope', 'Api', '$state', 'SweetAlert', 'Misc', function($scope, Api, $state, SweetAlert, Misc) {
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
	$scope.storageSourceName = $state.params.storageSourceName;
	if(!$scope.storageSourceName)
		return SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
	$scope.storageSource = $state.params.storageSource;
	if(!$scope.storageSource)
		return SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
	$scope.processState = $state.params.processState;
	if(!$scope.processState)
		return SweetAlert.Warning("Invalid URL", "No storage type provided!");

	$scope.base_url = "adapters/"+Misc.escapeURL($scope.adapterName)+ "/"+$scope.storageSource+"/"+Misc.escapeURL($scope.storageSourceName)+"/stores/"+$scope.processState;

	$scope.updateTable = function() {
		var table = $('#datatable').DataTable();
		if(table)
			table.draw();
	};

	$scope.doDeleteMessage = function(message, callback) {
		message.deleting = true;
		let messageId = message.id;
		Api.Delete($scope.base_url+"/messages/"+encodeURIComponent(encodeURIComponent(messageId)), function() {
			if(callback != undefined && typeof callback == 'function')
				callback(messageId);
			$scope.addNote("success", "Successfully deleted message with ID: "+messageId);
			$scope.updateTable();
		}, function() {
			message.deleting = false;
			$scope.addNote("danger", "Unable to delete messages with ID: "+messageId);
			$scope.updateTable();
		}, false);
	};
	$scope.downloadMessage = function(messageId) {
		window.open(Misc.getServerPath() + "iaf/api/"+$scope.base_url+"/messages/"+encodeURIComponent(encodeURIComponent(messageId))+"/download");
	};

	$scope.doResendMessage = function(message, callback) {
		message.resending = true;
		let messageId = message.id;
		Api.Put($scope.base_url+"/messages/"+encodeURIComponent(encodeURIComponent(messageId)), false, function() {
			if(callback != undefined && typeof callback == 'function')
				callback(message.id);
			$scope.addNote("success", "Message with ID: "+messageId+" will be reprocessed");
			$scope.updateTable();
		}, function(data) {
			message.resending = false;
			data = (data.error) ? data.error : data;
			$scope.addNote("danger", "Unable to resend message ["+messageId+"]. "+data);
			$scope.updateTable();
		}, false);
	};
}])

.controller('AdapterStorageCtrl', ['$scope', 'Api', '$compile', 'Cookies','Session', 'SweetAlert', function($scope, Api, $compile, Cookies, Session, SweetAlert) {
	$scope.closeNotes();
	$scope.selectedMessages = [];
	$scope.targetStates = [];
	var a = '';

	a += '<input icheck type="checkbox" ng-model="selectedMessages[message.id]"/>';
	a += '<div ng-show="!selectedMessages[message.id]">';
	a += '<a ui-sref="pages.storage.view({adapter:adapterName,receiver:receiverName,processState:processState,messageId: message.id })" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
	a += '<button ng-if="::processState==\'Error\'" ladda="message.resending" data-style="slide-down" title="Resend Message" ng-click="resendMessage(message)" class="btn btn-warning btn-xs" type="button"><i class="fa fa-repeat"></i> Resend</button>';
	a += '<button ng-if="::processState==\'Error\'" ladda="message.deleting" data-style="slide-down" title="Delete Message" ng-click="deleteMessage(message)" class="btn btn-danger btn-xs" type="button"><i class="fa fa-times"></i> Delete</button>';
	a += '<button title="Download Message" ng-click="downloadMessage(message.id)" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';
	a += '</div';

	var columns = [
		{ "data": null, defaultContent: a, className: "m-b-xxs storageActions", bSortable: false},
		{ "name": "pos", "data": "position", bSortable: false, defaultContent:"" },
		{ "name": "id", "data": "messageId", bSortable: false, defaultContent:"" },
		{ "name": "insertDate", "data": "insertDate", className: "date", defaultContent:"" },
		{ "name": "host", "data": "host", bSortable: false, defaultContent:"" },
		{ "name": "originalId", "data": "originalId", bSortable: false, defaultContent:"" },
		{ "name": "correlationId", "data": "correlationId", bSortable: false, defaultContent:"" },
		{ "name": "comment", "data": "comment", bSortable: false, defaultContent:"" },
		{ "name": "expiryDate", "data": "expiryDate", className: "date", bSortable: false, defaultContent:"" },
		{ "name": "label", "data": "label", bSortable: false, defaultContent:"" },
	];
	var filterCookie = Cookies.get($scope.processState+"Filter");
	if(filterCookie) {
		for(let column in columns) {
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

	$scope.searchUpdated = function() {
		$scope.searching = true;
		$scope.updateTable();
	};

	$scope.truncated = false;
	$scope.truncateButtonText = "Truncate displayed data";
	$scope.truncate = function() {
		$scope.truncated = !$scope.truncated;
		if($scope.truncated) {
			$scope.truncateButtonText="Show original";
		} else {
			$scope.truncateButtonText="Truncate displayed data";
		}
		$scope.updateTable();
	};

	$scope.dtOptions = {
		stateSave: true,
		stateSaveCallback: function(settings, data) {
			data.columns = columns;
			Session.set('DataTable'+$scope.processState, data);
		},
		stateLoadCallback: function(settings) {
			return Session.get('DataTable'+$scope.processState);
		},
		drawCallback: function( settings ) {
			// reset visited rows with all draw actions e.g. pagination, filter, search
			$scope.selectedMessages = [];
			var table = $('#datatable').DataTable();
			var data = table.rows( {page:'current'} ).data();
			// visit rows in the current page once (draw event is fired after rowcallbacks)
			for(var i=0;i<data.length;i++){
				$scope.selectedMessages[data[i].id] = false;
			}
		},
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
		bAutoWidth: false,
		orderCellsTop: true,
		serverSide: true,
		processing: true,
		paging: true,
		lengthMenu: [10,25,50,100,500,999],
		order: [[ 3, 'asc' ]],
		columns: columns,
		columnDefs: [ {
			targets: 0,
			render: function ( data, type, row ) {
				if(type === 'display') {
					data["messageId"] = data["id"];
					for(let i in data) {
						if(i == "id") continue;
						var columnData = data[i];
						if(typeof columnData == 'string' && columnData.length > 30 && $scope.truncated) {
							data[i] = '<span title="'+columnData.replace(/"/g, '&quot;')+'">'+columnData.substr(0, 15)+' &#8230; '+columnData.substr(-15)+'</span>';
						}
					}
				}
				return data;
			}
		}],
		sAjaxDataProp: 'messages',
		ajax: function (data, callback, settings) {
			var start = data.start;
			var length = data.length;
			var order = data.order[0];
			var direction = order.dir; // asc or desc

			var url = $scope.base_url+"?max="+length+"&skip="+start+"&sort="+direction;
			let search = $scope.search;
			let searchSession = {};
			for(let column in search) {
				let text = search[column];
				if(text) {
					url += "&"+column+"="+text;
					searchSession[column] = text;
				}
			}
			Session.set('search', searchSession);
			Api.Get(url, function(response) {
				response.draw = data.draw;
				response.recordsTotal = response.totalMessages;
				$scope.targetStates = response.targetStates;
				callback(response);
				$scope.searching = false;
				$scope.clearSearchLadda = false;
			}, function(error){
				$scope.searching = false;
				$scope.clearSearchLadda = false;
			});
		}
	};

	let searchSession = Session.get('search');
	$scope.search = {
		id: searchSession ? searchSession['id'] : "",
		startDate: searchSession ? searchSession["startDate"] : "",
		endDate: searchSession ? searchSession["endDate"] : "",
		host: searchSession ? searchSession["host"] : "",
		messageId: searchSession ? searchSession["messageId"] : "",
		correlationId: searchSession ? searchSession["correlationId"] : "",
		comment: searchSession ? searchSession["comment"] : "",
		label: searchSession ? searchSession["label"] : "",
		message: searchSession ? searchSession["message"] : ""
	};

	$scope.clearSearch = function() {
		$scope.clearSearchLadda = true;
		Session.remove('search');
		$scope.search = {};
		$scope.updateTable();
	};

	$scope.filterBoxExpanded = false;
	var search = $scope.search;
	if(search){
		for(let column in search) {
			let value = search[column];
			if(value && value != "") {
				$scope.filterBoxExpanded = true;
			}
		}
	}

	$scope.updateFilter = function(column) {
		Cookies.set($scope.processState+"Filter", $scope.displayColumn);

		let table = $('#datatable').DataTable();
		if(table) {
			let tableColumn = table.column(column+":name");
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
		let fd = getFormData();
		if($scope.isSelectedMessages(fd)) {
			$scope.messagesResending = true;
			Api.Post($scope.base_url, fd, function() {
				$scope.messagesResending = false;
				$scope.addNote("success", "Selected messages will be reprocessed");
				$scope.updateTable();
			}, function(data) {
				$scope.messagesResending = false;
				$scope.addNote("danger", "Something went wrong, unable to resend all messages!");
				$scope.updateTable();
			});
		}
	}
	$scope.deleteMessages = function() {
		let fd = getFormData();
		if($scope.isSelectedMessages(fd)) {
			$scope.messagesDeleting = true;
			Api.Delete($scope.base_url, fd, function() {
				$scope.messagesDeleting = false;
				$scope.addNote("success", "Successfully deleted messages");
				$scope.updateTable();
			}, function(data) {
				$scope.messagesDeleting = false;
				$scope.addNote("danger", "Something went wrong, unable to delete all messages!");
				$scope.updateTable();
			});
		}
	}

	$scope.downloadMessages = function() {
		let fd = getFormData();
		if($scope.isSelectedMessages(fd)) {
			$scope.messagesDownloading = true;
			Api.Post($scope.base_url+"/messages/download", fd, function(response) {
				let blob = new Blob([response], {type: 'application/octet-stream'});
				let downloadLink = document.createElement('a');
				downloadLink.href = window.URL.createObjectURL(blob);
				downloadLink.setAttribute('download', 'messages.zip');
				document.body.appendChild(downloadLink);
				downloadLink.click();
				downloadLink.parentNode.removeChild(downloadLink);
				$scope.addNote("success", "Successfully downloaded messages");
				$scope.messagesDownloading = false;
			}, function(data) {
				$scope.messagesDownloading = false;
				$scope.addNote("danger", "Something went wrong, unable to download selected messages!");
			}, null, 'blob');
		}
	}

	$scope.changingProcessState = false;
	$scope.changeProcessState = function(processState, targetState) {
		let fd = getFormData();
		if($scope.isSelectedMessages(fd)) {
			$scope.changingProcessState = true;
			Api.Post($scope.base_url+"/move/"+targetState, fd, function() {
				$scope.changingProcessState = false;
				$scope.addNote("success", "Successfully changed the state of messages to "+targetState);
				$scope.updateTable();
			}, function(data) {
				$scope.changingProcessState = false;
				$scope.addNote("danger", "Something went wrong, unable to move selected messages!");
				$scope.updateTable();
			});
		}
	}

	$scope.isSelectedMessages = function(data){
		let selectedMessages = data.get("messageIds");
		if(!selectedMessages || selectedMessages.length == 0){
			SweetAlert.Warning("No message selected!");
			return false;
		} else {
			return true;
		}
	};
}])

.controller('AdapterViewStorageIdCtrl', ['$scope', 'Api', '$state', 'SweetAlert', function($scope, Api, $state, SweetAlert) {
	$scope.message = {};
	$scope.closeNotes();

	$scope.message.id = $state.params.messageId;
	if(!$scope.message.id)
		return SweetAlert.Warning("Invalid URL", "No message id provided!");

	Api.Get($scope.base_url+"/messages/"+encodeURIComponent(encodeURIComponent($scope.message.id)), function(data) {
		$scope.metadata = data;
	}, function(errorData, statusCode, errorMsg) {
		let error = (errorData) ? errorData.error : errorMsg;
		if(statusCode == 500) {
			SweetAlert.Warning("An error occured while opening the message", "message id ["+$scope.message.id+"] error ["+error+"]");
		} else {
			SweetAlert.Warning("Message not found", "message id ["+$scope.message.id+"] error ["+error+"]");
		}
		$state.go("pages.storage.list", {adapter:$scope.adapterName, storageSource:$scope.storageSource, storageSourceName:$scope.storageSourceName, processState:$scope.processState});
	});

	$scope.resendMessage = function(message) {
		$scope.doResendMessage(message, function(messageId) {
			//Go back to the storage list if successful
			$state.go("pages.storage.list", {adapter:$scope.adapterName, storageSource:$scope.storageSource, storageSourceName:$scope.storageSourceName, processState:$scope.processState});
		});
	};

	$scope.deleteMessage = function(message) {
		$scope.doDeleteMessage(message, function(messageId) {
			//Go back to the storage list if successful
			$state.go("pages.storage.list", {adapter:$scope.adapterName, storageSource:$scope.storageSource, storageSourceName:$scope.storageSourceName, processState:$scope.processState});
		});
	};
}])
.controller('ConnectionOverviewCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.dtOptions = {
		processing: true,
		lengthMenu: [50,100,250,500],
		columns : [
			{"data": "adapterName", bSortable: false},
			{"data": "componentName", bSortable: false},
			{"data": "domain", bSortable: false},
			{"data": "destination", bSortable: false},
			{"data": "direction", bSortable: false}
		],
		sAjaxDataProp: 'data',
		ajax: function (data, callback, settings) {
			Api.Get("connections", function(response) {
				response.draw = data.draw;
				response.recordsTotal = response.data.length;
				response.recordsFiltered = response.data.length;
				callback(response);
			});
		},
		initComplete: function () {
			this.api().columns([2,4]).every( function () {
				var column = this;
				var select = $('<select><option value=""></option></select>')
					.appendTo( $(column.header()) )
					.on( 'change', function () {
						var val = $.fn.dataTable.util.escapeRegex(
							$(this).val()
						);
						column.search( val ? '^'+val+'$' : '', true, false ).draw();
					});

					column.data().unique().sort().each( function ( d, j ) {
						select.append( '<option value="'+d+'">'+d+'</option>' )
					});
			});
			this.api().columns([0,1,3]).every( function () {
				var column = this;
				$('<input type="text" style="display:block; font-size:12px" placeholder="Search..." />')
					.appendTo( $(column.header()) )
					.on( 'keyup change clear', function () {
						if ( column.search() !== this.value ) {
							column.search( this.value ).draw();
						}
					});
			});
		}
	};
}])
.controller('InlineStoreOverviewCtrl', ['$scope', 'Api', function($scope, Api) {
	Api.Get("inlinestores/overview", function(data) {
		$scope.result = data;
	});
	
}])
.controller('WebservicesCtrl', ['$scope', 'Api', 'Misc', function($scope, Api, Misc) {
	$scope.rootURL = Misc.getServerPath();
	$scope.compileURL = function(apiListener) {
		return $scope.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
	}
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
	$scope.searchFilter = "";

	Poller.add("schedules", function(data) {
		$.extend($scope, data);
	}, true, 5000);
	$scope.$on('$destroy', function() {
		Poller.remove("schedules");
	});

	$scope.start = function() {
		Api.Put("schedules", {action: "start"});
	};

	$scope.pauseScheduler = function() {
		Api.Put("schedules", {action: "pause"});
	};

	$scope.pause = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/jobs/"+jobName, {action: "pause"});
	};

	$scope.resume = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/jobs/"+jobName, {action: "resume"});
	};

	$scope.remove = function(jobGroup, jobName) {
		SweetAlert.Confirm({title:"Please confirm the deletion of '"+jobName+"'"}, function(imSure) {
			if(imSure) {
				Api.Delete("schedules/"+jobGroup+"/jobs/"+jobName);
			}
		});
	};

	$scope.trigger = function(jobGroup, jobName) {
		Api.Put("schedules/"+jobGroup+"/jobs/"+jobName, {action: "trigger"});
	};

	$scope.edit = function(jobGroup, jobName) {
		$state.go('pages.edit_schedule', {name:jobName,group:jobGroup});
	};
}])

.controller('AddScheduleCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.state = [];
	$scope.addLocalAlert = function(type, message) {
		$scope.state.push({type:type, message: message});
	};

	$scope.selectedConfiguration = "";
	$scope.form = {
			name:"",
			group:"",
			adapter:"",
			listener:"",
			cron:"",
			interval:"",
			message:"",
			description:"",
			locker:false,
			lockkey:"",
	};

	$scope.submit = function() {
		var fd = new FormData();
		$scope.state = [];

		fd.append("name", $scope.form.name);
		fd.append("group", $scope.form.group);
		fd.append("configuration", $scope.selectedConfiguration);
		fd.append("adapter", $scope.form.adapter);
		fd.append("listener", $scope.form.listener);
		fd.append("cron", $scope.form.cron);
		fd.append("interval", $scope.form.interval);
		fd.append("message", $scope.form.message);
		fd.append("description", $scope.form.description);
		fd.append("locker", $scope.form.locker);
		fd.append("lockkey", $scope.form.lockkey);

		Api.Post("schedules", fd, function(data) {
			$scope.addLocalAlert("success", "Successfully added schedule!");
			$scope.selectedConfiguration = "";
			$scope.form = {
					name:"",
					group:"",
					adapter:"",
					listener:"",
					cron:"",
					interval:"",
					message:"",
					description:"",
					locker:false,
					lockkey:"",
			};
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.addLocalAlert("warning", error);
		}, false);
	};
}])

.controller('EditScheduleCtrl', ['$scope', 'Api', '$stateParams', function($scope, Api, $stateParams) {
	$scope.state = [];
	$scope.addLocalAlert = function(type, message) {
		$scope.state.push({type:type, message: message});
	};
	var url ="schedules/"+$stateParams.group+"/jobs/"+$stateParams.name;
	$scope.editMode = true;
	$scope.selectedConfiguration = "";

	$scope.form = {
			name:"",
			group:"",
			adapter:"",
			listener:"",
			cron:"",
			interval:"",
			message:"",
			description:"",
			locker:false,
			lockkey:"",
	};

	Api.Get(url, function(data) {
		$scope.selectedConfiguration = data.configuration;
		$scope.form = {
				name: data.name,
				group: data.group,
				adapter: data.adapter,
				listener: data.listener,
				cron: data.triggers[0].cronExpression || "",
				interval: data.triggers[0].repeatInterval || "",
				message: data.message,
				description: data.description,
				locker: data.locker,
				lockkey: data.lockkey,
		};
	});

	$scope.submit = function(form) {
		var fd = new FormData();
		$scope.state = [];

		fd.append("name", $scope.form.name);
		fd.append("group", $scope.form.group);
		fd.append("configuration", $scope.selectedConfiguration);
		fd.append("adapter", $scope.form.adapter);
		fd.append("listener", $scope.form.listener);
		if($scope.form.cron)
			fd.append("cron", $scope.form.cron);
		if($scope.form.interval)
			fd.append("interval", $scope.form.interval);
		fd.append("message", $scope.form.message);
		fd.append("description", $scope.form.description);
		fd.append("locker", $scope.form.locker);
		if($scope.form.lockkey)
			fd.append("lockkey", $scope.form.lockkey);

		Api.Put(url, fd, function(data) {
			$scope.addLocalAlert("success", "Successfully edited schedule!");
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.addLocalAlert("warning", error);
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

		var URL = Misc.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + Misc.escapeURL(file.path) + params;
		if(resultType == "xml") {
			window.open(URL, "_blank");
			return;
		}

		$scope.viewFile = URL;
		$timeout(function() {
			var iframe = angular.element("iframe");

			iframe[0].onload = function() {
				var iframeBody = $(iframe[0].contentWindow.document.body);
				iframe.css({"height": iframeBody.height() + 50});
			};
		});
	};

	$scope.closeFile = function () {
		$scope.viewFile = false;
		$state.transitionTo('pages.logging_show', {directory: $scope.directory});
	};

	$scope.download = function (file) {
		var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + Misc.escapeURL(file.path);
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
			if(data.count > data.list.length) {
				$scope.alert = "Total number of items ["+data.count+"] exceeded maximum number, only showing first ["+(data.list.length-1)+"] items!";
			}
		}, function(data) {
			$scope.alert = (data) ? data.error : "An unknown error occured!";
		}, false);
	};

	$scope.open = function(file) {
		if(file.type == "directory") {
			$state.transitionTo('pages.logging_show', {directory: file.path});
		} else {
			$state.transitionTo('pages.logging_show', {directory: $scope.directory, file: file.name}, { notify: false, reload: false });
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

.controller('LogSettingsCtrl', ['$scope', 'Api', 'Misc', '$timeout', '$state','Toastr', function($scope, Api, Misc, $timeout, $state, Toastr) {
	$scope.updateDynamicParams = false;

	$scope.loggers = {};
	var logURL = "server/logging";
	function updateLogInformation() {
		Api.Get(logURL+"/settings", function(data) {
			$scope.loggers = data.loggers;
			$scope.loggersLength = Object.keys(data.loggers).length;
			$scope.definitions = data.definitions;
		}, function(data) {
			console.error(data);
		});
	}
	updateLogInformation();

	$scope.errorLevels = ["DEBUG", "INFO", "WARN", "ERROR"];
	Api.Get(logURL, function(data) {
		$scope.form = data;
		$scope.errorLevels = data.errorLevels;
	});

	$scope.form = {
		loglevel: "DEBUG",
		logIntermediaryResults: true,
		maxMessageLength: -1,
		errorLevels: $scope.errorLevels,
		enableDebugger: true,
	};

	//Root logger level
	$scope.changeRootLoglevel = function(level) {
		$scope.form.loglevel = level;
	};

	//Individual level
	$scope.changeLoglevel = function(logger, level) {
		Api.Put(logURL+"/settings", {logger:logger, level:level}, function() {
			Toastr.success("Updated logger ["+logger+"] to ["+level+"]");
			updateLogInformation();
		});
	};

	//Reconfigure Log4j2
	$scope.reconfigure = function () {
		Api.Put(logURL+"/settings", {reconfigure:true}, function() {
			Toastr.success("Reconfigured log definitions!");
			updateLogInformation();
		});
	}

	$scope.submit = function(formData) {
		$scope.updateDynamicParams = true;
		Api.Put(logURL, formData, function() {
			Api.Get(logURL, function(data) {
				$scope.form = data;
				$scope.updateDynamicParams = false;
				Toastr.success("Successfully updated log configuration!");
				updateLogInformation();
			});
		}, function() {
			$scope.updateDynamicParams = false;
		});
	};
}])

.controller('IBISstoreSummaryCtrl', ['$scope', 'Api', '$location', 'appConstants', function($scope, Api, $location, appConstants) {
	$scope.datasources = {};
	$scope.form = {};

	$scope.$on('appConstants', function() {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
	});

	if($location.search() && $location.search().datasource != null) {
		var datasource = $location.search().datasource;
		fetch(datasource);
	}
	function fetch(datasource) {
		Api.Post("jdbc/summary", JSON.stringify({datasource: datasource}), function(data) {
			$scope.error = "";
			$.extend($scope, data);
		}, function(errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	}

	$scope.submit = function(formData) {
		if(!formData) formData = {};

		if(!formData.datasource) formData.datasource = $scope.datasources[0] || false;
		$location.search('datasource', formData.datasource);
		fetch(formData.datasource);
	};

	$scope.reset = function() {
		$location.search('datasource', null);
		$scope.result = "";
		$scope.error = "";
	};
}])

.controller('LiquibaseScriptCtrl', ['$scope', 'Api', 'Misc', function($scope, Api, Misc) {
	$scope.form = {};
	$scope.file = null;

	let findFirstAvailabeConfiguration = function() {
		for(let i in $scope.configurations) {
			let configuration = $scope.configurations[i];
			if(configuration.jdbcMigrator) {
				$scope.form.configuration = configuration.name;
				break;
			}
		}
	}
	findFirstAvailabeConfiguration();
	$scope.$on('configurations', findFirstAvailabeConfiguration);

	$scope.download = function() {
		window.open(Misc.getServerPath() + "iaf/api/jdbc/liquibase/");
	};

	$scope.generateSql = false;
	$scope.submit = function(formData) {
		if(!formData) formData = {};
		var fd = new FormData();
		$scope.generateSql=true;
		if($scope.file != null) {
			fd.append("file", $scope.file);
		}

		fd.append("configuration", formData.configuration);
		Api.Post("jdbc/liquibase", fd, function(returnData) {
			$scope.error = "";
			$scope.generateSql=false;
			$.extend($scope, returnData);
		}, function(errorData, status, errorMsg) {
			$scope.generateSql=false;
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	};

}])

.controller('SendJmsMessageCtrl', ['$scope', 'Api', function($scope, Api) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"];
	$scope.processing = false;
	Api.Get("jms", function(data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	$scope.file = null;

	$scope.submit = function(formData) {
		$scope.processing = true;
		if(!formData) return;

		var fd = new FormData();
		if(formData.connectionFactory && formData.connectionFactory != "")
			fd.append("connectionFactory", formData.connectionFactory);
		else
			fd.append("connectionFactory", $scope.connectionFactories[0]);
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
		if(formData.synchronous && formData.synchronous != "")
			fd.append("synchronous", formData.synchronous);
		if(formData.lookupDestination && formData.lookupDestination != "")
			fd.append("lookupDestination", formData.lookupDestination);

		if(formData.propertyKey && formData.propertyKey != "" && formData.propertyValue && formData.propertyValue != "")
			fd.append("property", formData.propertyKey+","+formData.propertyValue);
		if(formData.message && formData.message != "") {
			var encoding = (formData.encoding && formData.encoding != "") ? ";charset="+formData.encoding : "";
			fd.append("message", new Blob([formData.message], {type: "text/plain"+encoding}), 'message');
		}
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);

		if(!formData.message && !$scope.file) {
			$scope.error = "Please specify a file or message!";
			$scope.processing = false;
			return;
		}

		Api.Post("jms/message", fd, function(returnData) {
			$scope.error = null;
			$scope.processing = false;
		}, function(errorData, status, errorMsg) {
			$scope.processing = false;
			errorMsg = (errorMsg) ? errorMsg : "An unknown error occured, check the logs for more info.";
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

.controller('BrowseJmsQueueCtrl', ['$scope', 'Api', 'Cookies', function($scope, Api, Cookies) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"];
	$scope.form = {};
	Api.Get("jms", function(data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	var browseJmsQueue = Cookies.get("browseJmsQueue");
	if(browseJmsQueue) {
		$scope.form = browseJmsQueue;
	}

	$scope.messages = [];
	$scope.numberOfMessages = -1;
	$scope.processing = false;
	$scope.submit = function(formData) {
		$scope.processing = true;
		if(!formData || !formData.destination) {
			$scope.error = "Please specify a connection factory and destination!";
			return;
		}

		Cookies.set("browseJmsQueue", formData);
		if(!formData.connectionFactory) formData.connectionFactory = $scope.connectionFactories[0] || false;
		if(!formData.type) formData.type = $scope.destinationTypes[0] || false;

		Api.Post("jms/browse", JSON.stringify(formData), function(data) {
			$.extend($scope, data);
			if(!data.messages) {
				$scope.messages = [];
			}
			$scope.error = "";
			$scope.processing = false;
		}, function(errorData, status, errorMsg) {
			$scope.error = (errorData && errorData.error) ? errorData.error : errorMsg;
			$scope.processing = false;
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

		$scope.messages = [];
		$scope.numberOfMessages = -1;
		$scope.processing = false;
	};
}])

.controller('ExecuteJdbcQueryCtrl', ['$scope', 'Api', '$timeout', '$state', 'Cookies', 'appConstants', function($scope, Api, $timeout, $state, Cookies, appConstants) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;
	$scope.form = {};

	$scope.$on('appConstants', function() {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	var executeQueryCookie = Cookies.get("executeQuery");

	Api.Get("jdbc", function(data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
		$scope.form.queryType = data.queryTypes[0];
		$scope.form.resultType = data.resultTypes[0];
		if(executeQueryCookie) {
			$scope.form.query = executeQueryCookie.query;
			if(data.datasources.indexOf(executeQueryCookie.datasource) !== -1){
				$scope.form.datasource = executeQueryCookie.datasource;
			}
			$scope.form.resultType = executeQueryCookie.resultType;
		}

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
		}, function(errorData, status, errorMsg) {
			var error = (errorData && errorData.error) ? errorData.error : "An error occured!";
			$scope.error = error;
			$scope.result = "";
			$scope.processingMessage = false;
		}, false);
	};

	$scope.reset = function() {
		$scope.form.query = "";
		$scope.result = "";
		$scope.form.datasource = $scope.datasources[0];
		$scope.form.resultType = $scope.resultTypes[0];
		$scope.form.avoidLocking=false;
		$scope.form.trimSpaces=false;
		Cookies.remove("executeQuery");
	};
}])

.controller('BrowseJdbcTablesCtrl', ['$scope', 'Api', '$timeout', '$state', 'appConstants', function($scope, Api, $timeout, $state, appConstants) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;
	$scope.form = {};

	$scope.$on('appConstants', function() {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function(data) {
		$scope.datasources = data.datasources;
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
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

		}];
		var columnNameArray = [];
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

.controller('ShowMonitorsCtrl', ['$scope', 'Api', '$state', 'Misc', function($scope, Api, $state, Misc) {

	$scope.selectedConfiguration = null;
	$scope.monitors = [];
	$scope.destinations = [];
	$scope.eventTypes = [];

	$scope.changeConfiguration = function(name) {
		$scope.selectedConfiguration = name;

		if($state.params.configuration == "" || $state.params.configuration != name) { //Update the URL
			$state.transitionTo('pages.monitors', {configuration: name}, { notify: false, reload: false});
		}

		update();
	};

	$scope.totalRaised = 0;
	function update() {
		Api.Get("configurations/"+$scope.selectedConfiguration+"/monitors", function(data) {
			$.extend($scope, data);

			$scope.totalRaised = 0;
			for(i in $scope.monitors) {
				if($scope.monitors[i].raised) $scope.totalRaised++;
				var monitor = $scope.monitors[i];
				monitor.activeDestinations = [];
				for(j in $scope.destinations) {
					var destination = $scope.destinations[j];
					monitor.activeDestinations[destination] = (monitor.destinations.indexOf(destination)>-1);
				}
			}
		});
	}

	//Wait for the 'configurations' field to be populated to change the monitoring page
	$scope.$watch('configurations', function(configs) {
		if(configs) {
			var configName = $state.params.configuration; //See if the configuration query param is populated
			if(!configName) configName = configs[0].name; //Fall back to the first configuration
			$scope.changeConfiguration(configName); //Update the view
		}
	});

	function getUrl(monitor, trigger) {
		var url = "configurations/"+$scope.selectedConfiguration+"/monitors/"+monitor.name;
		if(trigger != undefined && trigger != "") url += "/triggers/"+trigger.id;
		return url;
	}

	$scope.raise = function(monitor) {
		Api.Put(getUrl(monitor), {action: "raise"}, function() {
			update();
		});
	}
	$scope.clear = function(monitor) {
		Api.Put(getUrl(monitor), {action: "clear"}, function() {
			update();
		});
	}
	$scope.edit = function(monitor) {
		var destinations = [];
		for(dest in monitor.activeDestinations) {
			if(monitor.activeDestinations[dest]) {
				destinations.push(dest);
			}
		}
		Api.Put(getUrl(monitor), {action: "edit", name: monitor.displayName, type: monitor.type, destinations: destinations}, function() {
			update();
		});
	}
	$scope.deleteMonitor = function(monitor) {
		Api.Delete(getUrl(monitor), function() {
			update();
		});
	}

	$scope.deleteTrigger = function(monitor, trigger) {
		Api.Delete(getUrl(monitor, trigger), function() {
			update();
		});
	}

	$scope.downloadXML = function(monitorName) {
		var url = Misc.getServerPath() + "iaf/api/configurations/"+$scope.selectedConfiguration+"/monitors";
		if(monitorName) {
			url += "/"+monitorName;
		}
		window.open(url+"?xml=true", "_blank");
	}
}])

.controller('EditMonitorsCtrl', ['$scope', 'Api', '$state', function($scope, Api, $state) {
	$scope.loading = true;

	$scope.$on('loading', function() {
		$scope.loading = false;
	});

	$scope.selectedConfiguration = null;
	$scope.monitor = "";
	$scope.events = "";
	$scope.severities = [];
	$scope.triggerId = "";
	$scope.trigger = {
		type: "Alarm",
		filter: "none",
		events: [],
	}
	var url;
	if($state.params.configuration == "" || $state.params.monitor == "") {
		$state.go('pages.monitors');
	} else {
		$scope.selectedConfiguration = $state.params.configuration;
		$scope.monitor = $state.params.monitor;
		$scope.triggerId = $state.params.trigger || "";
		url = "configurations/"+$scope.selectedConfiguration+"/monitors/"+$scope.monitor+"/triggers/"+$scope.triggerId;
		Api.Get(url, function(data) {
			$.extend($scope, data);
			calculateEventSources();
			if(data.trigger && data.trigger.sources) {
			var sources = data.trigger.sources;
				$scope.trigger.sources = [];
				$scope.trigger.adapters = [];
				for(adapter in sources) {
					if(data.trigger.filter == "source") {
						for(i in sources[adapter]) {
							$scope.trigger.sources.push(adapter+"$$"+sources[adapter][i]);
						}
					} else {
						$scope.trigger.adapters.push(adapter);
					}
				}
			}
		}, function() {
			$state.go('pages.monitors', $state.params);
		});
	}

	$scope.getAdaptersForEvents = function(events) {
		if(!events) return [];

		var adapters = [];
		for(eventName in $scope.events) {
			if(events.indexOf(eventName) > -1) {
				let sourceList = $scope.events[eventName].sources;
				adapters = adapters.concat(Object.keys(sourceList));
			}
		}
		return Array.from(new Set(adapters));
	}
	$scope.eventSources = [];
	function calculateEventSources() {
		for(eventCode in $scope.events) {
			var retVal = [];
			var eventSources = $scope.events[eventCode].sources;
			for(adapter in eventSources) {
				for(i in eventSources[adapter]) {
					retVal.push({adapter:adapter, source: eventSources[adapter][i]});
				}
			}
			$scope.eventSources[eventCode] = retVal;
		}
	}
	$scope.getSourceForEvents = function(events) {
		var retval = [];
		for(i in events) {
			var eventCode = events[i];
			retval = retval.concat($scope.eventSources[eventCode]);
		}
		return retval;
	}

	$scope.submit = function(trigger) {
		if(trigger.filter == "adapter") {
			delete trigger.sources;
		} else if(trigger.filter == "source") {
			delete trigger.adapters;
			var sources = trigger.sources;
			trigger.sources = {};
			for(i in sources) {
				var s = sources[i].split("$$");
				var adapter = s[0];
				var source = s[1];
				if(!trigger.sources[adapter]) trigger.sources[adapter] = [];
				trigger.sources[adapter].push(source);
			}
		}
		if($scope.triggerId && $scope.triggerId > -1) {
			Api.Put(url, trigger, function(returnData) {
				$state.go('pages.monitors', $state.params);
			});
		} else {
			Api.Post(url, JSON.stringify(trigger), function(returnData) {
				$state.go('pages.monitors', $state.params);
			});
		}
	}
}])

.controller('TestPipelineCtrl', ['$scope', 'Api', 'Alert', function($scope, Api, Alert) {
	$scope.state = [];
	$scope.file = null;
	$scope.selectedConfiguration = "";

	$scope.addNote = function(type, message, removeQueue) {
		$scope.state.push({type:type, message: message});
	};

	$scope.processingMessage = false;

	$scope.sessionKeyIndex=1;
	$scope.sessionKeyIndices = [$scope.sessionKeyIndex];
	var sessionKeys = [];

	$scope.updateSessionKeys = function(sessionKey, index) {
		let sessionKeyIndex = sessionKeys.findIndex(f => f.index===index);	// find by index
		if(sessionKeyIndex >= 0) {	
			if(sessionKey.name=="" && sessionKey.value=="") { // remove row if row is empty
				sessionKeys.splice(sessionKeyIndex, 1);
				$scope.sessionKeyIndices.splice(sessionKeyIndex, 1);
			} else { // update existing key value pair
				sessionKeys[sessionKeyIndex].key = sessionKey.name;
				sessionKeys[sessionKeyIndex].value = sessionKey.value;
			}
			$scope.state = [];
		} else if(sessionKey.name && sessionKey.name != "" && sessionKey.value && sessionKey.value != "") {
			let keyIndex = sessionKeys.findIndex(f => f.key===sessionKey.name);	// find by key
			// add new key
			if(keyIndex < 0) {
				$scope.sessionKeyIndex+=1;
				$scope.sessionKeyIndices.push($scope.sessionKeyIndex);
				sessionKeys.push({index:index, key:sessionKey.name, value:sessionKey.value});
				$scope.state = [];
			} else { // key with the same name already exists show warning
				if($scope.state.findIndex(f => f.message === "Session keys cannot have the same name!") < 0) //avoid adding it more than once
					$scope.addNote("warning", "Session keys cannot have the same name!");
			}
		}
		
	}

	$scope.submit = function(formData) {
		$scope.result = "";
		$scope.state = [];
		if(!formData && $scope.selectedConfiguration == "") {
			$scope.addNote("warning", "Please specify a configuration");
			return;
		}

		let fd = new FormData();
		fd.append("configuration", $scope.selectedConfiguration);
		if(formData && formData.adapter && formData.adapter != "") {
			fd.append("adapter", formData.adapter);
		} else {
			$scope.addNote("warning", "Please specify an adapter!");
			return;
		}
		if(formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if(formData.message && formData.message != "") {
			let encoding = (formData.encoding && formData.encoding != "") ? ";charset="+formData.encoding : "";
			fd.append("message", new Blob([formData.message], {type: "text/plain"+encoding}), 'message');
		}
		if($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if(sessionKeys.length > 0){
			let incompleteKeyIndex = sessionKeys.findIndex(f => (f.key==="" || f.value===""));
			if(incompleteKeyIndex < 0) {
				fd.append("sessionKeys", JSON.stringify(sessionKeys));
			} else {
				$scope.addNote("warning", "Please make sure all sessionkeys have name and value!");
				return;
			}
		}

		$scope.processingMessage = true;
		Api.Post("test-pipeline", fd, function(returnData) {
			var warnLevel = "success";
			if(returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
			if($scope.file != null) {
				angular.element(".form-file")[0].value = null;
				$scope.file = null;
				formData.message = returnData.message;
			}
		}, function(errorData) {
			let error = (errorData && errorData.error) ? errorData.error : "An error occured!";
			$scope.result = "";
			$scope.addNote("warning", error);
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
	$scope.processingMessage = false;

	Api.Get("test-servicelistener", function(data) {
		$scope.services = data.services;
	});

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
