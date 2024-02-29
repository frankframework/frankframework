import {
  Component,
  Inject,
  LOCALE_ID,
  OnDestroy,
  OnInit,
  Renderer2,
} from '@angular/core';
import { Idle } from '@ng-idle/core';
import { Observable, Subscription, filter, first } from 'rxjs';
import {
  Adapter,
  AdapterMessage,
  AppConstants,
  AppService,
  MessageLog,
  ServerInfo,
} from './app.service';
import {
  ActivatedRoute,
  Data,
  NavigationEnd,
  NavigationSkipped,
  NavigationStart,
  ParamMap,
  Router,
  convertToParamMap,
} from '@angular/router';
import { ViewportScroller, formatDate } from '@angular/common';
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
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { InformationModalComponent } from './components/pages/information-modal/information-modal.component';
import { FeedbackModalComponent } from './components/pages/feedback-modal/feedback-modal.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit, OnDestroy {
  loading = true;
  serverInfo: ServerInfo | null = null;
  loggedin = false;
  monitoring = false;
  config_database = false;
  dtapStage = '';
  dtapSide = '';
  serverTime = '';
  startupError: string | null = null;
  userName?: string;
  appConstants: AppConstants;
  routeData: Data = {};
  routeQueryParams: ParamMap = convertToParamMap({});
  isLoginView: boolean = false;

  private urlHash$!: Observable<string | null>;
  private _subscriptions = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private renderer: Renderer2,
    private title: Title,
    private viewportScroller: ViewportScroller,
    private authService: AuthService,
    private pollerService: PollerService,
    private notificationService: NotificationService,
    private miscService: MiscService,
    private sessionService: SessionService,
    private debugService: DebugService,
    private sweetAlertService: SweetalertService,
    private appService: AppService,
    private idle: Idle,
    private modalService: NgbModal,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
  }

  ngOnInit(): void {
    Pace.start({
      ajax: false,
    });

    this.urlHash$ = this.route.fragment;
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
          this.routeQueryParams = childRoute.snapshot.queryParamMap;
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

    /* state controller */
    this.authService.loggedin(); //Check if the user is logged in.

    $('.main').show();
    $('.loading').remove();
    /* state controller end */

    Pace.on('done', () => this.initializeFrankConsole());
    window.setTimeout(() => this.initializeFrankConsole(), 250);

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
      function (timeRemaining) {
        let minutes = Math.floor(timeRemaining / 60);
        let seconds = Math.round(timeRemaining % 60);
        if (minutes < 10) minutes = +'0' + minutes;
        if (seconds < 10) seconds = +'0' + seconds;
        const elm = $('.swal2-container').find('.idleTimer');
        elm.text(`${minutes}:${seconds}`);
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
      const elm = $('.swal2-container').find('.swal2-close');
      elm.click();

      this.pollerService
        .getAll()
        .changeInterval(this.appConstants['console.pollerInterval'] as number);
    });
    this._subscriptions.add(idleEndSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  initializeFrankConsole(): void {
    if (this.appConstants['init'] === -1) {
      this.appConstants['init'] = 0;
      this.debugService.log('Initializing Frank!Console');
    } else if (this.appConstants['init'] === 0) {
      this.debugService.log('Cancelling 2nd initialization attempt');
      Pace.stop();
      return;
    } else {
      this.debugService.info(
        'Loading Frank!Console',
        this.appConstants['init'],
      );
    }

    if (this.appConstants['init'] === 0) {
      //Only continue if the init state was -1
      this.appConstants['init'] = 1;
      this.appService.getServerInfo().subscribe({
        next: (data) => {
          this.serverInfo = data;

          this.appConstants['init'] = 2;
          if (!this.router.url.includes('login')) {
            this.idle.watch();
            $('body').removeClass('gray-bg');
            $('.main').show();
            $('.loading').hide();
          }

          this.appService.dtapStage = data['dtap.stage'];
          this.dtapStage = data['dtap.stage'];
          this.dtapSide = data['dtap.side'];
          // appService.userName = data["userName"];
          this.userName = data['userName'];

          const serverTime = Date.parse(
            new Date(data.serverTime).toUTCString(),
          );
          const localTime = Date.parse(new Date().toUTCString());
          this.appConstants['timeOffset'] = serverTime - localTime;
          // TODO this doesnt work as serverTime gets converted to local time before getTimezoneOffset is called
          this.appConstants['timezoneOffset'] = 0;
          //this.appConstants['timezoneOffset'] = new Date(data.serverTime).getTimezoneOffset();

          const updateTime = (): void => {
            const serverDate = new Date();
            serverDate.setTime(
              serverDate.getTime() -
                (this.appConstants['timeOffset'] as number),
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
          $('.iaf-info').html(
            `${data.framework.name} ${data.framework.version}: ${data.instance.name} ${data.instance.version}`,
          );
          this.appService.updateTitle(this.title.getTitle().split(' | ')[1]);

          if (this.appService.dtapStage == 'LOC') {
            this.debugService.setLevel(3);
          }

          //Was it able to retrieve the serverinfo without logging in?
          if (!this.loggedin) {
            this.idle.setTimeout(0);
          }

          this.appService.getConfigurations().subscribe((data) => {
            this.appService.updateConfigurations(data);
          });
          this.checkIafVersions();
          this.initializeWarnings();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status == 500) {
            this.router.navigate(['error']);
          }
        },
      });
      this.appService.getEnvironmentVariables().subscribe((data) => {
        if (data['Application Constants']) {
          this.appConstants = $.extend(
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

    const token = sessionStorage.getItem('authToken');
    this.loggedin = token != null && token != 'null' ? true : false;
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
            'FF update available!',
            false,
            () => {
              this.router.navigate(['iaf-update']);
            },
          );
        }
      });
  }

  initializeWarnings(): void {
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
        };
        delete configurations['messages'];
        delete configurations['totalErrorStoreCount'];

        for (const warning of configurations[
          'warnings'
        ] as unknown as string[]) {
          this.appService.addWarning('', warning);
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

        const startupErrorSubscription =
          this.appService.startupError$.subscribe(() => {
            this.startupError = this.appService.startupError;
          });
        this._subscriptions.add(startupErrorSubscription);
      },
      true,
      60_000,
    );

    const raw_adapter_data: Record<string, string> = {};
    const pollerCallback = (allAdapters: Record<string, Adapter>): void => {
      for (const index in raw_adapter_data) {
        //Check if any old adapters should be removed
        if (!allAdapters[index]) {
          delete raw_adapter_data[index];
          delete this.appService.adapters[index];
          this.debugService.log(`removed adapter [${index}]`);
        }
      }
      for (const adapterName in allAdapters) {
        //Add new adapter information
        const adapter = allAdapters[adapterName];

        if (raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
          raw_adapter_data[adapter.name] = JSON.stringify(adapter);

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
                const count = Number.parseInt(pipe.messageLogCount || '');
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

          this.appService.adapters[`${adapter.configuration}/${adapter.name}`] =
            adapter;

          const selectedConfiguration =
            this.routeQueryParams.get('configuration');
          this.appService.updateAdapterSummary(
            selectedConfiguration ?? 'All',
            false,
          );
          this.updateAdapterNotifications(adapter);
        }
      }
      this.appService.updateAdapters(this.appService.adapters);
    };

    //Get base information first, then update it with more details
    this.appService
      .getAdapters()
      .pipe(first())
      .subscribe((data: Record<string, Adapter>) => {
        pollerCallback(data);
      });
    window.setTimeout(() => {
      this.pollerService.add(
        'adapters?expanded=all',
        (data: unknown) => {
          pollerCallback(data as Record<string, Adapter>);
        },
        true,
      );
      this.appService.updateLoading(false);
      this.loading = false;
      this.scrollToAdapter();
    }, 3000);
  }

  scrollToAdapter(): void {
    this.urlHash$.subscribe((hash) => {
      if (this.router.url.startsWith('/status') && hash && hash !== '') {
        /* let el = $("#" + hash);
        if (el && el[0]) {
          el[0].scrollIntoView();
        } */
        this.viewportScroller.scrollToAnchor(hash);
      }
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

  openInfoModel(): void {
    this.modalService.open(InformationModalComponent /* { size: 'sm' } */);
  }

  sendFeedback(rating?: number): void {
    if (!this.appConstants['console.feedbackURL']) return;

    $('.rating i').each(function (index, element) {
      $(element).addClass('fa-star-o').removeClass('fa-star');
    });
    const modalReference = this.modalService.open(FeedbackModalComponent);
    modalReference.componentInstance.rating = rating;
  }
}
