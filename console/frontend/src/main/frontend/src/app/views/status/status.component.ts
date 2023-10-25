import { ViewportScroller } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ConfigurationFilter } from 'src/app/pipes/configuration-filter.pipe';
import { StatusService } from './status.service';
import { Adapter, AdapterStatus, Alert, AppService, Configuration, MessageLog, MessageSummary, Receiver, Summary } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { PollerService } from 'src/app/services/poller.service';

type Filter = Record<AdapterStatus, boolean>;

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit, OnDestroy {
  filter: Filter = {
    started: true,
    stopped: true,
    warning: true
  };
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};
  adapterName = "";
  searchText = "";
  selectedConfiguration = "All";
  reloading = false;
  configurationFlowDiagram: string | null = null;
  isConfigStubbed: Record<string, boolean> = {};
  isConfigReloading: Record<string, boolean> = {};
  msgBoxExpanded = false;
  adapterShowContent: Record<keyof typeof this.adapters, boolean> = {}

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
  alerts: Alert[] = [];
  messageLog: Record<string, MessageLog> = {};

  // functions
  getProcessStateIcon = this.appService.getProcessStateIcon;
  getProcessStateIconColor = this.appService.getProcessStateIconColor;

  private routeQueryParams!: ParamMap;
  private _subscriptions = new Subscription();

  constructor(
    // private Api: ApiService,
    private Poller: PollerService,
    private Misc: MiscService,
    private viewportScroller: ViewportScroller,
    private route: ActivatedRoute,
    private router: Router,
    private statusService: StatusService,
    private appService: AppService
  ) { }

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      this.routeQueryParams = params;
      this.route.fragment.subscribe(fragment => {
        const hash = fragment ?? ""; // let hash = this.$location.hash();
        this.adapterName = params.get("adapter") ?? "";
        if (this.adapterName == "" && hash != "") { //If the adapter param hasn't explicitly been set
          this.adapterName = hash;
        } else {
          this.router.navigate([], { relativeTo: this.route, fragment: hash }); // this.$location.hash(this.adapterName);
        }
      });

      const filterParam = params.get("filter");
      if (filterParam && filterParam != "") {
        const filter = filterParam.split("+");
        for (const f in filter) {
          this.filter[f as keyof Filter] = (filter.indexOf(f) > -1);
        }
      }
      const searchParam = params.get("search");
      if (searchParam && searchParam != "") {
        this.searchText = searchParam;
      }

      const configurationParam = params.get("configuration");
      if (configurationParam && configurationParam != "All")
        this.changeConfiguration(configurationParam);
    });

    this.updateConfigurationFlowDiagram(this.selectedConfiguration);
    this.appService.appConstants$.subscribe(() => {
      this.updateConfigurationFlowDiagram(this.selectedConfiguration);
    });

    this.check4StubbedConfigs();
    this.adapterSummary = this.appService.adapterSummary;
    this.receiverSummary = this.appService.receiverSummary;
    this.messageSummary = this.appService.messageSummary;
    this.alerts = this.appService.alerts;
    this.messageLog = this.appService.messageLog;
    this.adapters = this.appService.adapters;
    const configurationsSubscription = this.appService.configurations$.subscribe(() => this.check4StubbedConfigs());
    this._subscriptions.add(configurationsSubscription);
    const summariesSubscription = this.appService.summaries$.subscribe(() => {
      this.adapterSummary = this.appService.adapterSummary;
      this.receiverSummary = this.appService.receiverSummary;
      this.messageSummary = this.appService.messageSummary;
    });
    this._subscriptions.add(summariesSubscription);
    const alertsSubscription = this.appService.alerts$.subscribe(() => { this.alerts = [...this.appService.alerts]; });
    this._subscriptions.add(alertsSubscription);
    const messageLogSubscription = this.appService.messageLog$.subscribe(() => { this.messageLog = {...this.appService.messageLog}; });
    this._subscriptions.add(messageLogSubscription);
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.adapters = {...this.appService.adapters};
      this.updateAdapterShownContent();
    });
    this._subscriptions.add(adaptersSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  applyFilter(filter: Filter) {
    this.filter = filter;
    this.updateQueryParams();
  };

  showContent(adapter: Adapter) {
    return this.adapterShowContent[adapter.name];
  }

  updateQueryParams() {
    let filterStr = [];
    for (const f in this.filter) {
      if (this.filter[f as keyof Filter])
        filterStr.push(f);
    }
    let transitionObj: Record<string, string> = {};
    transitionObj["filter"] = filterStr.join("+");
    if (this.selectedConfiguration != "All")
      transitionObj["configuration"] = this.selectedConfiguration;
    if (this.searchText.length > 0)
      transitionObj["search"] = this.searchText;

    this.router.navigate(['status'], { queryParams: transitionObj, preserveFragment: true });
  };

  collapseAll() {
    Object.keys(this.adapters)
      .forEach(adapter => this.adapterShowContent[adapter] = false);
  }

  expandAll() {
    Object.keys(this.adapters)
      .forEach(adapter => this.adapterShowContent[adapter] = true);
  };
  stopAll() {
    this.Api.Put("adapters", { "action": "stop", "adapters": this.getCompiledAdapterList() });
  };
  startAll() {
    this.Api.Put("adapters", { "action": "start", "adapters": this.getCompiledAdapterList() });
  };
  reloadConfiguration() {
    if (this.selectedConfiguration == "All") return;

    this.isConfigReloading[this.selectedConfiguration] = true;

    this.Poller.getAll().stop();
    this.Api.Put("configurations/" + this.selectedConfiguration, { "action": "reload" }, () => {
      this.startPollingForConfigurationStateChanges(() => {
        this.Poller.getAll().start();
      });
    });
  };
  fullReload() {
    this.reloading = true;
    this.Poller.getAll().stop();
    this.Api.Put("configurations", { "action": "reload" }, () => {
      this.reloading = false;
      this.startPollingForConfigurationStateChanges(() => {
        this.Poller.getAll().start();
      });
    });
  };

  startPollingForConfigurationStateChanges(callback?: () => void) {
    this.Poller.add("server/configurations", (configurations) => {
      this.appService.updateConfigurations(configurations);

      let ready = true;
      for (let i in configurations) {
        let config = configurations[i];
        //When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
        if (config.state != "STARTED" && !(config.state == "STOPPED" && config.exception != null)) {
          ready = false;
          break;
        }
      }
      if (ready) { //Remove poller once all states are STARTED
        this.Poller.remove("server/configurations");
        if (callback != null && typeof callback == "function") callback();
      }
    }, true);
  }

  showReferences() {
    window.open(this.configurationFlowDiagram!);
  }

  updateConfigurationFlowDiagram(configurationName: string) {
    let flowUrl: string;
    if (configurationName == "All") {
      flowUrl = "?flow=true";
    } else {
      flowUrl = configurationName + "/flow";
    }

    this.statusService.getConfigurationFlowDiagram(flowUrl).subscribe(({ data, url }) => {
      let status = (data && data.status) ? data.status : 204;
      if (status == 200) {
        this.configurationFlowDiagram = url;
      }
    });
  }

  check4StubbedConfigs() {
    this.configurations = this.appService.configurations;
    for (let i in this.appService.configurations) {
      let config = this.appService.configurations[i];
      this.isConfigStubbed[config.name] = config.stubbed;
      this.isConfigReloading[config.name] = config.state == "STARTING" || config.state == "STOPPING"; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
    }
  }

  // Commented out in template, so unused
  closeAlert(index: number) {
    this.appService.alerts.splice(index, 1);
    this.appService.updateAlerts(this.appService.alerts);
  }

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    this.appService.updateAdapterSummary(this.routeQueryParams, name);
    this.updateQueryParams();
    this.updateConfigurationFlowDiagram(name);
  }

  getTransactionalStores(receiver: Receiver) {
    return Object.values(receiver.transactionalStores);
  }

  getMessageLog(selectedConfiguration: string){
    return this.messageLog[selectedConfiguration].messages ?? [];
  }

  startAdapter(adapter: Adapter) {
    adapter.state = 'starting';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name), { "action": "start" });
  }
  stopAdapter(adapter: Adapter) {
    adapter.state = 'stopping';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name), { "action": "stop" });
  }
  startReceiver(adapter: Adapter, receiver: Receiver) {
    receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "start" });
  }
  stopReceiver(adapter: Adapter, receiver: Receiver) {
    receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "stop" });
  }
  addThread(adapter: Adapter, receiver: Receiver) {
    receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "incthread" });
  }
  removeThread(adapter: Adapter, receiver: Receiver) {
    receiver.state = 'loading';
    this.Api.Put("configurations/" + adapter.configuration + "/adapters/" + this.Misc.escapeURL(adapter.name) + "/receivers/" + this.Misc.escapeURL(receiver.name), { "action": "decthread" });
  }

  private getCompiledAdapterList() {
    const compiledAdapterList: string[] = [];
    // let adapters = $filter('configurationFilter')($scope.adapters, $scope);
    const adapters = ConfigurationFilter(this.adapters, this.selectedConfiguration, this.filter);
    for (const adapter in adapters) {
      const configuration = adapters[adapter].configuration;
      compiledAdapterList.push(configuration + "/" + adapter);
    }
  }

  private determineShowContent(adapter: Adapter) {
    if (adapter.status == "stopped") {
      return true;
    } else if (this.adapterName != "" && adapter.name == this.adapterName) {
      this.viewportScroller.scrollToAnchor(this.adapterName); // this.$anchorScroll();
      return true;
    } else {
      return false;
    }
  };

  private updateAdapterShownContent() {
    for (const adapter in this.adapters) {
      if (!this.adapterShowContent.hasOwnProperty(adapter))
        this.adapterShowContent[adapter] = this.determineShowContent(this.adapters[adapter]);
    }
  }
}
