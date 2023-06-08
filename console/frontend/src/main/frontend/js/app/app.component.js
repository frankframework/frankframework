import { appModule } from "./app.module";

const AppController = function ($scope, $rootScope, authService, appConstants, Api, Hooks, $state, $location, Poller, Notification, dateFilter, $interval, Idle, $http, Misc, $uibModal, Session, Debug, SweetAlert, $timeout) {
	const ctrl = this;

	ctrl.$state = $state;

	ctrl.loading = true;
	ctrl.serverInfo = {};

	ctrl.loggedin = false;

	ctrl.$onInit = function () {
		/* state controller */
		authService.loggedin(); //Check if the user is logged in.
		ctrl.monitoring = false;
		ctrl.config_database = false;

		angular.element(".main").show();
		angular.element(".loading").remove();
		/* state controller end */

		$rootScope.adapters = {};
		$rootScope.alerts = [];

		$rootScope.adapterSummary = {
			started: 0,
			stopped: 0,
			starting: 0,
			stopping: 0,
			error: 0
		};
		$rootScope.receiverSummary = {
			started: 0,
			stopped: 0,
			starting: 0,
			stopping: 0,
			error: 0
		};
		$rootScope.messageSummary = {
			info: 0,
			warn: 0,
			error: 0
		};

		$rootScope.lastUpdated = 0;
		$rootScope.timeout = null;

		$rootScope.configurations = [];

		Pace.on("done", ctrl.initializeFrankConsole);
		$scope.$on('initializeFrankConsole', ctrl.initializeFrankConsole);
		$timeout(ctrl.initializeFrankConsole, 250);

		$scope.$on('IdleStart', function () {
			Poller.getAll().changeInterval(appConstants["console.idle.pollerInterval"]);

			var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
			if (!idleTimeout) return;

			SweetAlert.Warning({
				title: "Idle timer...",
				text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
				showConfirmButton: false,
				showCloseButton: true
			});
		});

		$scope.$on('IdleWarn', function (e, time) {
			var minutes = Math.floor(time / 60);
			var seconds = Math.round(time % 60);
			if (minutes < 10) minutes = "0" + minutes;
			if (seconds < 10) seconds = "0" + seconds;
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

		Hooks.register("init:once", function () {
			/* Check IAF version */
			console.log("Checking IAF version with remote...");
			$http.get("https://ibissource.org/iaf/releases/?q=" + Misc.getUID(ctrl.serverInfo)).then(function (response) {
				if (!response || !response.data) return false;
				var release = response.data[0]; //Not sure what ID to pick, smallest or latest?

				var newVersion = (release.tag_name.substr(0, 1) == "v") ? release.tag_name.substr(1) : release.tag_name;
				var currentVersion = appConstants["application.version"];
				var version = Misc.compare_version(newVersion, currentVersion);
				console.log("Comparing version: '" + currentVersion + "' with latest release: '" + newVersion + "'.");
				Session.remove("IAF-Release");

				if (version > 0) {
					Session.set("IAF-Release", release);
					Notification.add('fa-exclamation-circle', "IAF update available!", false, function () {
						$location.path("iaf-update");
					});
				}
				ctrl.serverInfo = null;
			}).catch(function (error) {
				Debug.error("An error occured while comparing IAF versions", error);
				ctrl.serverInfo = null;
			});

			Poller.add("server/warnings", function (configurations) {
				$rootScope.alerts = []; //Clear all old alerts

				configurations['All'] = { messages: configurations.messages };
				delete configurations.messages;

				configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
				delete configurations.totalErrorStoreCount;

				for (let x in configurations.warnings) {
					ctrl.addWarning('', configurations.warnings[x]);
				}

				for (const i in configurations) {
					var configuration = configurations[i];
					if (configuration.exception)
						ctrl.addException(i, configuration.exception);
					if (configuration.warnings) {
						for (const x in configuration.warnings) {
							ctrl.addWarning(i, configuration.warnings[x]);
						}
					}

					configuration.messageLevel = "INFO";
					for (const x in configuration.messages) {
						var level = configuration.messages[x].level;
						if (level == "WARN" && configuration.messageLevel != "ERROR")
							configuration.messageLevel = "WARN";
						if (level == "ERROR")
							configuration.messageLevel = "ERROR";
					}
				}

				$rootScope.messageLog = configurations;
			}, true, 60000);

			var raw_adapter_data = {};
			var pollerCallback = function (allAdapters) {
				for (const adapterName in raw_adapter_data) { //Check if any old adapters should be removed
					if (!allAdapters[adapterName]) {
						delete raw_adapter_data[adapterName];
						delete $rootScope.adapters[adapterName];
						Debug.log("removed adapter [" + adapterName + "]");
					}
				}
				for (const adapterName in allAdapters) { //Add new adapter information
					var adapter = allAdapters[adapterName];

					if (raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
						raw_adapter_data[adapter.name] = JSON.stringify(adapter);

						adapter.status = "started";

						for (const x in adapter.receivers) {
							var adapterReceiver = adapter.receivers[x];
							if (adapterReceiver.state != 'started')
								adapter.status = 'warning';

							if (adapterReceiver.transactionalStores) {
								let store = adapterReceiver.transactionalStores["ERROR"];
								if (store && store.numberOfMessages > 0) {
									adapter.status = 'warning';
								}
							}
						}
						if (adapter.receiverReachedMaxExceptions) {
							adapter.status = 'warning';
						}
						adapter.hasSender = false;
						adapter.sendersMessageLogCount = 0;
						adapter.senderTransactionalStorageMessageCount = 0;
						for (const x in adapter.pipes) {
							let pipe = adapter.pipes[x];
							if (pipe.sender) {
								adapter.hasSender = true;
								if (pipe.hasMessageLog) {
									let count = parseInt(pipe.messageLogCount);
									if (!Number.isNaN(count)) {
										if (pipe.isSenderTransactionalStorage) {
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
						if (adapter.state != "started") {
							adapter.status = "stopped";
						}

						$rootScope.adapters[adapter.name] = adapter;

						$rootScope.updateAdapterSummary();
						Hooks.call("adapterUpdated", adapter);
						//					$scope.$broadcast('adapterUpdated', adapter);
					}
				}
			};

			//Get base information first, then update it with more details
			Api.Get("adapters", pollerCallback);
			$timeout(function () {
				Poller.add("adapters?expanded=all", pollerCallback, true);
				$scope.$broadcast('loading', false);
			}, 3000);
		});

		Hooks.register("adapterUpdated:once", function () {
			if ($location.path() == "/status" && $location.hash()) {
				var el = angular.element("#" + $location.hash());
				if (el && el[0]) {
					el[0].scrollIntoView();
				}
			}
		});

		Hooks.register("adapterUpdated", function (adapter) {
			var name = adapter.name;
			if (name.length > 20)
				name = name.substring(0, 17) + "...";
			if (adapter.started == true) {
				for (const x in adapter.receivers) {
					if (adapter.receivers[x].started == false) {
						Notification.add('fa-exclamation-circle', "Receiver '" + name + "' stopped!", false, function () {
							$location.path("status");
							$location.hash(adapter.name);
						});
					}
				}
			}
			else {
				Notification.add('fa-exclamation-circle', "Adapter '" + name + "' stopped!", false, function () {
					$location.path("status");
					$location.hash(adapter.name);
				});
			}
		});
	}

	ctrl.initializeFrankConsole = function() {
		if (appConstants.init === -1) {
			appConstants.init = 0;
			Debug.log("Initializing Frank!Console");
		} else if (appConstants.init === 0) {
			Debug.log("Cancelling 2nd initialization attempt");
			Pace.stop();
			return;
		} else {
			Debug.info("Loading Frank!Console", appConstants.init);
		}

		if (appConstants.init === 0) { //Only continue if the init state was -1
			appConstants.init = 1;
			Api.Get("server/info", function (data) {
				ctrl.serverInfo = data;

				appConstants.init = 2;
				if (!($location.path().indexOf("login") >= 0)) {
					Idle.watch();
					angular.element("body").removeClass("gray-bg");
					angular.element(".main").show();
					angular.element(".loading").hide();
				}

				var serverTime = Date.parse(new Date(data.serverTime).toUTCString());
				var localTime = Date.parse(new Date().toUTCString());
				appConstants.timeOffset = serverTime - localTime;

				function updateTime() {
					var serverDate = new Date();
					serverDate.setTime(serverDate.getTime() - appConstants.timeOffset);
					ctrl.serverTime = dateFilter(serverDate, appConstants["console.dateFormat"]);
				}
				$interval(updateTime, 1000);
				updateTime();

				$rootScope.instanceName = data.instance.name;
				angular.element(".iaf-info").html(data.framework.name + " " + data.framework.version + ": " + data.instance.name + " " + data.instance.version);

				$rootScope.dtapStage = data["dtap.stage"];
				ctrl.dtapStage = data["dtap.stage"];
				ctrl.dtapSide = data["dtap.side"];
				// $rootScope.userName = data["userName"];
				ctrl.userName = data["userName"];

				if ($rootScope.dtapStage == "LOC") {
					Debug.setLevel(3);
				}

				//Was it able to retrieve the serverinfo without logging in?
				if (!ctrl.loggedin) {
					Idle.setTimeout(false);
				}

				Api.Get("server/configurations", function (data) {
					ctrl.updateConfigurations(data);
				});
				Hooks.call("init", false);
			}, function (message, statusCode, statusText) {
				if (statusCode == 500) {
					$state.go("pages.errorpage");
				}
			});
			Api.Get("environmentvariables", function (data) {
				if (data["Application Constants"]) {
					appConstants = $.extend(appConstants, data["Application Constants"]["All"]); //make FF!Application Constants default

					var idleTime = (parseInt(appConstants["console.idle.time"]) > 0) ? parseInt(appConstants["console.idle.time"]) : false;
					if (idleTime > 0) {
						var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
						Idle.setIdle(idleTime);
						Idle.setTimeout(idleTimeout);
					}
					else {
						Idle.unwatch();
					}
					ctrl.databaseSchedulesEnabled = (appConstants["loadDatabaseSchedules.active"] === 'true');
					$rootScope.$broadcast('appConstants');
				}
			});
		}

		var token = sessionStorage.getItem('authToken');
		ctrl.loggedin = (token != null && token != "null") ? true : false;
	}

	ctrl.reloadRoute = function () {
		$state.reload();
	};

	ctrl.addAlert = function (type, configuration, message) {
		var line = message.match(/line \[(\d+)\]/);
		var isValidationAlert = message.indexOf("Validation") !== -1;
		var link = (line && !isValidationAlert) ? { name: configuration, '#': 'L' + line[1] } : undefined;
		$rootScope.alerts.push({
			link: link,
			type: type,
			configuration: configuration,
			message: message
		});
	};
	ctrl.addWarning = function (configuration, message) {
		ctrl.addAlert("warning", configuration, message);
	};
	ctrl.addException = function (configuration, message) {
		ctrl.addAlert("danger", configuration, message);
	};

	ctrl.updateConfigurations = function (configurations) {
		const updatedConfigurations = [];
		for (var i in configurations) {
			var config = configurations[i];
			if (config.name.startsWith("IAF_"))
				updatedConfigurations.unshift(config);
			else
				updatedConfigurations.push(config);
		}
		$rootScope.$broadcast('configurations', updatedConfigurations);
		$rootScope.configurations = updatedConfigurations;
	}

	ctrl.getProcessStateIcon = function (processState) {
		switch (processState) {
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
	$rootScope.getProcessStateIconColor = function (processState) {
		switch (processState) {
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

	$rootScope.updateAdapterSummary = function (configurationName) {
		var updated = (new Date().getTime());
		if (updated - 3000 < $rootScope.lastUpdated && !configurationName) { //3 seconds
			clearTimeout($rootScope.timeout);
			$rootScope.timeout = setTimeout($rootScope.updateAdapterSummary, 1000);
			return;
		}
		if (configurationName == undefined)
			configurationName = $state.params.configuration;

		var adapterSummary = {
			started: 0,
			stopped: 0,
			starting: 0,
			stopping: 0,
			exception_starting: 0,
			exception_stopping: 0,
			error: 0
		};
		var receiverSummary = {
			started: 0,
			stopped: 0,
			starting: 0,
			stopping: 0,
			exception_starting: 0,
			exception_stopping: 0,
			error: 0
		};
		var messageSummary = {
			info: 0,
			warn: 0,
			error: 0
		};

		var allAdapters = $rootScope.adapters;
		for (const adapterName in allAdapters) {
			var adapter = allAdapters[adapterName];

			if (adapter.configuration == configurationName || configurationName == 'All') { // Only adapters for active config
				adapterSummary[adapter.state]++;
				for (const i in adapter.receivers) {
					receiverSummary[adapter.receivers[i].state.toLowerCase()]++;
				}
				for (const i in adapter.messages) {
					var level = adapter.messages[i].level.toLowerCase();
					messageSummary[level]++;
				}
			}
		}

		$rootScope.adapterSummary = adapterSummary;
		$rootScope.receiverSummary = receiverSummary;
		$rootScope.messageSummary = messageSummary;
		$rootScope.lastUpdated = updated;
	};

	ctrl.openInfoModel = function () {
		$uibModal.open({
			templateUrl: 'js/app/components/pages/information-modal/information.html',
			//            size: 'sm',
			controller: 'InformationCtrl',
		});
	};

	ctrl.sendFeedback = function (rating) {
		console.log("sendFeedback rating", rating);
		if (!appConstants["console.feedbackURL"])
			return;

		$(".rating i").each(function (i, e) {
			$(e).addClass("fa-star-o").removeClass("fa-star");
		});
		$uibModal.open({
			templateUrl: 'js/app/components/pages/feedback-modal/feedback.html',
			controller: 'FeedbackCtrl',
			resolve: { rating: function () { return rating; } },
		});
	};
}

appModule.component('app', {
	controller: ['$scope', '$rootScope', 'authService', 'appConstants', 'Api', 'Hooks', '$state', '$location', 'Poller', 'Notification', 'dateFilter', '$interval', 'Idle', '$http', 'Misc', '$uibModal', 'Session', 'Debug', 'SweetAlert', '$timeout', AppController],
	templateUrl: 'js/app/app.component.html'
});
