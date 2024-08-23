import {
  Component,
  Inject,
  LOCALE_ID,
  OnDestroy,
  OnInit,
  Renderer2,
} from '@angular/core';
import { Idle } from '@ng-idle/core';
import { filter, first, Subscription } from 'rxjs';
import {
  Adapter,
  AdapterMessage,
  AppConstants,
  appInitState,
  AppService,
  ConsoleState,
  MessageLog,
} from './app.service';
import {
  ActivatedRoute,
  convertToParamMap,
  Data,
  NavigationEnd,
  NavigationSkipped,
  NavigationStart,
  ParamMap,
  Router,
} from '@angular/router';
import { formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
// @ts-expect-error pace-js does not have types
import * as Pace from 'pace-js';
import { NotificationService } from './services/notification.service';
import { MiscService } from './services/misc.service';
import { DebugService } from './services/debug.service';
import { PollerService } from './services/poller.service';
import { AuthService } from './services/auth.service';
import { SessionService } from './services/session.service';
import { SweetalertService } from './services/sweetalert.service';
import { Title } from '@angular/platform-browser';
import { NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import { InformationModalComponent } from './components/pages/information-modal/information-modal.component';
import { ToastService } from './services/toast.service';
import { ServerInfo, ServerInfoService } from './services/server-info.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit, OnDestroy {
  loading = true;
  serverInfo: ServerInfo | null = null;
  dtapStage = '';
  dtapSide = '';
  serverTime = '';
  startupError: string | null = null;
  userName?: string;
  routeData: Data = {};
  routeQueryParams: ParamMap = convertToParamMap({});
  isLoginView: boolean = false;

  private appConstants: AppConstants;
  private consoleState: ConsoleState;
  private _subscriptions = new Subscription();
  private serializedRawAdapterData: Record<string, string> = {};
  private readonly MODAL_OPTIONS_CLASSES: NgbModalOptions = {
    modalDialogClass: 'animated fadeInDown',
    windowClass: 'animated fadeIn',
  };

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private renderer: Renderer2,
    private title: Title,
    private authService: AuthService,
    private pollerService: PollerService,
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
    @Inject(LOCALE_ID) private locale: string,
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
        filter(
          (event) =>
            event instanceof NavigationStart && event.url.startsWith('/!'),
        ),
        first(),
      )
      .subscribe((event) => {
        const navigationEvent = event as NavigationStart;
        this.router.navigateByUrl(navigationEvent.url.replace('/!', ''));
      });

    this.router.events
      .pipe(filter((event) => event instanceof NavigationSkipped))
      .subscribe(() => {
        const childRoute = this.route.children.at(-1);
        if (childRoute) {
          this.routeQueryParams = childRoute.snapshot.queryParamMap;
          this.routeData = childRoute.snapshot.data;
        }
      });

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
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
      this.pollerService
        .getAll()
        .changeInterval(
          this.appConstants['console.idle.pollerInterval'] as number,
        );

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

    const idleWarnSubscription = this.idle.onTimeoutWarning.subscribe(
      (timeRemaining) => {
        let minutes = Math.floor(timeRemaining / 60);
        let seconds = Math.round(timeRemaining % 60);
        if (minutes < 10) minutes = +'0' + minutes;
        if (seconds < 10) seconds = +'0' + seconds;
        const elm = document.querySelector('.swal2-container .idleTimer');
        if (elm) elm.textContent = `${minutes}:${seconds}`;
      },
    );
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
      const element = document.querySelector<HTMLElement>(
        '.swal2-container .swal2-close',
      );
      if (element) element.click();

      this.pollerService
        .getAll()
        .changeInterval(this.appConstants['console.pollerInterval'] as number);
    });
    this._subscriptions.add(idleEndSubscription);
    this.initializeFrankConsole();
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
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
    if (this.consoleState.init === appInitState.UN_INIT) {
      this.consoleState.init = appInitState.PRE_INIT;
      this.debugService.log('Initializing Frank!Console');
    } else if (this.consoleState.init === appInitState.PRE_INIT) {
      this.debugService.log('Cancelling 2nd initialization attempt');
      Pace.stop();
      return;
    } else {
      this.debugService.info('Loading Frank!Console', this.consoleState.init);
    }

    if (this.consoleState.init === appInitState.PRE_INIT) {
      this.consoleState.init = appInitState.INIT;
      this.serverInfoService.refresh();
      this.serverInfoService.serverInfo$.pipe(first()).subscribe({
        next: (data) => {
          this.serverInfo = data;

          this.appService.dtapStage = data['dtap.stage'];
          this.dtapStage = data['dtap.stage'];
          this.dtapSide = data['dtap.side'];
          // appService.userName = data["userName"];
          this.userName = data['userName'];
          this.authService.setLoggedIn(this.userName);

          this.consoleState.init = appInitState.POST_INIT;
          if (!this.router.url.includes('login')) {
            this.idle.watch();
            this.renderer.removeClass(document.body, 'gray-bg');
          }

          const serverTime = Date.parse(
            new Date(data.serverTime).toUTCString(),
          );
          const localTime = Date.parse(new Date().toUTCString());
          this.consoleState.timeOffset = serverTime - localTime;
          // TODO this doesnt work as serverTime gets converted to local time before getTimezoneOffset is called
          this.appConstants['timezoneOffset'] = 0;
          //this.appConstants['timezoneOffset'] = new Date(data.serverTime).getTimezoneOffset();

          const updateTime = (): void => {
            const serverDate = new Date();
            serverDate.setTime(
              serverDate.getTime() - this.consoleState.timeOffset,
            );
            this.serverTime = formatDate(
              serverDate,
              this.appConstants['console.dateFormat'] as string,
              this.locale,
            );
          };
          window.setInterval(updateTime, 1000);
          updateTime();

          this.appService.updateInstanceName(data.instance.name);

          const iafInfoElement =
            document.querySelector<HTMLElement>('.iaf-info');
          if (iafInfoElement)
            iafInfoElement.textContent = `${data.framework.name} ${data.framework.version}: ${data.instance.name} ${data.instance.version}`;

          this.appService.updateTitle(this.title.getTitle().split(' | ')[1]);

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
          this.checkIafVersions();
          this.initializeWarnings();
        },
        error: (error: HttpErrorResponse) => {
          // HTTP 5xx error
          if (error.status.toString().startsWith('5')) {
            this.router.navigate(['error']);
          }
        },
      });
      this.appService.getEnvironmentVariables().subscribe((data) => {
        if (data['Application Constants']) {
          this.appConstants = Object.assign(
            this.appConstants,
            data['Application Constants']['All'],
          ); //make FF!Application Constants default

          const idleTime =
            Number.parseInt(this.appConstants['console.idle.time'] as string) >
            0
              ? Number.parseInt(
                  this.appConstants['console.idle.time'] as string,
                )
              : 0;
          if (idleTime > 0) {
            const idleTimeout =
              Number.parseInt(
                this.appConstants['console.idle.timeout'] as string,
              ) > 0
                ? Number.parseInt(
                    this.appConstants['console.idle.timeout'] as string,
                  )
                : 0;
            this.idle.setIdle(idleTime);
            this.idle.setTimeout(idleTimeout);
          } else {
            this.idle.stop();
          }
          this.appService.updateDatabaseSchedulesEnabled(
            this.appConstants['loadDatabaseSchedules.active'] === 'true',
          );
          this.appService.triggerAppConstants();
        }
      });
    }
  }

  checkIafVersions(): void {
    /* Check FF version */
    console.log('Checking FF version with remote...');
    this.appService
      .getIafVersions(this.miscService.getUID(this.serverInfo!))
      .subscribe((response) => {
        this.serverInfo = null;
        if (!response || response.length === 0) return;

        const release = response[0]; //Not sure what ID to pick, smallest or latest?

        const newVersion =
          release.tag_name.slice(0, 1) == 'v'
            ? release.tag_name.slice(1)
            : release.tag_name;
        const currentVersion = this.appConstants[
          'application.version'
        ] as string;
        const version =
          this.miscService.compare_version(newVersion, currentVersion) || 0;
        console.log(
          `Comparing version: '${currentVersion}' with latest release: '${newVersion}'.`,
        );
        this.sessionService.remove('IAF-Release');

        if (+version > 0) {
          this.sessionService.set('IAF-Release', release);
          this.notificationService.add(
            'fa-exclamation-circle',
            'FF! update available!',
            false,
            () => {
              this.router.navigate(['iaf-update']);
            },
          );
        }
      });
  }

  initializeWarnings(): void {
    const startupErrorSubscription = this.appService.startupError$.subscribe(
      () => {
        this.startupError = this.appService.startupError;
      },
    );
    this._subscriptions.add(startupErrorSubscription);

    this.pollerService.add(
      'server/warnings',
      (data) => {
        const configurations = data as Record<string, MessageLog>;
        this.appService.updateAlerts([]); //Clear all old alerts

        configurations['All'] = {
          messages: configurations['messages'] as unknown as AdapterMessage[],
          errorStoreCount: configurations[
            'totalErrorStoreCount'
          ] as unknown as number,
          messageLevel: 'ERROR',
          serverTime: configurations['serverTime'] as unknown as number,
          uptime: configurations['serverTime'] as unknown as number,
        };
        delete configurations['messages'];
        delete configurations['totalErrorStoreCount'];
        delete configurations['serverTime'];
        delete configurations['uptime'];

        if (configurations['warnings']) {
          for (const warning of configurations[
            'warnings'
          ] as unknown as string[]) {
            this.appService.addWarning('', warning);
          }
        }

        for (const index in configurations) {
          const configuration = configurations[index];
          if (configuration.exception)
            this.appService.addException(index, configuration.exception);
          if (configuration.warnings) {
            for (const warning of configuration.warnings) {
              this.appService.addWarning(index, warning);
            }
          }

          configuration.messageLevel = 'INFO';
          for (const x in configuration.messages) {
            const level = configuration.messages[x].level;
            if (level == 'WARN' && configuration.messageLevel != 'ERROR')
              configuration.messageLevel = 'WARN';
            if (level == 'ERROR') configuration.messageLevel = 'ERROR';
          }
        }

        this.appService.updateMessageLog(configurations);
      },
      60_000,
    );

    this.initializeAdapters();
  }

  initializeAdapters(): void {
    //Get base information first, then update it with more details
    this.appService
      .getAdapters()
      .pipe(first())
      .subscribe((data: Record<string, Adapter>) => {
        this.finalizeStartup(data);
      });
  }

  pollerCallback(allAdapters: Record<string, Adapter>): void {
    let reloadedAdapters = false;
    const updatedAdapters: typeof this.appService.adapters = {};
    const deletedAdapters: string[] = [];

    for (const index in this.serializedRawAdapterData) {
      //Check if any old adapters should be removed
      if (!allAdapters[index]) {
        deletedAdapters.push(index);
        delete this.serializedRawAdapterData[index];
        this.debugService.log(`removing adapter [${index}]`);
      }
    }

    for (const adapterName in allAdapters) {
      //Add new adapter information
      const adapter = allAdapters[adapterName];

      const serializedAdapter = JSON.stringify(adapter);
      if (this.serializedRawAdapterData[adapterName] != serializedAdapter) {
        this.serializedRawAdapterData[adapterName] = serializedAdapter;

        adapter.status = 'started';

        for (const x in adapter.receivers) {
          const adapterReceiver = adapter.receivers[+x];
          if (adapterReceiver.state != 'started') adapter.status = 'warning';

          if (adapterReceiver.transactionalStores) {
            const store = adapterReceiver.transactionalStores['ERROR'];
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
          const pipe = adapter.pipes[+x];
          if (pipe.sender) {
            adapter.hasSender = true;
            if (pipe.hasMessageLog) {
              const count = Number.parseInt(pipe.messageLogCount ?? '');
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
                    let message = adapter.messages[adapter.messages.length -1];
                    if(message.level != "INFO")
                      adapter.status = 'warning';
                  }
        */
        if (adapter.state != 'started') {
          adapter.status = 'stopped';
        }

        if (!reloadedAdapters)
          reloadedAdapters = this.hasAdapterReloaded(adapter);

        if (adapterName.includes('/')) {
          updatedAdapters[adapterName] = adapter;
        } else {
          updatedAdapters[`${adapter.configuration}/${adapter.name}`] = adapter;
        }

        const selectedConfiguration =
          this.routeQueryParams.get('configuration');
        this.appService.updateAdapterSummary(
          selectedConfiguration ?? 'All',
          false,
        );
        this.updateAdapterNotifications(adapter);
      }
    }

    const oldAdapters = { ...this.appService.adapters };
    for (const index of deletedAdapters) {
      delete oldAdapters[index];
    }

    this.appService.updateAdapters({ ...oldAdapters, ...updatedAdapters });

    if (reloadedAdapters)
      this.toastService.success(
        'Reloaded',
        'Adapter(s) have successfully been reloaded!',
        { timeout: 3000 },
      );
  }

  finalizeStartup(data: Record<string, Adapter>): void {
    this.pollerCallback(data);

    setTimeout(() => {
      this.appService.updateLoading(false);
      this.loading = false;

      this.pollerService.add(
        'adapters?expanded=all',
        (data: unknown) => {
          this.pollerCallback(data as Record<string, Adapter>);
        },
        undefined,
      );
    });
  }

  updateAdapterNotifications(adapter: Adapter): void {
    let name = adapter.name;
    if (name.length > 20) name = `${name.slice(0, 17)}...`;
    if (adapter.started == true) {
      for (const x in adapter.receivers) {
        // TODO Receiver.started is not really a thing, maybe this should work differently?
        // @ts-expect-error Receiver.started does not exist
        if (adapter.receivers[+x].started == false) {
          this.notificationService.add(
            'fa-exclamation-circle',
            `Receiver '${name}' stopped!`,
            false,
            () => {
              this.router.navigate(['status'], { fragment: adapter.name });
            },
          );
        }
      }
    } else {
      this.notificationService.add(
        'fa-exclamation-circle',
        `Adapter '${name}' stopped!`,
        false,
        () => {
          this.router.navigate(['status'], { fragment: adapter.name });
        },
      );
    }
  }

  hasAdapterReloaded(adapter: Adapter): boolean {
    const oldAdapter =
      this.appService.adapters[`${adapter.configuration}/${adapter.name}`];
    return adapter.upSince > oldAdapter?.upSince;
  }

  openInfoModel(): void {
    this.modalService.open(
      InformationModalComponent,
      this.MODAL_OPTIONS_CLASSES,
    );
  }
}
