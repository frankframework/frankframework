import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ParamMap, Params } from '@angular/router';
import { Subject, catchError, of } from 'rxjs';
import { DebugService } from './services/debug.service';
import { Title } from '@angular/platform-browser';

export type RunState = 'ERROR' | 'STARTING' | 'EXCEPTION_STARTING' | 'STARTED' | 'STOPPING' | 'EXCEPTION_STOPPING' | 'STOPPED';
export type RunStateRuntime = RunState | 'loading'
export type MessageLevel = 'INFO' | 'WARN' | 'ERROR';
export type AdapterStatus = 'started' | 'warning' | 'stopped';

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
  threadCount?: number,
  maxThreadCount?: number,
  threadCountControllable?: true
}

type Message = {
  date: number,
  level: MessageLevel,
}

export interface JobMessage extends Message {
  text: string
}

export interface AdapterMessage extends Message {
  capacity: number,
  message: string
}

export type Pipe = {
  forwards: Record<'success' | 'exception', string>,
  sender: string,
  name: string,
  destination: string,
  isJdbcSender: boolean,
  hasMessageLog?: boolean,
  messageLogCount?: 'error' | string,
  isSenderTransactionalStorage?: boolean,
  certificate?: Certificate
}

export type Certificate = {
  name: string
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
  status?: AdapterStatus,
  sendersMessageLogCount?: number,
  senderTransactionalStorageMessageCount?: number,
  receiverReachedMaxExceptions?: boolean
  lastMessage?: number,
  messagesInProcess?: number,
  messagesProcessed?: number,
  messagesInError?: number,
  messageLogMessageCount?: number,
  errorStoreMessageCount?: number,
}

export type Configuration = {
  name: string,
  stubbed: boolean,
  state: RunState,
  type: 'DatabaseClassLoader' | 'DirectoryClassLoader' | 'DummyClassLoader' | 'JarFileClassLoader' | 'ScanningDirectoryClassLoader' | 'WebAppClassLoader',
  jdbcMigrator: boolean,
  exception?: string,
  version?: string,
  parent?: string,
  filename?: string,
  created?: string,
  user?: string,
  active?: boolean,
  autoreload?: boolean,
  loaded?: boolean,

  actived?: boolean // not from the api, love the name
}

export type Alert = {
  link?: { name: string, '#': string },
  type: string,
  configuration: string,
  message: string
}

export type MessageLog = {
  errorStoreCount: number,
  messages: AdapterMessage[],
  messageLevel: MessageLevel,
}

export type Summary = Record<Lowercase<RunState>, number>;

export type MessageSummary = {
  info: number,
  warn: number,
  error: number
}

export type IAFRelease = {
  url: string,
  assets_url: string,
  upload_url: string,
  html_url: string,
  id: number,
  author: {
    login: string,
    id: number,
    node_id: string,
    avatar_url: string,
    gravatar_id: string,
    url: string,
    html_url: string,
    followers_url: string,
    following_url: string,
    gists_url: string,
    starred_url: string,
    subscriptions_url: string,
    organizations_url: string,
    repos_url: string,
    events_url: string,
    received_events_url: string,
    type: string,
    site_admin: boolean
  },
  node_id: string,
  tag_name: string,
  target_commitish: string,
  name: string,
  draft: boolean,
  prerelease: boolean,
  created_at: string,
  published_at: string,
  assets: [],
  tarball_url: string,
  zipball_url: string,
  body: string,
  reactions: Record<string, number>
}

export type ServerInfo = {
  fileSystem: {
    freeSpace: string,
    totalSpace: string
  },
  framework: {
    name: string,
    version: string
  },
  instance: {
    name: string,
    version: string
  },
  applicationServer: string,
  javaVersion: string,
  serverTime: number,
  "dtap.stage": string,
  "dtap.side": string,
  processMetrics: {
    maxMemory: string,
    freeMemory: string,
    totalMemory: string,
    heapSize: string
  },
  machineName: string,
  uptime: number,
  userName?: string
}

export type ServerEnvironmentVariables = {
  "Application Constants": Record<string, Record<string, string>>;
  "Environment Variables": Record<string, string>;
  "System Properties": Record<string, string>;
}

export type AppConstants = Record<string, string | any>;

@Injectable({
  providedIn: 'root'
})
export class AppService {

  private loadingSubject = new Subject<boolean>();
  private customBreadcrumbsSubject = new Subject<string>();
  private appConstantsSubject = new Subject<void>();
  private adaptersSubject = new Subject<Record<string, Adapter>>();
  private alertsSubject = new Subject<Alert[]>();
  private startupErrorSubject = new Subject<string | null>();
  private configurationsSubject = new Subject<Configuration[]>();
  private messageLogSubject = new Subject<Record<string, MessageLog>>();
  private instanceNameSubject = new Subject<string>();
  private dtapStageSubject = new Subject<string>();
  private databaseSchedulesEnabledSubject = new Subject<boolean>();
  private summariesSubject = new Subject<void>();
  private GDPRSubject = new Subject<void>();

  loading$ = this.loadingSubject.asObservable();
  customBreadscrumb$ = this.customBreadcrumbsSubject.asObservable();
  appConstants$ = this.appConstantsSubject.asObservable();
  adapters$ = this.adaptersSubject.asObservable();
  alerts$ = this.alertsSubject.asObservable();
  startupError$ = this.startupErrorSubject.asObservable();
  configurations$ = this.configurationsSubject.asObservable();
  messageLog$ = this.messageLogSubject.asObservable();
  instanceName$ = this.instanceNameSubject.asObservable();
  dtapStage$ = this.dtapStageSubject.asObservable();
  databaseSchedulesEnabled$ = this.databaseSchedulesEnabledSubject.asObservable();
  summaries$ = this.summariesSubject.asObservable();
  GDPR$ = this.GDPRSubject.asObservable();

  adapters: Record<string, Adapter> = {};
  alerts: Alert[] = [];

  adapterSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0
  };
  receiverSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0
  };
  messageSummary: MessageSummary = {
    info: 0,
    warn: 0,
    error: 0
  };

  APP_CONSTANTS: AppConstants = {
    //Configure these in the server AppConstants!!!
    //The settings here are defaults and will be overwritten upon set in any .properties file.

    //Server to connect to, defaults to local server.
    "server": server,

    //How often the interactive frontend should poll the IAF API for new data
    "console.pollerInterval": 30000,

    //How often the interactive frontend should poll during IDLE state
    "console.idle.pollerInterval": 180000,

    //After x minutes the app goes into 'idle' state (use 0 to disable)
    "console.idle.time": 300,

    //After x minutes the user will be forcefully logged out
    "console.idle.timeout": 0,

    //Time format in which to display the time and date.
    "console.dateFormat": "yyyy-MM-dd HH:mm:ss",

    //These will automatically be updated.
    "timeOffset": 0,
    "init": -1,
    getString: function (variable: keyof AppConstants) {
      return this[variable];
    },
    getBoolean: function (variable: keyof AppConstants, dfault: any) {
      if (this[variable] != undefined) return (this[variable] === "true");
      return dfault;
    }
  };

  absoluteApiPath = this.getServerPath() + "iaf/api/";

  private lastUpdated = 0;
  private timeout?: number;

  constructor(
    private title: Title,
    private http: HttpClient,
    private debugService: DebugService,
  ) { }

  updateLoading(loading: boolean) {
    this.loadingSubject.next(loading);
  }

  customBreadcrumbs(breadcrumbs: string){
    this.customBreadcrumbsSubject.next(breadcrumbs);
  }

  triggerAppConstants() {
    this.appConstantsSubject.next();
  }

  triggerGDPR() {
    this.GDPRSubject.next();
  }

  updateAdapters(adapters: Record<string, Adapter>) {
    this.adapters = adapters;
    this.adaptersSubject.next(adapters);
  }

  updateAlerts(alerts: Alert[]) {
    this.alerts = alerts;
    this.alertsSubject.next(alerts);
  }

  startupError: string | null = null;
  updateStartupError(startupError: string) {
    this.startupError = startupError;
    this.startupErrorSubject.next(startupError);
  }

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
    this.configurationsSubject.next(updatedConfigurations);
  }

  messageLog: Record<string, MessageLog> = {};
  updateMessageLog(messageLog: Record<string, MessageLog>) {
    this.messageLog = messageLog;
    this.messageLogSubject.next(messageLog);
  }

  instanceName = "";
  updateInstanceName(instanceName: string) {
    this.instanceName = instanceName;
    this.instanceNameSubject.next(instanceName);
  }

  dtapStage = "";
  updateDtapStage(dtapStage: string) {
    this.dtapStage = dtapStage;
    this.dtapStageSubject.next(dtapStage);
  }

  databaseSchedulesEnabled = false;
  updateDatabaseSchedulesEnabled(databaseSchedulesEnabled: boolean) {
    this.databaseSchedulesEnabled = databaseSchedulesEnabled;
    this.databaseSchedulesEnabledSubject.next(databaseSchedulesEnabled);
  }

  updateTitle(title: string){
    this.title.setTitle(`${this.dtapStage}-${this.instanceName} | ${title}`)
  }

  addAlert(type: string, configuration: string, message: string) {
    let line = message.match(/line \[(\d+)\]/);
    let isValidationAlert = message.indexOf("Validation") !== -1;
    let link = (line && !isValidationAlert) ? { name: configuration, '#': 'L' + line[1] } : undefined;
    this.alerts.push({
      link: link,
      type: type,
      configuration: configuration,
      message: message
    });
    this.updateAlerts(this.alerts);
  };
  addWarning(configuration: string, message: string) {
    this.addAlert("warning", configuration, message);
  };
  addException(configuration: string, message: string) {
    this.addAlert("danger", configuration, message);
  };

  getServerPath(): string {
    let absolutePath = this.APP_CONSTANTS["server"];
    if (absolutePath && absolutePath.slice(-1) != "/") absolutePath += "/";
    return absolutePath;
  }

  getIafVersions(UID: string){
    return this.http.get<IAFRelease[]>("https://ibissource.org/iaf/releases/?q=" + UID)
      .pipe(catchError((error) => {
        this.debugService.error("An error occured while comparing IAF versions", error);
        return of([]);
      }));
  }

  getServerInfo(){
    return this.http.get<ServerInfo>(this.absoluteApiPath + "server/info");
  }

  getConfigurations(){
    return this.http.get<Configuration[]>(this.absoluteApiPath + "server/configurations");
  }

  getAdapters(){
    return this.http.get<Record<string, Adapter>>(this.absoluteApiPath + "adapters");
  }

  getEnvironmentVariables(){
    return this.http.get<ServerEnvironmentVariables>(this.absoluteApiPath + "environmentvariables");
  }

  updateAdapterSummary(routeQueryParams: ParamMap, configurationName?: string) {
    var updated = (new Date().getTime());
    if (updated - 3000 < this.lastUpdated && !configurationName) { //3 seconds
      clearTimeout(this.timeout);
      this.timeout = window.setTimeout(() => this.updateAdapterSummary(routeQueryParams), 1000);
      return;
    }
    if (configurationName == undefined)
      configurationName = routeQueryParams.get("configuration") ?? undefined;

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
    this.summariesSubject.next();
  };

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
  }

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
  }

  getUserLocale() {
    if (window.navigator.languages) {
      return window.navigator.languages[0];
    }
    return window.navigator.language;
  }
}
