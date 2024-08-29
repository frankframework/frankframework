import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject, catchError, of } from 'rxjs';
import { DebugService } from './services/debug.service';
import { Title } from '@angular/platform-browser';
import { computeServerPath, deepMerge, findIndexOfAll } from './utils';

export type RunState =
  | 'ERROR'
  | 'STARTING'
  | 'EXCEPTION_STARTING'
  | 'STARTED'
  | 'STOPPING'
  | 'EXCEPTION_STOPPING'
  | 'STOPPED';
export type RunStateRuntime = RunState | 'loading';
export type MessageLevel = 'INFO' | 'WARN' | 'ERROR';
export type AdapterStatus = 'started' | 'warning' | 'stopped';
export type TransactionalStores = Record<'DONE' | 'ERROR', { name: string; numberOfMessages: number }>;

export type Receiver = {
  isEsbJmsFFListener: boolean;
  name: string;
  transactionalStores: TransactionalStores;
  listener: {
    name: string;
    destination: string;
    class: string;
    isRestListener: boolean;
  };
  messages: {
    retried: number;
    rejected: number;
    received: number;
  };
  state: Lowercase<RunStateRuntime>;
  threadCount?: number;
  maxThreadCount?: number;
  threadCountControllable?: true;
};

type Message = {
  date: number;
  level: MessageLevel;
};

export interface JobMessage extends Message {
  text: string;
}

export interface AdapterMessage extends Message {
  capacity: number;
  message: string;
}

export type Pipe = {
  forwards: Record<'success' | 'exception', string>;
  sender: string;
  name: string;
  destination: string;
  message?: PipeMessage;
  isJdbcSender?: boolean;
  hasMessageLog?: boolean;
  messageLogCount?: 'error' | string;
  isSenderTransactionalStorage?: boolean;
  certificate?: Certificate;
};

export type PipeMessage = {
  name: string;
  type: string;
  slotId: string;
  count: string;
};

export type Certificate = {
  name: string;
};

export type Adapter = {
  configuration: string;
  configured: boolean;
  upSince: number;
  name: string;
  description: null | string;
  started: boolean;
  state: Lowercase<RunState>;
  receivers?: Receiver[];
  messages?: AdapterMessage[];
  pipes?: Pipe[];
  hasSender?: boolean;
  status?: AdapterStatus;
  sendersMessageLogCount?: number;
  senderTransactionalStorageMessageCount?: number;
  receiverReachedMaxExceptions?: boolean;
  lastMessage?: number;
  messagesInProcess?: number;
  messagesProcessed?: number;
  messagesInError?: number;
  messageLogMessageCount?: number;
  errorStoreMessageCount?: number;
};

export type Configuration = {
  name: string;
  stubbed: boolean;
  state: RunState;
  type:
    | 'DatabaseClassLoader'
    | 'DirectoryClassLoader'
    | 'DummyClassLoader'
    | 'JarFileClassLoader'
    | 'ScanningDirectoryClassLoader'
    | 'WebAppClassLoader';
  jdbcMigrator: boolean;
  exception?: string;
  version?: string;
  parent?: string;
  filename?: string;
  created?: string;
  user?: string;
  active?: boolean;
  autoreload?: boolean;
  loaded?: boolean;

  actived?: boolean; // not from the api, love the name
};

export type Alert = {
  link?: { name: string; '#': string };
  type: string;
  configuration: string;
  message: string;
};

export type MessageLog = {
  errorStoreCount: number;
  messages: AdapterMessage[];
  messageLevel: MessageLevel;
  exception?: string;
  warnings?: string[];
  serverTime?: number;
  uptime?: number;
};

export type Summary = Record<Lowercase<RunState>, number>;

export type MessageSummary = {
  info: number;
  warn: number;
  error: number;
};

export type IAFRelease = {
  url: string;
  assets_url: string;
  upload_url: string;
  html_url: string;
  id: number;
  author: {
    login: string;
    id: number;
    node_id: string;
    avatar_url: string;
    gravatar_id: string;
    url: string;
    html_url: string;
    followers_url: string;
    following_url: string;
    gists_url: string;
    starred_url: string;
    subscriptions_url: string;
    organizations_url: string;
    repos_url: string;
    events_url: string;
    received_events_url: string;
    type: string;
    site_admin: boolean;
  };
  node_id: string;
  tag_name: string;
  target_commitish: string;
  name: string;
  draft: boolean;
  prerelease: boolean;
  created_at: string;
  published_at: string;
  assets: [];
  tarball_url: string;
  zipball_url: string;
  body: string;
  reactions: Record<string, number>;
};

export type ServerEnvironmentVariables = {
  'Application Constants': Record<string, Record<string, string>>;
  'Environment Variables': Record<string, string>;
  'System Properties': Record<string, string>;
};

export type ClusterMember = {
  id: string;
  address: string;
  localMember: boolean;
  selectedMember: boolean;
  type: 'worker';
  attributes: Record<string, string> & {
    name?: string;
    application?: string;
  };
};

export type AppConstants = Record<string, string | number | boolean | object>;

export type ServerErrorResponse = {
  status: string;
  error: string;
};

export const appInitState = {
  UN_INIT: -1,
  PRE_INIT: 0,
  INIT: 1,
  POST_INIT: 2,
} as const;
export type AppInitState = (typeof appInitState)[keyof typeof appInitState];

export type ConsoleState = {
  server: string;
  timeOffset: number;
  init: AppInitState;
};

@Injectable({
  providedIn: 'root',
})
export class AppService {
  private loadingSubject = new Subject<boolean>();
  private reloadSubject = new Subject<void>();
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
  private iframePopoutUrlSubject = new Subject<string>();

  loading$ = this.loadingSubject.asObservable();
  reload$ = this.reloadSubject.asObservable();
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
  iframePopoutUrl$ = this.iframePopoutUrlSubject.asObservable();

  adapters: Record<string, Adapter> = {};
  alerts: Alert[] = [];

  adapterSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  };
  receiverSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  };
  messageSummary: MessageSummary = {
    info: 0,
    warn: 0,
    error: 0,
  };

  APP_CONSTANTS: AppConstants = {
    //Configure these in the server AppConstants!!!
    //The settings here are defaults and will be overwritten upon set in any .properties file.

    //How often the interactive frontend should poll the FF API for new data
    'console.pollerInterval': 10_000,

    //How often the interactive frontend should poll during IDLE state
    'console.idle.pollerInterval': 60_000,

    //After x minutes the app goes into 'idle' state (use 0 to disable)
    'console.idle.time': 300,

    //After x minutes the user will be forcefully logged out
    'console.idle.timeout': 0,

    //Time format in which to display the time and date.
    'console.dateFormat': 'yyyy-MM-dd HH:mm:ss',
  };

  CONSOLE_STATE: ConsoleState = {
    server: computeServerPath(),
    timeOffset: 0,
    init: appInitState.UN_INIT,
  };

  absoluteApiPath = `${this.getServerPath()}iaf/api/`;

  private lastUpdated = 0;
  private timeout?: number;

  constructor(
    private title: Title,
    private http: HttpClient,
    private debugService: DebugService,
  ) {}

  triggerReload(): void {
    this.reloadSubject.next();
  }

  updateLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  customBreadcrumbs(breadcrumbs: string): void {
    this.customBreadcrumbsSubject.next(breadcrumbs);
  }

  triggerAppConstants(): void {
    this.appConstantsSubject.next();
  }

  updateAdapters(adapters: Record<string, Partial<Adapter>>): void {
    this.adapters = deepMerge({}, this.adapters, adapters);
    this.adaptersSubject.next({ ...this.adapters });
  }

  resetAdapters(): void {
    this.adapters = {};
    this.adaptersSubject.next(this.adapters);
  }

  removeAdapter(adapter: string): void {
    delete this.adapters[adapter];
  }

  updateAlerts(alerts: Alert[]): void {
    this.alerts = alerts;
    this.alertsSubject.next(alerts);
  }

  resetAlerts(): void {
    this.alerts = [];
    this.alertsSubject.next(this.alerts);
  }

  startupError: string | null = null;
  updateStartupError(startupError: string): void {
    this.startupError = startupError;
    this.startupErrorSubject.next(startupError);
  }

  configurations: Configuration[] = [];
  updateConfigurations(configurations: Configuration[]): void {
    const updatedConfigurations: Configuration[] = [];
    for (const index in configurations) {
      const config = configurations[index];
      if (config.name.startsWith('IAF_')) updatedConfigurations.unshift(config);
      else updatedConfigurations.push(config);
    }
    this.configurations = updatedConfigurations;
    this.configurationsSubject.next(updatedConfigurations);
  }

  messageLog: Record<string, MessageLog> = {};
  updateMessageLog(messageLog: Record<string, Partial<MessageLog>>): void {
    this.messageLog = deepMerge({}, this.messageLog, messageLog);
    this.messageLogSubject.next({ ...this.messageLog });
  }

  resetMessageLog(): void {
    this.messageLog = {};
    this.messageLogSubject.next(this.messageLog);
  }

  instanceName = '';
  updateInstanceName(instanceName: string): void {
    this.instanceName = instanceName;
    this.instanceNameSubject.next(instanceName);
  }

  dtapStage = '';
  updateDtapStage(dtapStage: string): void {
    this.dtapStage = dtapStage;
    this.dtapStageSubject.next(dtapStage);
  }

  databaseSchedulesEnabled = false;
  updateDatabaseSchedulesEnabled(databaseSchedulesEnabled: boolean): void {
    this.databaseSchedulesEnabled = databaseSchedulesEnabled;
    this.databaseSchedulesEnabledSubject.next(databaseSchedulesEnabled);
  }

  updateTitle(title: string): void {
    this.title.setTitle(`${this.dtapStage}-${this.instanceName} | ${title}`);
  }

  setIframePopoutUrl(url: string): void {
    this.iframePopoutUrlSubject.next(url);
  }

  addAlert(type: string, configuration: string, message: string): void {
    const line = message.match(/line \[(\d+)]/);
    const isValidationAlert = message.includes('Validation');
    const link = line && !isValidationAlert ? { name: configuration, '#': `L${line[1]}` } : undefined;
    this.alerts.push({
      link: link,
      type: type,
      configuration: configuration,
      message: message,
    });
    this.updateAlerts(this.alerts);
  }
  addWarning(configuration: string, message: string): void {
    this.addAlert('warning', configuration, message);
  }
  addException(configuration: string, message: string): void {
    this.addAlert('danger', configuration, message);
  }

  removeAlerts(configuration: string): void {
    const indicesToRemove = findIndexOfAll(this.alerts, (alert) => alert.configuration === configuration);
    const updatedAlerts = [...this.alerts];

    for (const index of indicesToRemove) {
      updatedAlerts.splice(index, 1);
    }
    this.updateAlerts(updatedAlerts);
  }

  getServerPath(): string {
    let absolutePath = this.CONSOLE_STATE.server;
    if (absolutePath && absolutePath.slice(-1) != '/') absolutePath += '/';
    return absolutePath;
  }

  getIafVersions(UID: string): Observable<IAFRelease[] | never[]> {
    return this.http.get<IAFRelease[]>(`https://ibissource.org/iaf/releases/?q=${UID}`).pipe(
      catchError((error) => {
        this.debugService.error('An error occured while comparing IAF versions', error);
        return of([]);
      }),
    );
  }

  getClusterMembers(): Observable<ClusterMember[]> {
    return this.http.get<ClusterMember[]>(`${this.absoluteApiPath}cluster/members?type=worker`);
  }

  updateSelectedClusterMember(id: string): Observable<object> {
    return this.http.post(`${this.absoluteApiPath}cluster/members`, {
      id,
    });
  }

  getConfigurations(): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(`${this.absoluteApiPath}server/configurations`);
  }

  getAdapters(expanded?: string): Observable<Record<string, Adapter>> {
    return this.http.get<Record<string, Adapter>>(
      `${this.absoluteApiPath}adapters${expanded ? `?expanded=${expanded}` : ''}`,
    );
  }

  getEnvironmentVariables(): Observable<ServerEnvironmentVariables> {
    return this.http.get<ServerEnvironmentVariables>(`${this.absoluteApiPath}environmentvariables`);
  }

  getServerHealth(): Observable<string> {
    return this.http.get(`${this.absoluteApiPath}server/health`, {
      responseType: 'text',
    });
  }

  updateAdapterSummary(configurationName: string, changedConfiguration: boolean): void {
    const updated = Date.now();
    if (updated - 3000 < this.lastUpdated && !changedConfiguration) {
      //3 seconds
      clearTimeout(this.timeout);
      this.timeout = window.setTimeout(() => {
        this.updateAdapterSummary(configurationName, false);
      }, 1000);
      return;
    }

    const adapterSummary: Record<Lowercase<RunState>, number> = {
      started: 0,
      stopped: 0,
      starting: 0,
      stopping: 0,
      exception_starting: 0,
      exception_stopping: 0,
      error: 0,
    };
    const receiverSummary: Record<Lowercase<RunState>, number> = {
      started: 0,
      stopped: 0,
      starting: 0,
      stopping: 0,
      exception_starting: 0,
      exception_stopping: 0,
      error: 0,
    };
    const messageSummary: Record<Lowercase<MessageLevel>, number> = {
      info: 0,
      warn: 0,
      error: 0,
    };

    const allAdapters = this.adapters;
    for (const adapterName in allAdapters) {
      const adapter = allAdapters[adapterName];

      if (adapter.configuration == configurationName || configurationName == 'All') {
        // Only adapters for active config
        adapterSummary[adapter.state]++;
        for (const index in adapter.receivers) {
          receiverSummary[adapter.receivers[+index].state.toLowerCase() as Lowercase<RunState>]++;
        }
        for (const index in adapter.messages) {
          const level = adapter.messages[+index].level.toLowerCase() as Lowercase<MessageLevel>;
          messageSummary[level]++;
        }
      }
    }

    this.adapterSummary = adapterSummary;
    this.receiverSummary = receiverSummary;
    this.messageSummary = messageSummary;
    this.lastUpdated = updated;
    this.summariesSubject.next();
  }
}
