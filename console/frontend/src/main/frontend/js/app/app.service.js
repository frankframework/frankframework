import { appModule } from "./app.module";

appModule.factory('appService', ['$rootScope', '$state', function ($rootScope, $state){
	const service = {};

	service.adapters = {};
	service.updateAdapters = function (adapters) {
		service.adapters = adapters;
		$rootScope.$broadcast('adapters', adapters);
	}

	service.alerts = [];
	service.updateAlerts = function(alerts){
		service.alerts = alerts;
		$rootScope.$broadcast('alerts', alerts);
	}

	service.adapterSummary = {
		started: 0,
		stopped: 0,
		starting: 0,
		stopping: 0,
		error: 0
	};
	service.receiverSummary = {
		started: 0,
		stopped: 0,
		starting: 0,
		stopping: 0,
		error: 0
	};
	service.messageSummary = {
		info: 0,
		warn: 0,
		error: 0
	};

	service.lastUpdated = 0;
	service.timeout = null;

	service.configurations = [];

	service.messageLog = [];
	service.updateMessageLog = function (messageLog) {
		service.messageLog = messageLog;
		$rootScope.$broadcast('messageLog', messageLog);
	}

	service.instanceName = "";
	service.updateInstanceName = function (instanceName) {
		service.instanceName = instanceName;
		$rootScope.$broadcast('instanceName', instanceName);
	}

	service.dtapStage = "";

	service.updateConfigurations = function (configurations) {
		const updatedConfigurations = [];
		for (var i in configurations) {
			var config = configurations[i];
			if (config.name.startsWith("IAF_"))
				updatedConfigurations.unshift(config);
			else
				updatedConfigurations.push(config);
		}
		service.configurations = updatedConfigurations;
		$rootScope.$broadcast('configurations', updatedConfigurations);
	}

	service.getProcessStateIconColor = function (processState) {
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

	service.updateAdapterSummary = function (configurationName) {
		var updated = (new Date().getTime());
		if (updated - 3000 < service.lastUpdated && !configurationName) { //3 seconds
			clearTimeout(service.timeout);
			service.timeout = setTimeout(service.updateAdapterSummary, 1000);
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

		var allAdapters = service.adapters;
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

		service.adapterSummary = adapterSummary;
		service.receiverSummary = receiverSummary;
		service.messageSummary = messageSummary;
		service.lastUpdated = updated;
		$rootScope.$broadcast('summaries');
	};

	return service;
}]);
