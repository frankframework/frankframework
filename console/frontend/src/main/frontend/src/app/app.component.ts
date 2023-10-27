import { Component, Inject, LOCALE_ID, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { Idle } from '@ng-idle/core';
import { Observable, Subscription } from 'rxjs';
import { Adapter, AppConstants, AppService, Configuration, ServerInfo } from './app.service';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
// @ts-ignore pace-js does not have types
import * as Pace from 'pace-js';
import { NotificationService } from './services/notification.service';
import { MiscService } from './services/misc.service';
import { DebugService } from './services/debug.service';
import { PollerService } from './services/poller.service';
import { AuthService } from './services/auth.service';
import { SessionService } from './services/session.service';
import { SweetalertService } from './services/sweetalert.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  loading = true;
  serverInfo: ServerInfo | null = null;
  loggedin = false;
  monitoring = false;
  config_database = false;
  dtapStage = "";
  dtapSide = "";
  serverTime = "";
  startupError: string | null = null;
  userName?: string;
  appConstants: AppConstants;
  routeData: Record<string, any> = {};

  private urlHash$!: Observable<string | null>;
  private routeQueryParams!: ParamMap;
  private _subscriptions = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private renderer: Renderer2,
    private authService: AuthService,
    private pollerService: PollerService,
    private notificationService: NotificationService,
    private miscService: MiscService,
    private sessionService: SessionService,
    private debugService: DebugService,
    private sweetAlertService: SweetalertService,
    private appService: AppService,
    private idle: Idle,
    @Inject(LOCALE_ID) private locale: string
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
  }

  ngOnInit() {
    Pace.start({
      ajax: false
    });

    this.urlHash$ = this.route.fragment;
    this.route.queryParamMap.subscribe(params => {
      if(this.router.url !== '/login'){
        this.renderer.addClass(document.body, 'gray-bg');
      } else {
        this.renderer.removeClass(document.body, 'gray-bg');
      };
      this.routeQueryParams = params;
    });
    this.route.data.subscribe((data) => {
      this.routeData = data;
    });

    /* state controller */
    this.authService.loggedin(); //Check if the user is logged in.

    $(".main").show();
    $(".loading").remove();
    /* state controller end */

    Pace.on("done", () => this.initializeFrankConsole());
    window.setTimeout(() => this.initializeFrankConsole(), 250);

    const idleStartSubscription = this.idle.onIdleStart.subscribe(() => {
      this.pollerService.getAll().changeInterval(this.appConstants["console.idle.pollerInterval"]);

      let idleTimeout = (parseInt(this.appConstants["console.idle.timeout"]) > 0) ? parseInt(this.appConstants["console.idle.timeout"]) : false;
      if (!idleTimeout) return;

      this.sweetAlertService.Warning({
        title: "Idle timer...",
        text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
        showConfirmButton: false,
        showCloseButton: true
      });
    });
    this._subscriptions.add(idleStartSubscription);

    const idleWarnSubscription = this.idle.onTimeoutWarning.subscribe(function (timeRemaining) {
      let minutes = Math.floor(timeRemaining / 60);
      let seconds = Math.round(timeRemaining % 60);
      if (minutes < 10) minutes = +"0" + minutes;
      if (seconds < 10) seconds = +"0" + seconds;
      let elm = $(".swal2-container").find(".idleTimer");
      elm.text(minutes + ":" + seconds);
    });
    this._subscriptions.add(idleWarnSubscription);

    const idleTimeoutSubscription = this.idle.onTimeout.subscribe(() => {
      this.sweetAlertService.Info({
        title: "Idle timer...",
        text: "You have been logged out due to inactivity.",
        showCloseButton: true
      });
      this.router.navigate(['logout']);
    });
    this._subscriptions.add(idleTimeoutSubscription);

    const idleEndSubscription = this.idle.onIdleEnd.subscribe(() => {
      let elm = $(".swal2-container").find(".swal2-close");
      elm.click();

      this.pollerService.getAll().changeInterval(this.appConstants["console.pollerInterval"]);
    });
    this._subscriptions.add(idleEndSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  initializeFrankConsole() {
    if (this.appConstants['init'] === -1) {
      this.appConstants['init'] = 0;
      this.debugService.log("Initializing Frank!Console");
    } else if (this.appConstants['init'] === 0) {
      this.debugService.log("Cancelling 2nd initialization attempt");
      Pace.stop();
      return;
    } else {
      this.debugService.info("Loading Frank!Console", this.appConstants['init']);
    }

    if (this.appConstants['init'] === 0) { //Only continue if the init state was -1
      this.appConstants['init'] = 1;
      this.appService.getServerInfo().subscribe({ next: (data) => {
        this.serverInfo = data;

        this.appConstants['init'] = 2;
        if (!(this.router.url.indexOf("login") >= 0)) {
          this.idle.watch();
          $("body").removeClass("gray-bg");
          $(".main").show();
          $(".loading").hide();
        }

        this.appService.dtapStage = data["dtap.stage"];
        this.dtapStage = data["dtap.stage"];
        this.dtapSide = data["dtap.side"];
        // appService.userName = data["userName"];
        this.userName = data["userName"];

        let serverTime = Date.parse(new Date(data.serverTime).toUTCString());
        let localTime = Date.parse(new Date().toUTCString());
        this.appConstants['timeOffset'] = serverTime - localTime;
        // TODO this doesnt work as serverTime gets converted to local time before getTimezoneOffset is called
        this.appConstants['timezoneOffset'] = 0;
        //this.appConstants['timezoneOffset'] = new Date(data.serverTime).getTimezoneOffset();

        const updateTime = () => {
          const serverDate = new Date();
          serverDate.setTime(serverDate.getTime() - this.appConstants['timeOffset']);
          this.serverTime = formatDate(serverDate, this.appConstants["console.dateFormat"], this.locale);
        }
        window.setInterval(updateTime, 1000);
        updateTime();

        this.appService.updateInstanceName(data.instance.name);
        $(".iaf-info").html(data.framework.name + " " + data.framework.version + ": " + data.instance.name + " " + data.instance.version);

        if (this.appService.dtapStage == "LOC") {
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
      }, error: (error: HttpErrorResponse) => {
        if (error.status == 500) {
          this.router.navigate(['error']);
        }
      }});
      this.appService.getEnvironmentVariables().subscribe((data) => {
        if (data["Application Constants"]) {
          this.appConstants = $.extend(this.appConstants, data["Application Constants"]["All"]); //make FF!Application Constants default

          let idleTime = (parseInt(this.appConstants["console.idle.time"]) > 0) ? parseInt(this.appConstants["console.idle.time"]) : 0;
          if (idleTime > 0) {
            let idleTimeout = (parseInt(this.appConstants["console.idle.timeout"]) > 0) ? parseInt(this.appConstants["console.idle.timeout"]) : 0;
            this.idle.setIdle(idleTime);
            this.idle.setTimeout(idleTimeout);
          }
          else {
            this.idle.stop();
          }
          this.appService.updateDatabaseSchedulesEnabled((this.appConstants["loadDatabaseSchedules.active"] === 'true'));
          this.appService.triggerAppConstants();
        }
      });
    }

    let token = sessionStorage.getItem('authToken');
    this.loggedin = (token != null && token != "null") ? true : false;
  }

  checkIafVersions(){
    /* Check IAF version */
    console.log("Checking IAF version with remote...");
    this.appService.getIafVersions(this.miscService.getUID(this.serverInfo!)).subscribe((response) => {
      this.serverInfo = null;
      if (!response || response.length === 0) return;

      const release = response[0]; //Not sure what ID to pick, smallest or latest?

      const newVersion = (release.tag_name.substr(0, 1) == "v") ? release.tag_name.substr(1) : release.tag_name;
      const currentVersion = this.appConstants["application.version"];
      const version = this.miscService.compare_version(newVersion, currentVersion) || 0;
      console.log("Comparing version: '" + currentVersion + "' with latest release: '" + newVersion + "'.");
      this.sessionService.remove("IAF-Release");

      if (+version > 0) {
        this.sessionService.set("IAF-Release", release);
        this.notificationService.add('fa-exclamation-circle', "IAF update available!", false, () => {
          this.router.navigate(['iaf-update']);
        });
      }
    });
  }

  initializeWarnings(){
    this.pollerService.add("server/warnings", (configurations) => {
      this.appService.updateAlerts([]); //Clear all old alerts

      configurations['All'] = { messages: configurations.messages };
      delete configurations.messages;

      configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
      delete configurations.totalErrorStoreCount;

      for (let x in configurations.warnings) {
        this.appService.addWarning('', configurations.warnings[x]);
      }

      for (const i in configurations) {
        let configuration = configurations[i];
        if (configuration.exception)
          this.appService.addException(i, configuration.exception);
        if (configuration.warnings) {
          for (const x in configuration.warnings) {
            this.appService.addWarning(i, configuration.warnings[x]);
          }
        }

        configuration.messageLevel = "INFO";
        for (const x in configuration.messages) {
          let level = configuration.messages[x].level;
          if (level == "WARN" && configuration.messageLevel != "ERROR")
            configuration.messageLevel = "WARN";
          if (level == "ERROR")
            configuration.messageLevel = "ERROR";
        }
      }

      this.appService.updateMessageLog(configurations);

      const startupErrorSubscription = this.appService.startupError$.subscribe(() => {
        this.startupError = this.appService.startupError;
      });
      this._subscriptions.add(startupErrorSubscription);
    }, true, 60000);

    let raw_adapter_data: Record<string, string> = {};
    let pollerCallback = (allAdapters: Record<string, Adapter>) => {
      for (const i in raw_adapter_data) { //Check if any old adapters should be removed
        if (!allAdapters[i]) {
          delete raw_adapter_data[i];
          delete this.appService.adapters[i];
          this.debugService.log("removed adapter [" + i + "]");
        }
      }
      for (const adapterName in allAdapters) { //Add new adapter information
        let adapter = allAdapters[adapterName];

        if (raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
          raw_adapter_data[adapter.name] = JSON.stringify(adapter);

          adapter.status = "started";

          for (const x in adapter.receivers) {
            let adapterReceiver = adapter.receivers[+x];
            if (adapterReceiver.state != 'started')
              adapter.status = 'warning';

            if (adapterReceiver.transactionalStores) {
              let store = adapterReceiver.transactionalStores["ERROR"];
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
            let pipe = adapter.pipes[+x];
            if (pipe.sender) {
              adapter.hasSender = true;
              if (pipe.hasMessageLog) {
                let count = parseInt(pipe.messageLogCount || '');
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
          if (adapter.state != "started") {
            adapter.status = "stopped";
          }

          this.appService.adapters[adapter.name] = adapter;

          this.appService.updateAdapterSummary(this.routeQueryParams);
          this.scrollToAdapter();
          this.updateAdapterNotifications(adapter);
        }
      }
      this.appService.updateAdapters(this.appService.adapters);
    };

    //Get base information first, then update it with more details
    this.appService.getAdapters().subscribe((data: Record<string, Adapter>) => pollerCallback(data));
    window.setTimeout(() => {
      this.pollerService.add("adapters?expanded=all", (data: Record<string, Adapter>) => { pollerCallback(data) }, true);
      this.appService.updateLoading(false);
      this.loading = false;
    }, 3000);
  }

  scrollToAdapter(){
    this.urlHash$.subscribe((hash) => {
      if (this.router.url == "/status" && hash) {
        let el = $("#" + hash);
        if (el && el[0]) {
          el[0].scrollIntoView();
        }
      }
    });
  }

  updateAdapterNotifications(adapter: Adapter){
    let name = adapter.name;
    if (name.length > 20)
      name = name.substring(0, 17) + "...";
    if (adapter.started == true) {
      for (const x in adapter.receivers) {
        // TODO Receiver.started is not really a thing, maybe this should work differently?
        // @ts-ignore
        if (adapter.receivers[+x].started == false) {
          this.notificationService.add('fa-exclamation-circle', "Receiver '" + name + "' stopped!", false, () => {
            this.router.navigate(['status'], { fragment: adapter.name });
          });
        }
      }
    }
    else {
      this.notificationService.add('fa-exclamation-circle', "Adapter '" + name + "' stopped!", false, () => {
        this.router.navigate(['status'], { fragment: adapter.name });
      });
    }
  }

  openInfoModel() {
    // this.$uibModal.open({
    //   templateUrl: 'js/app/components/pages/information-modal/information.html',
    //   //            size: 'sm',
    //   controller: 'InformationCtrl',
    // });
  };

  sendFeedback(rating?: number) {
    if (!this.appConstants["console.feedbackURL"])
      return;

    $(".rating i").each(function (i, e) {
      $(e).addClass("fa-star-o").removeClass("fa-star");
    });
    // this.$uibModal.open({
    //   templateUrl: 'angularjs/app/components/pages/feedback-modal/feedback.html',
    //   controller: 'FeedbackCtrl',
    //   resolve: { rating: function () { return rating; } },
    // });
  };
}
