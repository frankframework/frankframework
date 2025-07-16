import { Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { Idle } from '@ng-idle/core';
import { filter, first, Subscription } from 'rxjs';
import {
  Adapter,
  AdapterMessage,
  AppConstants,
  appInitState,
  AppService,
  ClusterMember,
  ConfigurationMessage,
  ConsoleState,
  MessageLog,
} from './app.service';
import {
  ActivatedRoute,
  convertToParamMap,
  Data,
  NavigationCancel,
  NavigationEnd,
  NavigationSkipped,
  NavigationStart,
  ParamMap,
  Router,
  RouterLink,
  RouterOutlet,
} from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from './services/notification.service';
import { MiscService } from './services/misc.service';
import { DebugService } from './services/debug.service';
import { AuthService } from './services/auth.service';
import { SessionService } from './services/session.service';
import { SweetalertService } from './services/sweetalert.service';
import { Title } from '@angular/platform-browser';
import { NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import { InformationModalComponent } from './components/pages/information-modal/information-modal.component';
import { ToastService } from './services/toast.service';
import { ServerInfo, ServerInfoService } from './services/server-info.service';
import { ClusterMemberEvent, ClusterMemberEventType, WebsocketService } from './services/websocket.service';
import { deepMerge } from './utils';
import { ServerTimeService } from './services/server-time.service';

import { ToastsContainerComponent } from './components/toasts-container/toasts-container.component';
import { PagesNavigationComponent } from './components/pages/pages-navigation/pages-navigation.component';
import { PagesTopnavbarComponent } from './components/pages/pages-topnavbar/pages-topnavbar.component';
import { PagesTopinfobarComponent } from './components/pages/pages-topinfobar/pages-topinfobar.component';
import { PagesFooterComponent } from './components/pages/pages-footer/pages-footer.component';
// @ts-expect-error pace-js does not have types
import Pace from 'pace-js';

@Component({
  selector: 'app-root',
  imports: [
    ToastsContainerComponent,
    PagesNavigationComponent,
    PagesTopnavbarComponent,
    PagesTopinfobarComponent,
    RouterOutlet,
    PagesFooterComponent,
    RouterLink,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit, OnDestroy {
  protected loading = true;
  protected dtapStage = '';
  protected dtapSide = '';
  protected startupError: string | null = null;
  protected userName?: string;
  protected routeData: Data = {};
  protected routeQueryParams: ParamMap = convertToParamMap({});
  protected isLoginView: boolean = false;
  protected clusterMembers: ClusterMember[] = [];
  protected selectedClusterMember: ClusterMember | null = null;

  private serverInfo: ServerInfo | null = null;
  private appConstants: AppConstants;
  private consoleState: ConsoleState;
  private _subscriptions = new Subscription();
  private _subscriptionsReloadable = new Subscription();
  private readonly MODAL_OPTIONS_CLASSES: NgbModalOptions = {
    modalDialogClass: 'animated fadeInDown',
    windowClass: 'animated fadeIn',
  };

  private messageKeeperSize = 10; // see Adapter.java#messageKeeperSize

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private renderer: Renderer2,
    private title: Title,
    private authService: AuthService,
    private notificationService: NotificationService,
    private miscService: MiscService,
    private sessionService: SessionService,
    private debugService: DebugService,
    private sweetAlertService: SweetalertService,
    private toastService: ToastService,
    private appService: AppService,
    private idle: Idle,
    private modalService: NgbModal,
    private serverInfoService: ServerInfoService,
    private websocketService: WebsocketService,
    private serverTimeService: ServerTimeService,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    this.consoleState = this.appService.CONSOLE_STATE;

    Pace.start({
      eventLag: {
        minSamples: 10,
        sampleCount: 3,
        lagThreshold: 20,
      },
      restartOnRequestAfter: false,
    });
  }

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationStart && event.url.startsWith('/!')),
        first(),
      )
      .subscribe((event) => {
        const navigationEvent = event as NavigationStart;
        this.router.navigateByUrl(navigationEvent.url.replace('/!', ''));
      });

    this.router.events.pipe(filter((event) => event instanceof NavigationSkipped)).subscribe(() => {
      const childRoute = this.route.children.at(-1);
      if (childRoute) {
        this.routeQueryParams = childRoute.snapshot.queryParamMap;
        this.routeData = childRoute.snapshot.data;
      }
    });

    this.router.events.pipe(filter((event) => event instanceof NavigationCancel)).subscribe(() => {
      if (this.loading) setTimeout(() => this.router.navigate(['loading']));
    });

    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      const childRoute = this.route.children.at(-1);
      if (childRoute) {
        this.handleQueryParams(childRoute.snapshot.queryParamMap);
        this.routeData = childRoute.snapshot.data;
        if (this.router.url === '/login') {
          this.isLoginView = true;
          this.renderer.addClass(document.body, 'gray-bg');
        } else {
          this.isLoginView = false;
          this.renderer.removeClass(document.body, 'gray-bg');
        }
      }
    });

    const idleStartSubscription = this.idle.onIdleStart.subscribe(() => {
      const idleTimeout =
        Number.parseInt(this.appConstants['console.idle.timeout'] as string) > 0
          ? Number.parseInt(this.appConstants['console.idle.timeout'] as string)
          : false;
      if (!idleTimeout) return;

      this.sweetAlertService.Warning({
        title: 'Idle timer...',
        text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
        showConfirmButton: false,
        showCloseButton: true,
      });
    });
    this._subscriptions.add(idleStartSubscription);

    const idleWarnSubscription = this.idle.onTimeoutWarning.subscribe((timeRemaining) => {
      let minutes = Math.floor(timeRemaining / 60);
      let seconds = Math.round(timeRemaining % 60);
      if (minutes < 10) minutes = +'0' + minutes;
      if (seconds < 10) seconds = +'0' + seconds;
      const elm = document.querySelector('.swal2-container .idleTimer');
      if (elm) elm.textContent = `${minutes}:${seconds}`;
    });
    this._subscriptions.add(idleWarnSubscription);

    const idleTimeoutSubscription = this.idle.onTimeout.subscribe(() => {
      this.sweetAlertService.Info({
        title: 'Idle timer...',
        text: 'You have been logged out due to inactivity.',
        showCloseButton: true,
      });
      this.router.navigate(['logout']);
    });
    this._subscriptions.add(idleTimeoutSubscription);

    const idleEndSubscription = this.idle.onIdleEnd.subscribe(() => {
      const element = document.querySelector<HTMLElement>('.swal2-container .swal2-close');
      if (element) element.click();
    });
    this._subscriptions.add(idleEndSubscription);

    const reloadSubscription = this.appService.reload$.subscribe(() => this.onAppReload());
    this._subscriptions.add(reloadSubscription);

    this.initializeFrankConsole();
  }

  ngOnDestroy(): void {
    this.websocketService.deactivate();
    this._subscriptions.unsubscribe();
    this._subscriptionsReloadable.unsubscribe();
  }

  onAppReload(): void {
    this.websocketService.deactivate();
    this._subscriptionsReloadable.unsubscribe();
    this.consoleState.init = appInitState.UN_INIT;

    this.appService.resetAlerts();
    this.appService.resetMessageLog();
    this.appService.resetAdapters();
    this.appService.updateLoading(true);

    this.initializeFrankConsole();
  }

  handleQueryParams(parameters: ParamMap): void {
    this.routeQueryParams = parameters;
    const uwu = parameters.get('uwu');
    if (uwu === 'true') {
      localStorage.setItem('uwu', uwu);
    } else if (uwu === 'false') {
      localStorage.removeItem('uwu');
    }
  }

  initializeFrankConsole(): void {
    if (this.consoleState.init !== appInitState.UN_INIT) {
      this.debugService.log('Cancelling 2nd initialization attempt');
      Pace.stop();
      return;
    }
    this.consoleState.init = appInitState.PRE_INIT;
    this.debugService.log('Initializing Frank!Console');

    this.consoleState.init = appInitState.INIT;
    this.serverInfoService.serverInfo$.pipe(first()).subscribe({
      next: (data) => {
        this.serverInfo = data;

        this.dtapStage = data['dtap.stage'];
        this.appService.updateDtapStage(data['dtap.stage']);
        this.dtapSide = data['dtap.side'];
        this.userName = data['userName'];
        this.appService.updateInstanceName(data.instance.name);
        this.authService.setLoggedIn(this.userName);
        this.appService.updateTitle(this.title.getTitle().split(' | ')[1]);

        this.consoleState.init = appInitState.POST_INIT;
        if (!this.router.url.includes('login')) {
          this.idle.watch();
          this.renderer.removeClass(document.body, 'gray-bg');
        }

        this.serverTimeService.setServerTime(data['serverTime'], data['serverTimezone'], data['serverTimezoneOffset']);

        const iafInfoElement = document.querySelector<HTMLElement>('.iaf-info');
        if (iafInfoElement)
          iafInfoElement.textContent = `${data.framework.name} ${data.framework.version}: ${data.instance.name} ${data.instance.version}`;

        if (this.appService.dtapStage == 'LOC') {
          this.debugService.setLevel(3);
        }

        //Was it able to retrieve the serverinfo without logging in?
        if (!this.authService.isLoggedIn()) {
          this.idle.setTimeout(0);
        }

        this.appService.getConfigurations().subscribe((data) => {
          this.appService.updateConfigurations(data);
        });

        this.initializeWarnings();
        this.checkIafVersions();
      },
    });
    this.serverInfoService.refresh().subscribe({
      error: (error: HttpErrorResponse) => {
        // HTTP 5xx error
        if (error.status.toString().startsWith('5')) {
          this.router.navigate(['error']);
        } else if (error.status === 400) {
          this.appService.getClusterMembers().subscribe({
            next: (data) => {
              this.clusterMembers = data;
              if (data.length > 0) {
                this.selectedClusterMember = data.find((member) => member.selectedMember) ?? null;
              }
              if (this.selectedClusterMember != null) {
                this.appService.triggerReload();
              }
            },
            error: () => {
              this.sweetAlertService
                .Error("Couldn't initialize Frank!Console", 'Please make sure the Frank!Framework is setup correctly!')
                .then(() => this.appService.triggerReload());
            },
          });
        }
      },
    });

    this.appService.getEnvironmentVariables().subscribe((data) => {
      if (data['Application Constants']) {
        this.appConstants = Object.assign(this.appConstants, data['Application Constants']['Global']); //make FF!Application Constants default

        const idleTime =
          Number.parseInt(this.appConstants['console.idle.time'] as string) > 0
            ? Number.parseInt(this.appConstants['console.idle.time'] as string)
            : 0;
        if (idleTime > 0) {
          const idleTimeout =
            Number.parseInt(this.appConstants['console.idle.timeout'] as string) > 0
              ? Number.parseInt(this.appConstants['console.idle.timeout'] as string)
              : 0;
          this.idle.setIdle(idleTime);
          this.idle.setTimeout(idleTimeout);
        } else {
          this.idle.stop();
        }
        this.appService.updateDatabaseSchedulesEnabled(this.appConstants['loadDatabaseSchedules.active'] === 'true');
        this.appService.triggerAppConstants();
      }
    });
  }

  checkIafVersions(): void {
    /* Check FF version */
    console.log('Checking FF version with remote...');
    this.appService.getIafVersions(this.miscService.getUID(this.serverInfo!)).subscribe((response) => {
      this.serverInfo = null;
      if (!response || response.length === 0) return;

      const release = response[0]; //Not sure what ID to pick, smallest or latest?

      const newVersion = release.tag_name.slice(0, 1) == 'v' ? release.tag_name.slice(1) : release.tag_name;
      const currentVersion = this.appConstants['application.version'] as string;
      const version = this.miscService.compare_version(newVersion, currentVersion) ?? 0;
      if (!currentVersion || currentVersion === '') {
        this.debugService.warn(`Latest version is '${newVersion}' but can't retrieve current version.`);
        this.sessionService.set('IAF-Release', newVersion);
        return;
      }

      this.debugService.log(`Comparing version: '${currentVersion}' with latest release: '${newVersion}'.`);
      this.sessionService.remove('IAF-Release');

      if (+version > 0) {
        this.sessionService.set('IAF-Release', release);
        this.notificationService.add('fa-exclamation-circle', 'FF! update available!', false, () => {
          this.router.navigate(['iaf-update']);
        });
      }
    });
  }

  initializeWebsocket(): void {
    this.appService.updateLoading(false);
    if (this.loading) {
      this.websocketService.onConnected$.subscribe(() => {
        this.loading = false;

        const channelBaseUrl = this.selectedClusterMember ? `/event/${this.selectedClusterMember.id}` : '/event';

        this.websocketService.subscribe<Record<string, MessageLog>>(
          `${channelBaseUrl}/server-warnings`,
          (configurations) => this.processWarnings(configurations),
        );

        this.websocketService.subscribe<Record<string, Partial<Adapter>>>(`${channelBaseUrl}/adapters`, (adapters) =>
          this.processAdapters(adapters),
        );

        this.websocketService.subscribe<ClusterMemberEvent>('/event/cluster', (clusterMemberEvent) =>
          this.updateClusterMembers(clusterMemberEvent.member, clusterMemberEvent.type),
        );
      });
    }

    this.appService.getAdapters('all').subscribe((data) => {
      this.processAdapters(data);
      this.websocketService.activate();
    });
  }

  initializeWarnings(): void {
    const startupErrorSubscription = this.appService.startupError$.subscribe(() => {
      this.startupError = this.appService.startupError;
    });
    this._subscriptionsReloadable.add(startupErrorSubscription);

    this.http
      .get<Record<string, MessageLog>>(`${this.appService.absoluteApiPath}server/warnings`)
      .subscribe((data) => this.processWarnings(data));

    this.initializeAdapters();
  }

  initializeAdapters(): void {
    //Get base information first, then update it with more details
    this.appService.getAdapters().subscribe((data: Record<string, Adapter>) => this.finalizeStartup(data));
  }

  processWarnings(configurations: Record<string, Partial<MessageLog> | number | string>): void {
    configurations['All'] = {
      messages: configurations['messages'] as ConfigurationMessage[],
      errorStoreCount: configurations['totalErrorStoreCount'] as number,
      messageLevel: 'ERROR',
      serverTime: configurations['serverTime'] as number,
      uptime: configurations['serverTime'] as number,
    };

    if (configurations['warnings']) {
      for (const warning of configurations['warnings'] as unknown as string[]) {
        this.appService.addWarning('', warning);
      }
    }

    for (const index in configurations) {
      const existingConfiguration = this.appService.messageLog[index] as MessageLog | undefined;
      const configuration = configurations[index];
      if (configuration === null) {
        this.appService.removeAlerts(configuration);
        continue;
      } else if (Array.isArray(configuration) || typeof configuration !== 'object') {
        delete configurations[index];
        continue;
      }

      if (configuration.exception) this.appService.addException(index, configuration.exception);
      if (configuration.warnings) {
        for (const warning of configuration.warnings) {
          this.appService.addWarning(index, warning);
        }
      } else if (configuration.warnings === null) {
        this.appService.removeAlerts(index);
      }

      if (existingConfiguration && configuration.messages) {
        configuration.messages = [...existingConfiguration.messages, ...configuration.messages].slice(
          -this.messageKeeperSize,
        );
      }

      configuration.messageLevel = existingConfiguration?.messageLevel ?? 'INFO';
      if (configuration.messages) {
        configuration.messages = configuration.messages.sort(
          (a: ConfigurationMessage, b: ConfigurationMessage) => b.date - a.date,
        );
        for (const x in configuration.messages) {
          const level = configuration.messages[x].level;
          if (level == 'WARN' && configuration.messageLevel != 'ERROR') configuration.messageLevel = 'WARN';
          if (level == 'ERROR') configuration.messageLevel = 'ERROR';
        }
      }
    }

    this.appService.updateMessageLog(configurations as Record<string, Partial<MessageLog>>);
  }

  processAdapters(adapters: Record<string, Partial<Adapter>>): void {
    let reloadedAdapters = false;
    const updatedAdapters: Record<string, Partial<Adapter>> = {};
    const deletedAdapters: string[] = [];

    for (const adapterIndex in adapters) {
      const adapter = adapters[adapterIndex];
      const existingAdapter = this.appService.adapters[adapterIndex];

      if (adapter === null) {
        deletedAdapters.push(adapterIndex);
        continue;
      }

      adapter.sendersMessageLogCount = 0;
      adapter.senderTransactionalStorageMessageCount = 0;
      adapter.hasSender = false;
      adapter.state ??= existingAdapter.state ?? 'error';

      this.processAdapterReceivers(adapter, existingAdapter);
      this.processAdapterPipes(adapter, existingAdapter);
      this.processAdapterMessages(adapter, existingAdapter);

      if (adapter.receiverReachedMaxExceptions) {
        adapter.status = 'warning';
      }
      //If last message is WARN or ERROR change adapter status to warning.
      if (adapter.messages && adapter.messages.length > 0 && adapter.status != 'stopped') {
        const message = adapter.messages.at(-1);
        if (message && message.level != 'INFO') adapter.status = 'warning';
      }
      if (adapter.state != 'started') {
        adapter.status = 'stopped';
      }
      adapter.status ??= 'started';
      if (!reloadedAdapters) reloadedAdapters = this.hasAdapterReloaded(adapter);

      updatedAdapters[adapterIndex] = adapter;

      const selectedConfiguration = this.routeQueryParams.get('configuration');
      this.appService.updateAdapterSummary(selectedConfiguration ?? 'All', false);
      this.updateAdapterNotifications(adapter.name ?? existingAdapter.name, adapter);
    }

    for (const deletedAdapter of deletedAdapters) {
      this.appService.removeAdapter(deletedAdapter);
      this.appService.removeAlerts(deletedAdapter);
    }

    this.appService.updateAdapters(updatedAdapters);

    if (reloadedAdapters)
      this.toastService.success('Reloaded', 'Adapter(s) have successfully been reloaded!', { timeout: 3000 });
  }

  finalizeStartup(data: Record<string, Adapter>): void {
    this.processAdapters(data);
    this.appService.getClusterMembers().subscribe((data) => {
      this.clusterMembers = data;
      if (data.length > 0) {
        this.selectedClusterMember = data.find((member) => member.selectedMember) ?? null;
      }
      this.initializeWebsocket();
    });
  }

  processAdapterReceivers(adapter: Partial<Adapter>, existingAdapter?: Adapter): void {
    if (existingAdapter?.receivers) {
      adapter.receivers = deepMerge([], existingAdapter.receivers, adapter.receivers);
    }
    if (adapter.receivers) {
      for (const index in adapter.receivers) {
        const adapterReceiver = adapter.receivers[+index];
        if (adapterReceiver.state != 'started') adapter.status = 'warning';

        if (adapterReceiver.transactionalStores) {
          const store = adapterReceiver.transactionalStores['ERROR'];
          if (store && store.numberOfMessages > 0) {
            adapter.status = 'warning';
          }
        }
      }
    }
  }

  processAdapterPipes(adapter: Partial<Adapter>, existingAdapter?: Adapter): void {
    if (existingAdapter?.pipes) {
      adapter.pipes = deepMerge([], existingAdapter.pipes, adapter.pipes);
    }
    if (adapter.pipes) {
      for (const index in adapter.pipes) {
        const pipe = adapter.pipes[+index];

        if (!pipe.sender) continue;
        adapter.hasSender = true;

        if (!pipe.hasMessageLog) continue;
        const count = Number.parseInt(pipe.messageLogCount ?? '');

        if (Number.isNaN(count)) continue;
        if (pipe.isSenderTransactionalStorage) {
          adapter.senderTransactionalStorageMessageCount! += count;
        } else {
          adapter.sendersMessageLogCount! += count;
        }
      }
    }
  }

  processAdapterMessages(adapter: Partial<Adapter>, existingAdapter?: Adapter): void {
    if (!adapter.messages) adapter.messages = existingAdapter?.messages ?? [];
    adapter.messages.sort((a, b) => b.date - a.date);
  }

  updateAdapterNotifications(adapterName: string, adapter: Partial<Adapter>): void {
    let name = adapterName;
    if (name.length > 20) name = `${name.slice(0, 17)}...`;
    if (adapter.started === true) {
      for (const x in adapter.receivers) {
        // TODO Receiver.started is not really a thing, maybe this should work differently?
        // @ts-expect-error Receiver.started does not exist
        if (adapter.receivers[+x].started === false) {
          this.notificationService.add('fa-exclamation-circle', `Receiver '${name}' stopped!`, false, () => {
            this.router.navigate(['status'], { fragment: adapter.name });
          });
        }
      }
    } else if (adapter.started === false) {
      this.notificationService.add('fa-exclamation-circle', `Adapter '${name}' stopped!`, false, () => {
        this.router.navigate(['status'], { fragment: adapter.name });
      });
    }
  }

  hasAdapterReloaded(adapter: Partial<Adapter>): boolean {
    if (adapter.upSince) {
      const oldAdapter = this.appService.adapters[`${adapter.configuration}/${adapter.name}`];
      return adapter.upSince > oldAdapter?.upSince;
    }
    return false;
  }

  openInfoModel(): void {
    this.modalService.open(InformationModalComponent, this.MODAL_OPTIONS_CLASSES);
  }

  private updateClusterMembers(member: ClusterMember, action: ClusterMemberEventType): void {
    const memberExists = this.clusterMembers.some((m) => m.id === member.id);
    if (action === 'ADD_MEMBER' && !memberExists) {
      this.clusterMembers = [...this.clusterMembers, member];
    } else if (action === 'REMOVE_MEMBER' && memberExists) {
      this.clusterMembers = this.clusterMembers.filter((m) => m.id !== member.id);

      if (this.selectedClusterMember?.id === member.id) {
        this.sweetAlertService
          .Warning({
            title: 'Current cluster member has been removed',
            text: 'Reload to a different member or stay in current unstable instance?',
            showCancelButton: true,
            cancelButtonText: 'Stay',
            confirmButtonText: 'Reload',
          })
          .then((result) => {
            if (result.isConfirmed) {
              if (this.clusterMembers.length > 0) {
                this.appService.updateSelectedClusterMember(this.clusterMembers[0].id).subscribe(() => {
                  this.appService.triggerReload();
                });
                return;
              }
              this.appService.triggerReload();
            }
          });
      }
    }
  }
}
