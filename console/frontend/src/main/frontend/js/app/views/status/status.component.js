import { appModule } from "../../app.module";

const StatusController = function ($scope, $rootScope, Api, Poller, $filter, $state, Misc, $anchorScroll, $location, $http, appService) {
	const ctrl = this;

	ctrl.filter = {
		"started": true,
		"stopped": true,
		"warning": true
	};

	ctrl.adapters = {};

	ctrl.searchText = "";

	ctrl.selectedConfiguration = "All";

	ctrl.reloading = false;

	ctrl.configurationFlowDiagram = null;

	ctrl.isConfigStubbed = {};
	ctrl.isConfigReloading = {};

	ctrl.$onInit = function () {
		var hash = $location.hash();
		ctrl.adapterName = $state.params.adapter;
		if (ctrl.adapterName == "" && hash != "") { //If the adapter param hasn't explicitly been set
			ctrl.adapterName = hash;
		} else {
			$location.hash(ctrl.adapterName);
		}

		if ($state.params.filter != "") {
			var filter = $state.params.filter.split("+");
			for (const f in ctrl.filter) {
				ctrl.filter[f] = (filter.indexOf(f) > -1);
			}
		}
		if ($state.params.search != "") {
			ctrl.searchText = $state.params.search;
		}

		$scope.$on('appConstants', function () {
			ctrl.updateConfigurationFlowDiagram(ctrl.selectedConfiguration);
		});

		ctrl.check4StubbedConfigs();
		ctrl.adapterSummary = appService.adapterSummary;
		ctrl.receiverSummary = appService.receiverSummary;
		ctrl.messageSummary = appService.messageSummary;
		ctrl.alerts = appService.alerts;
		ctrl.messageLog = appService.messageLog;
		ctrl.adapters = appService.adapters;
		$rootScope.$on('configurations', ctrl.check4StubbedConfigs);
		$rootScope.$on('summaries', function () {
			ctrl.adapterSummary = appService.adapterSummary;
			ctrl.receiverSummary = appService.receiverSummary;
			ctrl.messageSummary = appService.messageSummary;
		});
		$rootScope.$on('alerts', function () { ctrl.alerts = appService.alerts; }, true);
		$rootScope.$on('messageLog', function () { ctrl.messageLog = appService.messageLog; });
		$rootScope.$on('adapters', function () { ctrl.adapters = appService.adapters; });

		ctrl.getProcessStateIconColor = appService.getProcessStateIconColor;

		if ($state.params.configuration != "All")
			ctrl.changeConfiguration($state.params.configuration);
	};

	ctrl.showContent = function (adapter) {
		if (adapter.status == "stopped") {
			return true;
		} else if (ctrl.adapterName != "" && adapter.name == ctrl.adapterName) {
			$anchorScroll();
			return true;
		} else {
			return false;
		}
	};

	ctrl.applyFilter = function (filter) {
		ctrl.filter = filter;
		ctrl.updateQueryParams();
	};

	ctrl.updateQueryParams = function () {
		var filterStr = [];
		for (const f in ctrl.filter) {
			if (ctrl.filter[f])
				filterStr.push(f);
		}
		var transitionObj = {};
		transitionObj.filter = filterStr.join("+");
		if (ctrl.selectedConfiguration != "All")
			transitionObj.configuration = ctrl.selectedConfiguration;
		if (ctrl.searchText.length > 0)
			transitionObj.search = ctrl.searchText;

		$state.transitionTo('pages.status', transitionObj, { notify: false, reload: false });
	};

	ctrl.collapseAll = function () {
		$(".adapters").each(function (i, e) {
			var a = $(e).find("div.ibox-title");
			angular.element(a).scope().showContent = false;
		});
	};
	ctrl.expandAll = function () {
		$(".adapters").each(function (i, e) {
			setTimeout(function () {
				var a = $(e).find("div.ibox-title");
				angular.element(a).scope().showContent = true;
			}, i * 10);
		});
	};
	ctrl.stopAll = function () {
		let compiledAdapterList = Array();
		// let adapters = $filter('configurationFilter')($scope.adapters, $scope);
		let adapters = $filter('configurationFilter')(ctrl.adapters, ctrl);
		for (const adapter in adapters) {
			let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration + "/" + adapter);
		}
		Api.Put("adapters", { "action": "stop", "adapters": compiledAdapterList });
	};
	ctrl.startAll = function () {
		let compiledAdapterList = Array();
		// let adapters = $filter('configurationFilter')($scope.adapters, $scope);
		let adapters = $filter('configurationFilter')(ctrl.adapters, ctrl);
		for (const adapter in adapters) {
			let configuration = adapters[adapter].configuration;
			compiledAdapterList.push(configuration + "/" + adapter);
		}
		Api.Put("adapters", { "action": "start", "adapters": compiledAdapterList });
	};
	ctrl.reloadConfiguration = function () {
		if (ctrl.selectedConfiguration == "All") return;

		ctrl.isConfigReloading[ctrl.selectedConfiguration] = true;

		Poller.getAll().stop();
		Api.Put("configurations/" + ctrl.selectedConfiguration, { "action": "reload" }, function () {
			ctrl.startPollingForConfigurationStateChanges(function () {
				Poller.getAll().start();
			});
		});
	};
	ctrl.fullReload = function () {
		ctrl.reloading = true;
		Poller.getAll().stop();
		Api.Put("configurations", { "action": "reload" }, function () {
			ctrl.reloading = false;
			ctrl.startPollingForConfigurationStateChanges(function () {
				Poller.getAll().start();
			});
		});
	};

	ctrl.startPollingForConfigurationStateChanges = function(callback) {
		Poller.add("server/configurations", function (configurations) {
			appService.updateConfigurations(configurations);

			var ready = true;
			for (var i in configurations) {
				var config = configurations[i];
				//When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
				if (config.state != "STARTED" && !(config.state == "STOPPED" && config.exception != null)) {
					ready = false;
					break;
				}
			}
			if (ready) { //Remove poller once all states are STARTED
				Poller.remove("server/configurations");
				if (callback != null && typeof callback == "function") callback();
			}
		}, true);
	}

	ctrl.showReferences = function () {
		window.open(ctrl.configurationFlowDiagram);
	};
	ctrl.updateConfigurationFlowDiagram = function (configurationName) {
		var url = Misc.getServerPath() + 'iaf/api/configurations/';
		if (configurationName == "All") {
			url += "?flow=true";
		} else {
			url += configurationName + "/flow";
		}
		$http.get(url).then(function (data) {
			let status = (data && data.status) ? data.status : 204;
			if (status == 200) {
				ctrl.configurationFlowDiagram = url;
			}
		});
	}

	ctrl.check4StubbedConfigs = function () {
		ctrl.configurations = appService.configurations;
		for (var i in appService.configurations) {
			var config = appService.configurations[i];
			ctrl.isConfigStubbed[config.name] = config.stubbed;
			ctrl.isConfigReloading[config.name] = config.state == "STARTING" || config.state == "STOPPING"; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
		}
	};

	ctrl.closeAlert = function (index) {
		$rootScope.alerts.splice(index, 1);
		appService.updateAlerts(appService.alerts);
	};

	ctrl.changeConfiguration = function (name) {
		ctrl.selectedConfiguration = name;
		appService.updateAdapterSummary(name);
		ctrl.updateQueryParams();
		ctrl.updateConfigurationFlowDiagram(name);
	};

	ctrl.startAdapter = function (adapter) {
		adapter.state = 'starting';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name), { "action": "start" });
	};
	ctrl.stopAdapter = function (adapter) {
		adapter.state = 'stopping';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name), { "action": "stop" });
	};
	ctrl.startReceiver = function (adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "start" });
	};
	ctrl.stopReceiver = function (adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "stop" });
	};
	ctrl.addThread = function (adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "incthread" });
	};
	ctrl.removeThread = function (adapter, receiver) {
		receiver.state = 'loading';
		Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "decthread" });
	};

};

appModule.component('status', {
	controller: ['$scope', '$rootScope', 'Api', 'Poller', '$filter', '$state', 'Misc', '$anchorScroll', '$location', '$http', 'appService', StatusController],
	templateUrl: 'js/app/views/status/status.component.html',
});
