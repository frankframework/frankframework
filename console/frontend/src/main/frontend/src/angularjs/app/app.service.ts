import { StateService } from "angular-ui-router";
import { appModule } from "./app.module";

export type RunState = 'ERROR' | 'STARTING' | 'EXCEPTION_STARTING' | 'STARTED' | 'STOPPING' | 'EXCEPTION_STOPPING' | 'STOPPED';

export type Adapter = {
  "configuration": string,
  "upSince": number,
  "name": string,
  "description": null | string,
  "started": boolean,
  "state": RunState,
  "status"?: 'started' | 'warning' | 'stopped',
  "hasSender": boolean,
  "sendersMessageLogCount": number,
  "senderTransactionalStorageMessageCount": number,
  receivers: any[],
  messages: any[]
}

export type Configuration = {
  name: string,
  stubbed: boolean,
  state: RunState,
  type: 'DatabaseClassLoader' | 'DirectoryClassLoader' | 'DummyClassLoader' | 'JarFileClassLoader' | 'ScanningDirectoryClassLoader' | 'WebAppClassLoader',
  dbcMigrator: boolean
}

export class Service {
  constructor(
    private $rootScope: angular.IRootScopeService,
    private $state: StateService
  ){}

  private lastUpdated = 0;
  private timeout?: number;

  adapters: Record<string, Adapter> = {};
	updateAdapters(adapters: Record<string, Adapter>) {
    this.adapters = adapters;
    this.$rootScope.$broadcast('adapters', adapters);
  }

	alerts = [];
	updateAlerts(alerts) {
    this.alerts = alerts;
    this.$rootScope.$broadcast('alerts', alerts);
  }

	startupError = null;
  updateStartupError(startupError) {
    this.startupError = startupError;
    this.$rootScope.$broadcast('startupError', startupError);
  }

	adapterSummary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
	receiverSummary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
	messageSummary = {
    info: 0,
    warn: 0,
    error: 0
  };

  configurations: Configuration[] = [];
  updateConfigurations(configurations: Configuration[]) {
    const updatedConfigurations: Configuration[] = [];
    for (var i in configurations) {
      var config = configurations[i];
      if (config.name.startsWith("IAF_"))
        updatedConfigurations.unshift(config);
      else
        updatedConfigurations.push(config);
    }
    this.configurations = updatedConfigurations;
    this.$rootScope.$broadcast('configurations', updatedConfigurations);
  }

	messageLog = [];
	updateMessageLog(messageLog) {
    this.messageLog = messageLog;
    this.$rootScope.$broadcast('messageLog', messageLog);
  }

	instanceName = "";
	updateInstanceName(instanceName: string) {
    this.instanceName = instanceName;
    this.$rootScope.$broadcast('instanceName', instanceName);
  }

	dtapStage = "";
  updateDtapStage(dtapStage: string) {
    this.dtapStage = dtapStage;
    this.$rootScope.$broadcast('dtapStage', dtapStage);
  }

	databaseSchedulesEnabled = false;
	updateDatabaseSchedulesEnabled(databaseSchedulesEnabled) {
    this.databaseSchedulesEnabled = databaseSchedulesEnabled;
    this.$rootScope.$broadcast('databaseSchedulesEnabled', databaseSchedulesEnabled);
  }

	getProcessStateIcon(processState: string) {
    switch (processState) {
      case "Available":
        return "fa-server";
      case "InProcess":
        return "fa-gears";
      case "Done":
        return "fa-sign-in";
      case "Hold":
        return "fa-pause-circle";
      case "Error":
      default:
        return "fa-times-circle";
    }
  };

	getProcessStateIconColor(processState: string) {
    switch (processState) {
      case "Available":
        return "success";
      case "InProcess":
        return "success";
      case "Done":
        return "success";
      case "Hold":
        return "warning";
      case "Error":
      default:
        return "danger";
    }
  };

	updateAdapterSummary(configurationName: string) {
    var updated = (new Date().getTime());
    if (updated - 3000 < this.lastUpdated && !configurationName) { //3 seconds
      clearTimeout(this.timeout);
      this.timeout = setTimeout(this.updateAdapterSummary, 1000);
      return;
    }
    if (configurationName == undefined)
      configurationName = this.$state.params["configuration"];

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

    var allAdapters = this.adapters;
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

    this.adapterSummary = adapterSummary;
    this.receiverSummary = receiverSummary;
    this.messageSummary = messageSummary;
    this.lastUpdated = updated;
    this.$rootScope.$broadcast('summaries');
  };
}

appModule.factory('appService', ['$rootScope', '$state', function ($rootScope: angular.IRootScopeService, $state: StateService) {
  const service = new Service($rootScope, $state);


	return service;
}]);
