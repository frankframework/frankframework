import Pace from 'pace-js'
import './components/logout/logout.controller';
import './components/pages/topinfobar/infobar.controller';
import './views/adapterstatistics/adapterstatistics.controller';
import './views/configuration/configuration.controller';
import './views/configuration/configurations-upload/configurations-upload.controller';
import './views/configuration/configurations-manage/configurations-manage.controller';
import './views/configuration/configurations-manage/configurations-manage-details/configurations-manage-details.controller';
import './views/connections/connections.controller';
import './views/environment-variables/environment-variables.controller';
import './views/inlinestore/inlinestore.controller';
import './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.controller';
import './views/jdbc/jdbc-execute-query/jdbc-execute-query.controller';
import './views/jms/jms-browse-queue/jms-browse-queue.controller';
import './views/jms/jms-send-message/jms-send-message.controller';
import './views/logging/logging.controller';
import './views/logging/logging-manage/logging-manage.controller';
import './views/login/login.controller';
import './views/notifications/notifications.controller';
import './views/scheduler/scheduler.controller';
import './views/scheduler/scheduler-add/scheduler-add.controller';
import './views/scheduler/scheduler-edit/scheduler-edit.controller';
import './views/security-items/security-items.controller';
import './views/status/status.controller';
import './views/storage/storage.controller';
import './views/storage/storage-list/storage-list.controller';
import './views/storage/storage-view/storage-view.controller';
import './views/test-pipeline/test-pipeline.controller';
import './views/test-service-listener/test-service-listener.controller';
import './views/webservices/webservices.controller';

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

			for(const i in configurations) {
				var configuration = configurations[i];
				if(configuration.exception)
					$scope.addException(i, configuration.exception);
				if(configuration.warnings) {
					for(const x in configuration.warnings) {
						$scope.addWarning(i, configuration.warnings[x]);
					}
				}

				configuration.messageLevel = "INFO";
				for(const x in configuration.messages) {
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
			for(const adapterName in raw_adapter_data) { //Check if any old adapters should be removed
				if(!allAdapters[adapterName]) {
					delete raw_adapter_data[adapterName];
					delete $rootScope.adapters[adapterName];
					Debug.log("removed adapter ["+adapterName+"]");
				}
			}
			for(const adapterName in allAdapters) { //Add new adapter information
				var adapter = allAdapters[adapterName];

				if(raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
					raw_adapter_data[adapter.name] = JSON.stringify(adapter);

					adapter.status = "started";

					for(const x in adapter.receivers) {
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
					for(const x in adapter.pipes) {
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
		for(const adapterName in allAdapters) {
			var adapter = allAdapters[adapterName];

			if(adapter.configuration == configurationName || configurationName == 'All') { // Only adapters for active config
				adapterSummary[adapter.state]++;
				for(const i in adapter.receivers) {
					receiverSummary[adapter.receivers[i].state.toLowerCase()]++;
				}
				for(const i in adapter.messages) {
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
			for(const x in adapter.receivers) {
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
		for(const adapterName in adapters) {
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
		for(const adapterName in adapters) {
			var adapter = adapters[adapterName];

			if(JSON.stringify(adapter).replace(/"/g, '').toLowerCase().indexOf(searchText) > -1)
				r[adapterName] = adapter;
		}
		return r;
	};
})

.filter('variablesFilter', [function() {
	return function(variables, filterText) {
		var returnArray = new Array();

		filterText = filterText.toLowerCase();
		for(const i in variables) {
			var variable = variables[i];
			if(JSON.stringify(variable).toLowerCase().indexOf(filterText) > -1) {
				returnArray.push(variable);
			}
		}

		return returnArray;
	};
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
			for(const i in $scope.monitors) {
				if($scope.monitors[i].raised) $scope.totalRaised++;
				var monitor = $scope.monitors[i];
				monitor.activeDestinations = [];
				for(const j in $scope.destinations) {
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
		for(const dest in monitor.activeDestinations) {
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
				for(const adapter in sources) {
					if(data.trigger.filter == "SOURCE") {
						for(const i in sources[adapter]) {
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
		for(const eventName in $scope.events) {
			if(events.indexOf(eventName) > -1) {
				let sourceList = $scope.events[eventName].sources;
				adapters = adapters.concat(Object.keys(sourceList));
			}
		}
		return Array.from(new Set(adapters));
	}
	$scope.eventSources = [];
	function calculateEventSources() {
		for(const eventCode in $scope.events) {
			var retVal = [];
			var eventSources = $scope.events[eventCode].sources;
			for(const adapter in eventSources) {
				for(const i in eventSources[adapter]) {
					retVal.push({adapter:adapter, source: eventSources[adapter][i]});
				}
			}
			$scope.eventSources[eventCode] = retVal;
		}
	}
	$scope.getSourceForEvents = function(events) {
		var retval = [];
		for(const i in events) {
			var eventCode = events[i];
			retval = retval.concat($scope.eventSources[eventCode]);
		}
		return retval;
	}

	$scope.submit = function(trigger) {
		if(trigger.filter == "ADAPTER") {
			delete trigger.sources;
		} else if(trigger.filter == "SOURCE") {
			delete trigger.adapters;
			var sources = trigger.sources;
			trigger.sources = {};
			for(const i in sources) {
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
}]);
