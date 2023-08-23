import { StateService } from "@uirouter/angularjs";
import { appModule } from "./app.module";

export type RunState = 'ERROR' | 'STARTING' | 'EXCEPTION_STARTING' | 'STARTED' | 'STOPPING' | 'EXCEPTION_STOPPING' | 'STOPPED';
export type RunStateRuntime = RunState | 'loading'
export type MessageLevel = 'INFO' | 'WARN' | 'ERROR';

export type Receiver = {
  isEsbJmsFFListener: boolean,
  name: string,
  transactionalStores: Record<'DONE' | 'ERROR', { name: string, numberOfMessages: number }>,
  listener: {
    name: string,
    destination: string,
    class: string,
    isRestListener: boolean
  },
  messages: {
    retried: number,
    rejected: number,
    received: number
  },
  state: Lowercase<RunStateRuntime>,
}

type Message = {
  date: number,
  level: MessageLevel,
  message: string,
}

export interface AdapterMessage extends Message {
  capacity: number
}

export type Pipe = {
  forwards: Record<'success' | 'exception', string>,
  sender: string,
  name: string,
  destination: string,
  isJdbcSender: boolean,
  hasMessageLog?: boolean,
  messageLogCount?: 'error' | string,
  isSenderTransactionalStorage?: boolean
}

export type Adapter = {
  configuration: string,
  configured: boolean,
  upSince: number,
  name: string,
  description: null | string,
  started: boolean,
  state: Lowercase<RunState>,
  receivers?: Receiver[],
  messages?: AdapterMessage[],
  pipes?: Pipe[],
  hasSender?: boolean,
  status?: 'started' | 'warning' | 'stopped',
  sendersMessageLogCount?: number,
  senderTransactionalStorageMessageCount?: number,
  receiverReachedMaxExceptions?: boolean
}

export type Configuration = {
  name: string,
  stubbed: boolean,
  state: RunState,
  type: 'DatabaseClassLoader' | 'DirectoryClassLoader' | 'DummyClassLoader' | 'JarFileClassLoader' | 'ScanningDirectoryClassLoader' | 'WebAppClassLoader',
  dbcMigrator: boolean
}

export type Alert = {
  link?: { name: string, '#': string },
  type: string,
  configuration: string,
  message: string
}

export type MessageLog = {
  errorStoreCount: number,
  messages: Message[],
  messageLevel: MessageLevel,
}

export type Summary = {
  started: number,
  stopped: number,
  starting: number,
  stopping: number,
  error: number
}

export type MessageSummary = {
  info: number,
  warn: number,
  error: number
}

export class AppService {
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

  alerts: Alert[] = [];
	updateAlerts(alerts: Alert[]) {
    this.alerts = alerts;
    this.$rootScope.$broadcast('alerts', alerts);
  }

	startupError: string | null = null;
  updateStartupError(startupError: string) {
    this.startupError = startupError;
    this.$rootScope.$broadcast('startupError', startupError);
  }

  adapterSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
  receiverSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    error: 0
  };
	messageSummary: MessageSummary = {
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

	messageLog: Record<string, MessageLog> = {};
  updateMessageLog(messageLog: Record<string, MessageLog>) {
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
	updateDatabaseSchedulesEnabled(databaseSchedulesEnabled: boolean) {
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

	updateAdapterSummary(configurationName?: string) {
    var updated = (new Date().getTime());
    if (updated - 3000 < this.lastUpdated && !configurationName) { //3 seconds
      clearTimeout(this.timeout);
      this.timeout = window.setTimeout(() => this.updateAdapterSummary(), 1000);
      return;
    }
    if (configurationName == undefined)
      configurationName = this.$state.params["configuration"];

    var adapterSummary: Record<Lowercase<RunState>, number> = {
      started: 0,
      stopped: 0,
      starting: 0,
      stopping: 0,
      exception_starting: 0,
      exception_stopping: 0,
      error: 0
    };
    var receiverSummary: Record<Lowercase<RunState>, number> = {
      started: 0,
      stopped: 0,
      starting: 0,
      stopping: 0,
      exception_starting: 0,
      exception_stopping: 0,
      error: 0
    };
    var messageSummary: Record<Lowercase<MessageLevel>, number> = {
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
          receiverSummary[adapter.receivers[+i].state.toLowerCase() as Lowercase<RunState>]++;
        }
        for (const i in adapter.messages) {
          var level = adapter.messages[+i].level.toLowerCase() as Lowercase<MessageLevel>;
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
  const service = new AppService($rootScope, $state);


	return service;
}]);
