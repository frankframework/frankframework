import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal, Signal, WritableSignal } from '@angular/core';
import { catchError, Observable, of, Subject } from 'rxjs';
import { DebugService } from './services/debug.service';
import { Title } from '@angular/platform-browser';
import { computeServerPath, deepMerge } from './utilities';

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

export type Message = {
  date: number;
  level: MessageLevel;
};

export type AdapterMessage = {
  message: string;
  capacity: number;
} & Message;

export type ConfigurationMessage = {
  message: string;
} & Message;

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
  messages: ConfigurationMessage[];
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

@Injectable({
  providedIn: 'root',
})
export class AppService {
  public absoluteApiPath = `${this.getServerPath()}iaf/api/`;
  public loading: WritableSignal<boolean> = signal(true);
  public alerts: WritableSignal<Alert[]> = signal([]);
  public startupError: WritableSignal<string | null> = signal(null);
  public instanceName: WritableSignal<string> = signal('-');
  public dtapStage: WritableSignal<string> = signal('-');
  public iframePopoutUrl: WritableSignal<string | null> = signal(null);
  public selectedConfigurationTab: WritableSignal<string | null> = signal(null);
  public reload$: Observable<void>;
  public toggleSidebar$: Observable<void>;
  public customBreadcrumbs$: Observable<string>;
  public consoleState: WritableSignal<AppInitState> = signal(appInitState.UN_INIT);

  private reloadSubject = new Subject<void>();
  private toggleSidebarSubject = new Subject<void>();
  private customBreadcrumbsSubject = new Subject<string>();

  private _adapters: WritableSignal<Record<string, Adapter>> = signal({});
  private _configurations: WritableSignal<Configuration[]> = signal([]);
  private _messageLog: WritableSignal<Record<string, MessageLog>> = signal({});
  private _adapterSummary: WritableSignal<Summary> = signal({
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  });
  private _receiverSummary: WritableSignal<Summary> = signal({
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  });
  private _messageSummary: WritableSignal<MessageSummary> = signal({
    info: 0,
    warn: 0,
    error: 0,
  });

  private _appConstants: WritableSignal<AppConstants> = signal({
    //How often the interactive frontend should poll the FF API for new data
    'console.pollerInterval': 10_000,

    //How often the interactive frontend should poll during IDLE state
    'console.idle.pollerInterval': 60_000,

    //After x minutes the app goes into 'idle' state (use 0 to disable)
    'console.idle.time': 300,

    //After x minutes the user will be forcefully logged out
    'console.idle.timeout': 0,
  });

  private lastUpdated = 0;
  private timeout?: number;

  private title: Title = inject(Title);
  private http: HttpClient = inject(HttpClient);
  private debugService: DebugService = inject(DebugService);

  constructor() {
    this.reload$ = this.reloadSubject.asObservable();
    this.toggleSidebar$ = this.toggleSidebarSubject.asObservable();
    this.customBreadcrumbs$ = this.customBreadcrumbsSubject.asObservable();
  }

  get adapters(): Signal<Record<string, Adapter>> {
    return this._adapters.asReadonly();
  }
  get configurations(): Signal<Configuration[]> {
    return this._configurations.asReadonly();
  }
  get messageLog(): Signal<Record<string, MessageLog>> {
    return this._messageLog.asReadonly();
  }
  get adapterSummary(): Signal<Summary> {
    return this._adapterSummary.asReadonly();
  }
  get receiverSummary(): Signal<Summary> {
    return this._receiverSummary.asReadonly();
  }
  get messageSummary(): Signal<MessageSummary> {
    return this._messageSummary.asReadonly();
  }
  get appConstants(): Signal<AppConstants> {
    return this._appConstants.asReadonly();
  }

  updateAdapters(adapters: Record<string, Partial<Adapter>>): void {
    const mergedAdapters = deepMerge({}, this.adapters(), adapters);
    this._adapters.set(mergedAdapters);
  }

  resetAdapters(): void {
    this._adapters.set({});
  }

  removeAdapter(adapter: string): void {
    const adapters = this.adapters();
    delete adapters[adapter];
    this._adapters.set(adapters);
  }

  updateConfigurations(configurations: Configuration[]): void {
    const updatedConfigurations: Configuration[] = [];
    for (const config of configurations) {
      if (config.name.startsWith('IAF_')) updatedConfigurations.unshift(config);
      else updatedConfigurations.push(config);
    }
    this._configurations.set(updatedConfigurations);
  }

  updateMessageLog(messageLog: Record<string, Partial<MessageLog>>): void {
    const mergedMessageLog = deepMerge({}, this.messageLog(), messageLog);
    this._messageLog.set(mergedMessageLog);
  }

  resetMessageLog(): void {
    this._messageLog.set({});
  }

  updateAppConstants(appConstants: AppConstants): void {
    this._appConstants.set(appConstants);
  }

  updateAdapterSummary(configurationName: string, changedConfiguration: boolean): void {
    const updated = Date.now();
    if (updated - 3000 < this.lastUpdated && !changedConfiguration) {
      //3 seconds
      clearTimeout(this.timeout);
      this.timeout = globalThis.setTimeout(() => {
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

    for (const adapter of Object.values(this.adapters())) {
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

    this._adapterSummary.set(adapterSummary);
    this._receiverSummary.set(receiverSummary);
    this._messageSummary.set(messageSummary);
    this.lastUpdated = updated;
  }

  addAlert(type: string, configuration: string, message: string): void {
    const line = message.match(/line \[(\d+)]/);
    const isValidationAlert = message.includes('Validation');
    const link = line && !isValidationAlert ? { name: configuration, '#': `L${line[1]}` } : undefined;
    const alerts = this.alerts();
    alerts.push({
      link: link,
      type: type,
      configuration: configuration,
      message: message,
    });
    this.alerts.set(alerts);
  }
  addWarning(configuration: string, message: string): void {
    this.addAlert('warning', configuration, message);
  }
  addException(configuration: string, message: string): void {
    this.addAlert('danger', configuration, message);
  }

  removeAlerts(configuration: string): void {
    const alerts = this.alerts();
    const updatedAlerts = alerts.filter((alert) => alert.configuration !== configuration);
    this.alerts.set(updatedAlerts);
  }

  triggerReload(): void {
    this.reloadSubject.next();
  }

  customBreadcrumbs(breadcrumbs: string): void {
    this.customBreadcrumbsSubject.next(breadcrumbs);
  }

  toggleSidebar(): void {
    this.toggleSidebarSubject.next();
  }

  updateTitle(title: string): void {
    this.title.setTitle(`${this.dtapStage()}-${this.instanceName()} | ${title}`);
  }

  getServerPath(): string {
    let absolutePath = computeServerPath();
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
}
