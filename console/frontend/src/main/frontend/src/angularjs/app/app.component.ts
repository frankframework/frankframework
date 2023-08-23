
import { ApiService, AuthService, DebugService, HooksService, MiscService, NotificationService, PollerService, SessionService, SweetAlertService } from 'src/app/services.types';
import { Pace } from '../deps';
import { AppConstants, appModule } from "./app.module";
import { Adapter, AppService, Configuration } from './app.service';
import { StateService } from '@uirouter/angularjs';

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

class AppController {

  loading = true;
  serverInfo: Record<string, any> | null = {};
  loggedin = false;
  monitoring = false;
  config_database = false;
  dtapStage = "";
  dtapSide = "";
  serverTime = "";
  startupError: string | null = null;
  userName?: string;

  constructor(
    private $scope: angular.IScope,
    private $rootScope: angular.IRootScopeService,
    private authService: AuthService,
    private appConstants: AppConstants,
    private Api: ApiService,
    private Hooks: HooksService,
    private $state: StateService,
    private $location: angular.ILocationService,
    private Poller: PollerService,
    private Notification: NotificationService,
    private dateFilter: angular.IFilterDate,
    private $interval: angular.IIntervalService,
    private Idle: angular.idle.IIdleService,
    private $http: angular.IHttpService,
    private Misc: MiscService,
    private $uibModal: angular.ui.bootstrap.IModalService,
    private Session: SessionService,
    private Debug: DebugService,
    private SweetAlert: SweetAlertService,
    private $timeout: angular.ITimeoutService,
    private appService: AppService
  ) {}

  $onInit() {
    /* state controller */
    this.authService.loggedin(); //Check if the user is logged in.

    angular.element(".main").show();
    angular.element(".loading").remove();
    /* state controller end */

    Pace.on("done", () => this.initializeFrankConsole());
    this.$scope.$on('initializeFrankConsole', () => this.initializeFrankConsole());
    this.$timeout(() => this.initializeFrankConsole(), 250);

    this.$scope.$on('IdleStart', () => {
      this.Poller.getAll().changeInterval(this.appConstants["console.idle.pollerInterval"]);

      var idleTimeout = (parseInt(this.appConstants["console.idle.timeout"]) > 0) ? parseInt(this.appConstants["console.idle.timeout"]) : false;
      if (!idleTimeout) return;

      this.SweetAlert.Warning({
        title: "Idle timer...",
        text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
        showConfirmButton: false,
        showCloseButton: true
      });
    });

    this.$scope.$on('IdleWarn', function (e, time) {
      var minutes = Math.floor(time / 60);
      var seconds = Math.round(time % 60);
      if (minutes < 10) minutes = +"0" + minutes;
      if (seconds < 10) seconds = +"0" + seconds;
      var elm = angular.element(".swal2-container").find(".idleTimer");
      elm.text(minutes + ":" + seconds);
    });

    this.$scope.$on('IdleTimeout', () => {
      this.SweetAlert.Info({
        title: "Idle timer...",
        text: "You have been logged out due to inactivity.",
        showCloseButton: true
      });
      this.$location.path("logout");
    });

    this.$scope.$on('IdleEnd', () => {
      var elm = angular.element(".swal2-container").find(".swal2-close");
      elm.click();

      this.Poller.getAll().changeInterval(this.appConstants["console.pollerInterval"]);
    });

    this.Hooks.register("init:once", () => {
      /* Check IAF version */
      console.log("Checking IAF version with remote...");
      this.$http.get<IAFRelease[]>("https://ibissource.org/iaf/releases/?q=" + this.Misc.getUID(this.serverInfo!)).then((response) => {
        if (!response || !response.data) return;
        var release = response.data[0]; //Not sure what ID to pick, smallest or latest?

        var newVersion = (release.tag_name.substr(0, 1) == "v") ? release.tag_name.substr(1) : release.tag_name;
        var currentVersion = this.appConstants["application.version"];
        var version = this.Misc.compare_version(newVersion, currentVersion);
        console.log("Comparing version: '" + currentVersion + "' with latest release: '" + newVersion + "'.");
        this.Session.remove("IAF-Release");

        if (+version > 0) {
          this.Session.set("IAF-Release", release);
          this.Notification.add('fa-exclamation-circle', "IAF update available!", false, () => {
            this.$location.path("iaf-update");
          });
        }
        this.serverInfo = null;
      }).catch((error) => {
        this.Debug.error("An error occured while comparing IAF versions", error);
        this.serverInfo = null;
      });

      this.Poller.add("server/warnings", (configurations) => {
        this.appService.updateAlerts([]); //Clear all old alerts

        configurations['All'] = { messages: configurations.messages };
        delete configurations.messages;

        configurations['All'].errorStoreCount = configurations.totalErrorStoreCount;
        delete configurations.totalErrorStoreCount;

        for (let x in configurations.warnings) {
          this.addWarning('', configurations.warnings[x]);
        }

        for (const i in configurations) {
          var configuration = configurations[i];
          if (configuration.exception)
            this.addException(i, configuration.exception);
          if (configuration.warnings) {
            for (const x in configuration.warnings) {
              this.addWarning(i, configuration.warnings[x]);
            }
          }

          configuration.messageLevel = "INFO";
          for (const x in configuration.messages) {
            var level = configuration.messages[x].level;
            if (level == "WARN" && configuration.messageLevel != "ERROR")
              configuration.messageLevel = "WARN";
            if (level == "ERROR")
              configuration.messageLevel = "ERROR";
          }
        }

        this.appService.updateMessageLog(configurations);

        this.$scope.$on('startupError', () => {
          this.startupError = this.appService.startupError;
        })
      }, true, 60000);

      var raw_adapter_data: Record<string, string> = {};
      var pollerCallback = (allAdapters: Record<string, Adapter>) => {
        for (const i in raw_adapter_data) { //Check if any old adapters should be removed
          if (!allAdapters[i]) {
            delete raw_adapter_data[i];
            delete this.appService.adapters[i];
            this.Debug.log("removed adapter [" + i + "]");
          }
        }
        for (const adapterName in allAdapters) { //Add new adapter information
          var adapter = allAdapters[adapterName];

          if (raw_adapter_data[adapter.name] != JSON.stringify(adapter)) {
            raw_adapter_data[adapter.name] = JSON.stringify(adapter);

            adapter.status = "started";

            for (const x in adapter.receivers) {
              var adapterReceiver = adapter.receivers[+x];
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
                        var message = adapter.messages[adapter.messages.length -1];
                        if(message.level != "INFO")
                          adapter.status = 'warning';
                      }
            */
            if (adapter.state != "started") {
              adapter.status = "stopped";
            }

            this.appService.adapters[adapter.name] = adapter;

            this.appService.updateAdapterSummary();
            this.Hooks.call("adapterUpdated", adapter);
            //					$scope.$broadcast('adapterUpdated', adapter);
          }
        }
        this.appService.updateAdapters(this.appService.adapters);
      };

      //Get base information first, then update it with more details
      this.Api.Get("adapters", (data: Record<string, Adapter>) => pollerCallback(data));
      this.$timeout(() => {
        this.Poller.add("adapters?expanded=all", (data: Record<string, Adapter>) => { pollerCallback(data) }, true);
        this.$scope.$broadcast('loading', false);
      }, 3000);
    });

    this.Hooks.register("adapterUpdated:once", () => {
      if (this.$location.path() == "/status" && this.$location.hash()) {
        var el = angular.element("#" + this.$location.hash());
        if (el && el[0]) {
          el[0].scrollIntoView();
        }
      }
    });

    this.Hooks.register("adapterUpdated", (adapter: Adapter) => {
      var name = adapter.name;
      if (name.length > 20)
        name = name.substring(0, 17) + "...";
      if (adapter.started == true) {
        for (const x in adapter.receivers) {
          // TODO Receiver.started is not really a thing, maybe this should work differently?
          /* if (adapter.receivers[+x].started == false) {
            this.Notification.add('fa-exclamation-circle', "Receiver '" + name + "' stopped!", false, () => {
              this.$location.path("status");
              this.$location.hash(adapter.name);
            });
          } */
        }
      }
      else {
        this.Notification.add('fa-exclamation-circle', "Adapter '" + name + "' stopped!", false, () => {
          this.$location.path("status");
          this.$location.hash(adapter.name);
        });
      }
    });
  }

  initializeFrankConsole() {
    if (this.appConstants['init'] === -1) {
      this.appConstants['init'] = 0;
      this.Debug.log("Initializing Frank!Console");
    } else if (this.appConstants['init'] === 0) {
      this.Debug.log("Cancelling 2nd initialization attempt");
      Pace.stop();
      return;
    } else {
      this.Debug.info("Loading Frank!Console", this.appConstants['init']);
    }

    if (this.appConstants['init'] === 0) { //Only continue if the init state was -1
      this.appConstants['init'] = 1;
      this.Api.Get("server/info", (data) => {
        this.serverInfo = data;

        this.appConstants['init'] = 2;
        if (!(this.$location.path().indexOf("login") >= 0)) {
          this.Idle.watch();
          angular.element("body").removeClass("gray-bg");
          angular.element(".main").show();
          angular.element(".loading").hide();
        }

        this.appService.dtapStage = data["dtap.stage"];
        this.dtapStage = data["dtap.stage"];
        this.dtapSide = data["dtap.side"];
        // appService.userName = data["userName"];
        this.userName = data["userName"];

        var serverTime = Date.parse(new Date(data.serverTime).toUTCString());
        var localTime = Date.parse(new Date().toUTCString());
        this.appConstants['timeOffset'] = serverTime - localTime;

        const updateTime = () => {
          var serverDate = new Date();
          serverDate.setTime(serverDate.getTime() - this.appConstants['timeOffset']);
          this.serverTime = this.dateFilter(serverDate, this.appConstants["console.dateFormat"]);
        }
        this.$interval(updateTime, 1000);
        updateTime();

        this.appService.updateInstanceName(data.instance.name);
        angular.element(".iaf-info").html(data.framework.name + " " + data.framework.version + ": " + data.instance.name + " " + data.instance.version);

        if (this.appService.dtapStage == "LOC") {
          this.Debug.setLevel(3);
        }

        //Was it able to retrieve the serverinfo without logging in?
        if (!this.loggedin) {
          this.Idle.setTimeout(0);
        }

        this.Api.Get("server/configurations", (data: Configuration[]) => {
          this.appService.updateConfigurations(data);
        });
        this.Hooks.call("init", false);
      }, (message, statusCode, statusText) => {
        if (statusCode == 500) {
          this.$state.go("pages.errorpage");
        }
      });
      this.Api.Get("environmentvariables", (data) => {
        if (data["Application Constants"]) {
          this.appConstants = $.extend(this.appConstants, data["Application Constants"]["All"]); //make FF!Application Constants default

          var idleTime = (parseInt(this.appConstants["console.idle.time"]) > 0) ? parseInt(this.appConstants["console.idle.time"]) : 0;
          if (idleTime > 0) {
            var idleTimeout = (parseInt(this.appConstants["console.idle.timeout"]) > 0) ? parseInt(this.appConstants["console.idle.timeout"]) : 0;
            this.Idle.setIdle(idleTime);
            this.Idle.setTimeout(idleTimeout);
          }
          else {
            this.Idle.unwatch();
          }
          this.appService.updateDatabaseSchedulesEnabled((this.appConstants["loadDatabaseSchedules.active"] === 'true'));
          this.$rootScope.$broadcast('appConstants');
        }
      });
    }

    var token = sessionStorage.getItem('authToken');
    this.loggedin = (token != null && token != "null") ? true : false;
  }

  reloadRoute() {
    this.$state.reload();
  };

  addAlert(type: string, configuration: string, message: string) {
    var line = message.match(/line \[(\d+)\]/);
    var isValidationAlert = message.indexOf("Validation") !== -1;
    var link = (line && !isValidationAlert) ? { name: configuration, '#': 'L' + line[1] } : undefined;
    this.appService.alerts.push({
      link: link,
      type: type,
      configuration: configuration,
      message: message
    });
    this.appService.updateAlerts(this.appService.alerts);
  };
  addWarning(configuration: string, message: string) {
    this.addAlert("warning", configuration, message);
  };
  addException(configuration: string, message: string) {
    this.addAlert("danger", configuration, message);
  };

  openInfoModel() {
    this.$uibModal.open({
      templateUrl: 'js/app/components/pages/information-modal/information.html',
      //            size: 'sm',
      controller: 'InformationCtrl',
    });
  };

  sendFeedback(rating: number) {
    if (!this.appConstants["console.feedbackURL"])
      return;

    $(".rating i").each(function (i, e) {
      $(e).addClass("fa-star-o").removeClass("fa-star");
    });
    this.$uibModal.open({
      templateUrl: 'angularjs/app/components/pages/feedback-modal/feedback.html',
      controller: 'FeedbackCtrl',
      resolve: { rating: function () { return rating; } },
    });
  };
}

appModule.component('app', {
  controller: ['$scope', '$rootScope', 'authService', 'appConstants', 'Api', 'Hooks', '$state', '$location', 'Poller', 'Notification', 'dateFilter', '$interval', 'Idle', '$http', 'Misc', '$uibModal', 'Session', 'Debug', 'SweetAlert', '$timeout', 'appService', AppController],
  templateUrl: 'angularjs/app/app.component.html'
});
