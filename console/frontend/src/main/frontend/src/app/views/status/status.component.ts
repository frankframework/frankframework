import { ViewportScroller } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ConfigurationFilter } from 'src/app/pipes/configuration-filter.pipe';
import { StatusService } from './status.service';
import {
  Adapter,
  AdapterMessage,
  AdapterStatus,
  Alert,
  AppService,
  Configuration,
  MessageLog,
  MessageSummary,
  Receiver,
  Summary,
} from 'src/app/app.service';
import { PollerService } from 'src/app/services/poller.service';
import { getProcessStateIcon, getProcessStateIconColor } from 'src/app/utils';

type Filter = Record<AdapterStatus, boolean>;

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss'],
})
export class StatusComponent implements OnInit, OnDestroy {
  filter: Filter = {
    started: true,
    stopped: true,
    warning: true,
  };
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};
  adapterName = '';
  searchText = '';
  selectedConfiguration = 'All';
  reloading = false;
  configurationFlowDiagram: string | null = null;
  isConfigStubbed: Record<string, boolean> = {};
  isConfigReloading: Record<string, boolean> = {};
  isConfigAutoReloadable: Record<string, boolean> = {};
  msgBoxExpanded = false;
  adapterShowContent: Record<keyof typeof this.adapters, boolean> = {};
  loadFlowInline = true;

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
  alerts: Alert[] = [];
  messageLog: Record<string, MessageLog> = {};

  // functions
  getProcessStateIconFn = getProcessStateIcon;
  getProcessStateIconColorFn = getProcessStateIconColor;

  private _subscriptions = new Subscription();

  constructor(
    private Poller: PollerService,
    private viewportScroller: ViewportScroller,
    private route: ActivatedRoute,
    private router: Router,
    private statusService: StatusService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      this.route.fragment.subscribe((fragment) => {
        const hash = fragment; // let hash = this.$location.hash();
        this.adapterName = parameters.get('adapter') ?? '';
        if (this.adapterName == '' && hash && hash != '') {
          //If the adapter param hasn't explicitly been set
          this.adapterName = hash;
        } else if (this.adapterName != '') {
          /* let routeQueryParams = null;
          if (filterParam && filterParam != "") {
            let routeQueryParams = filterParam.length > 16 ? null // if everything is selected then filter doesnt need to be in the url
              : { filter: filterParam };
            this.router.navigate([], { relativeTo: this.route, queryParams: routeQueryParams, fragment: hash ?? undefined }); // this.$location.hash(this.adapterName);
          } */
          this.router.navigate([], {
            relativeTo: this.route,
            fragment: this.adapterName,
          });
        }
      });

      const filterParameter = parameters.get('filter');
      if (filterParameter && filterParameter != '') {
        const filters: Filter = {
          started: false,
          stopped: false,
          warning: false,
        };
        for (const f of filterParameter.split('+')) {
          filters[f as keyof Filter] = true;
        }
        this.filter = filters;
      }
      const searchParameter = parameters.get('search');
      if (searchParameter && searchParameter != '') {
        this.searchText = searchParameter;
      }

      const configurationParameter = parameters.get('configuration');
      if (configurationParameter && configurationParameter != 'All')
        this.changeConfiguration(configurationParameter);
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
    const configurationsSubscription =
      this.appService.configurations$.subscribe(() =>
        this.check4StubbedConfigs(),
      );
    this._subscriptions.add(configurationsSubscription);
    const summariesSubscription = this.appService.summaries$.subscribe(() => {
      this.adapterSummary = this.appService.adapterSummary;
      this.receiverSummary = this.appService.receiverSummary;
      this.messageSummary = this.appService.messageSummary;
    });
    this._subscriptions.add(summariesSubscription);
    const alertsSubscription = this.appService.alerts$.subscribe(() => {
      this.alerts = [...this.appService.alerts];
    });
    this._subscriptions.add(alertsSubscription);
    const messageLogSubscription = this.appService.messageLog$.subscribe(() => {
      this.messageLog = { ...this.appService.messageLog };
    });
    this._subscriptions.add(messageLogSubscription);
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.adapters = { ...this.appService.adapters };
      this.updateAdapterShownContent();
    });
    this._subscriptions.add(adaptersSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  applyFilter(filterName: keyof Filter): void {
    const filter = { ...this.filter };
    filter[filterName] = !filter[filterName];
    this.filter = filter;
    this.updateQueryParams();
  }

  showContent(adapter: Adapter): boolean {
    return this.adapterShowContent[`${adapter.configuration}/${adapter.name}`];
  }

  updateQueryParams(): void {
    const filterString: string[] = [];
    let filterCount = 0;
    for (const f in this.filter) {
      if (this.filter[f as keyof Filter]) {
        filterString.push(f);
        filterCount += 1;
      }
    }
    const transitionObject: Record<string, string> = {};
    if (filterCount < 3) transitionObject['filter'] = filterString.join('+');
    if (this.selectedConfiguration != 'All')
      transitionObject['configuration'] = this.selectedConfiguration;
    if (this.searchText.length > 0)
      transitionObject['search'] = this.searchText;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
      preserveFragment: true,
    });
  }

  collapseAll(): void {
    this.loadFlowInline = true;
    for (const adapter of Object.keys(this.adapters)) {
      this.adapterShowContent[adapter] = false;
    }
  }

  expandAll(): void {
    this.loadFlowInline = false;
    for (const adapter of Object.keys(this.adapters)) {
      this.adapterShowContent[adapter] = true;
    }
  }

  stopAll(): void {
    this.statusService
      .updateAdapters('stop', this.getCompiledAdapterList())
      .subscribe();
  }

  startAll(): void {
    this.statusService
      .updateAdapters('start', this.getCompiledAdapterList())
      .subscribe();
  }

  reloadConfiguration(): void {
    if (this.selectedConfiguration == 'All') return;

    this.isConfigReloading[this.selectedConfiguration] = true;

    this.Poller.getAll().stop();
    this.statusService
      .updateSelectedConfiguration(this.selectedConfiguration, 'reload')
      .subscribe(() => {
        this.startPollingForConfigurationStateChanges(() => {
          this.Poller.getAll().start();
        });
      });
  }
  fullReload(): void {
    this.reloading = true;
    this.Poller.getAll().stop();
    this.statusService.updateConfigurations('reload').subscribe(() => {
      this.reloading = false;
      this.startPollingForConfigurationStateChanges(() => {
        this.Poller.getAll().start();
      });
    });
  }

  startPollingForConfigurationStateChanges(callback?: () => void): void {
    this.Poller.add(
      'server/configurations',
      (data) => {
        const configurations = data as Configuration[];
        this.appService.updateConfigurations(configurations);

        let ready = true;
        for (const index in configurations) {
          const config = configurations[index];
          //When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
          if (
            config.state != 'STARTED' &&
            !(config.state == 'STOPPED' && config.exception != null)
          ) {
            ready = false;
            break;
          }
        }
        if (ready) {
          //Remove poller once all states are STARTED
          window.setTimeout(() => {
            this.Poller.remove('server/configurations');
            if (callback != null && typeof callback == 'function') callback();
          });
        }
      },
      true,
    );
  }

  showReferences(): void {
    window.open(this.configurationFlowDiagram!);
  }

  updateConfigurationFlowDiagram(configurationName: string): void {
    const flowUrl =
      configurationName == 'All' ? '?flow=true' : `${configurationName}/flow`;
    this.configurationFlowDiagram =
      this.statusService.getConfigurationFlowDiagramUrl(flowUrl);
  }

  check4StubbedConfigs(): void {
    this.configurations = this.appService.configurations;
    for (const index in this.appService.configurations) {
      const config = this.appService.configurations[index];
      this.isConfigStubbed[config.name] = config.stubbed;
      this.isConfigAutoReloadable[config.name] = config.autoreload ?? false;
      this.isConfigReloading[config.name] =
        config.state == 'STARTING' || config.state == 'STOPPING'; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
    }
  }

  // Commented out in template, so unused
  closeAlert(index: number): void {
    this.appService.alerts.splice(index, 1);
    this.appService.updateAlerts(this.appService.alerts);
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.appService.updateAdapterSummary(name, true);
    this.updateQueryParams();
    this.updateConfigurationFlowDiagram(name);
  }

  getTransactionalStores(
    receiver: Receiver,
  ): { name: string; numberOfMessages: number }[] {
    return Object.values(receiver.transactionalStores);
  }

  getMessageLog(selectedConfiguration: string): AdapterMessage[] {
    return this.messageLog[selectedConfiguration]?.messages ?? [];
  }

  startAdapter(adapter: Adapter): void {
    adapter.state = 'starting';
    this.statusService
      .updateAdapter(adapter.configuration, adapter.name, 'start')
      .subscribe();
  }
  stopAdapter(adapter: Adapter): void {
    adapter.state = 'stopping';
    this.statusService
      .updateAdapter(adapter.configuration, adapter.name, 'stop')
      .subscribe();
  }
  startReceiver(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService
      .updateReceiver(
        adapter.configuration,
        adapter.name,
        receiver.name,
        'start',
      )
      .subscribe();
  }
  stopReceiver(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService
      .updateReceiver(
        adapter.configuration,
        adapter.name,
        receiver.name,
        'stop',
      )
      .subscribe();
  }
  addThread(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService
      .updateReceiver(
        adapter.configuration,
        adapter.name,
        receiver.name,
        'incthread',
      )
      .subscribe();
  }
  removeThread(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService
      .updateReceiver(
        adapter.configuration,
        adapter.name,
        receiver.name,
        'decthread',
      )
      .subscribe();
  }

  navigateByAlert(alert: Alert): void {
    if (alert.link) {
      this.router.navigate(['configuration', alert.link.name], {
        fragment: alert.link['#'],
      });
    }
  }

  private getCompiledAdapterList(): string[] {
    const compiledAdapterList: string[] = [];
    const adapters = ConfigurationFilter(
      this.adapters,
      this.selectedConfiguration,
      this.filter,
    );
    for (const adapter of Object.values(adapters)) {
      const configuration = adapter.configuration;
      const adapterName = adapter.name;
      compiledAdapterList.push(`${configuration}/${adapterName}`);
    }
    return compiledAdapterList;
  }

  private determineShowContent(adapter: Adapter): boolean {
    if (adapter.status == 'stopped') {
      return true;
    } else if (this.adapterName != '' && adapter.name == this.adapterName) {
      this.viewportScroller.scrollToAnchor(this.adapterName); // this.$anchorScroll();
      return true;
    } else {
      return false;
    }
  }

  private updateAdapterShownContent(): void {
    for (const adapter in this.adapters) {
      if (!this.adapterShowContent.hasOwnProperty(adapter))
        this.adapterShowContent[adapter] = this.determineShowContent(
          this.adapters[adapter],
        );
    }
  }
}
