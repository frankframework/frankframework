import { Component, OnDestroy, OnInit, TrackByFunction } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ConfigurationFilter } from 'src/app/pipes/configuration-filter.pipe';
import { StatusService } from './status.service';
import { Adapter, AdapterStatus, Alert, AppService, Configuration, MessageLog } from 'src/app/app.service';
import { PollerService } from 'src/app/services/poller.service';
import { ServerInfo, ServerInfoService } from '../../services/server-info.service';
import { KeyValue } from '@angular/common';

type Filter = Record<AdapterStatus, boolean>;

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss'],
})
export class StatusComponent implements OnInit, OnDestroy {
  protected filter: Filter = {
    started: true,
    stopped: true,
    warning: true,
  };
  protected configurations: Configuration[] = [];
  protected adapters: Record<string, Adapter> = {};
  protected searchText = '';
  protected selectedConfiguration = 'All';
  protected reloading = false;
  protected configurationFlowDiagram: string | null = null;
  protected isConfigStubbed: Record<string, boolean> = {};
  protected isConfigReloading: Record<string, boolean> = {};
  protected isConfigAutoReloadable: Record<string, boolean> = {};
  protected adapterShowContent: Record<keyof typeof this.adapters, boolean> = {};
  protected loadFlowInline = true;
  protected alerts: Alert[] = [];
  protected messageLog: Record<string, MessageLog> = {};
  protected serverInfo?: ServerInfo;
  protected freeDiskSpacePercentage?: number;

  private adapterName = '';
  private _subscriptions = new Subscription();
  private hasExpendedAdaptersLoaded = false;

  constructor(
    private Poller: PollerService,
    private route: ActivatedRoute,
    private router: Router,
    private statusService: StatusService,
    private appService: AppService,
    private serverInfoService: ServerInfoService,
  ) {}

  ngOnInit(): void {
    this.route.fragment.subscribe((fragment) => {
      if (fragment && fragment != '' && this.adapterName == '') {
        //If the adapter param hasn't explicitly been set
        this.adapterName = fragment;
      }
    });
    this.route.queryParamMap.subscribe((parameters) => {
      const adapterName = parameters.get('adapter');
      if (adapterName && adapterName != '' && this.adapterName == '') {
        this.adapterName = adapterName;
      }

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
      if (configurationParameter && configurationParameter != 'All') this.changeConfiguration(configurationParameter);
    });

    this.updateConfigurationFlowDiagram(this.selectedConfiguration);
    this.appService.appConstants$.subscribe(() => {
      this.updateConfigurationFlowDiagram(this.selectedConfiguration);
    });

    this.check4StubbedConfigs();
    this.getFreeDiskSpacePercentage();
    this.alerts = this.appService.alerts;
    this.messageLog = this.appService.messageLog;
    this.adapters = this.appService.adapters;

    const configurationsSubscription = this.appService.configurations$.subscribe(() => this.check4StubbedConfigs());
    this._subscriptions.add(configurationsSubscription);

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

      if (this.hasExpendedAdaptersLoaded) {
        this.updateAdapterShownContent();
      } else {
        const adaptersList = Object.values(this.adapters);
        if (adaptersList.length > 0 && 'messages' in adaptersList[0]) {
          this.hasExpendedAdaptersLoaded = true;
          this.updateAdapterShownContent();
        }
      }
    });
    this._subscriptions.add(adaptersSubscription);
    this.updateAdapterShownContent();
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  trackAdaptersByFn: TrackByFunction<KeyValue<string, Adapter>> = (
    _index: number,
    adapterKV: KeyValue<string, Adapter>,
  ): string => {
    return adapterKV.key;
  };

  applyFilter(filterName: keyof Filter): void {
    const filter = { ...this.filter };
    filter[filterName] = !filter[filterName];
    this.filter = filter;
    this.updateQueryParams();
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
    if (this.selectedConfiguration != 'All') transitionObject['configuration'] = this.selectedConfiguration;
    if (this.searchText.length > 0) transitionObject['search'] = this.searchText;

    const fragment = this.adapterName === '' ? undefined : this.adapterName;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
      fragment,
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
    this.statusService.updateAdapters('stop', this.getCompiledAdapterList()).subscribe();
  }

  startAll(): void {
    this.statusService.updateAdapters('start', this.getCompiledAdapterList()).subscribe();
  }

  reloadConfiguration(): void {
    if (this.selectedConfiguration == 'All') return;

    this.isConfigReloading[this.selectedConfiguration] = true;

    this.Poller.getAll().stop();
    this.statusService.updateSelectedConfiguration(this.selectedConfiguration, 'reload').subscribe(() => {
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
    this.Poller.add('server/configurations', (data) => {
      const configurations = data as Configuration[];
      this.appService.updateConfigurations(configurations);

      let ready = true;
      for (const index in configurations) {
        const config = configurations[index];
        //When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
        if (config.state != 'STARTED' && !(config.state == 'STOPPED' && config.exception != null)) {
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
    });
  }

  showReferences(): void {
    window.open(this.configurationFlowDiagram!);
  }

  updateConfigurationFlowDiagram(configurationName: string): void {
    const flowUrl = configurationName == 'All' ? '?flow=true' : `/${configurationName}/flow`;
    this.configurationFlowDiagram = this.statusService.getConfigurationFlowDiagramUrl(flowUrl);
  }

  check4StubbedConfigs(): void {
    this.configurations = this.appService.configurations;
    for (const index in this.appService.configurations) {
      const config = this.appService.configurations[index];
      this.isConfigStubbed[config.name] = config.stubbed;
      this.isConfigAutoReloadable[config.name] = config.autoreload ?? false;
      this.isConfigReloading[config.name] = config.state == 'STARTING' || config.state == 'STOPPING'; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
    }
  }

  private getFreeDiskSpacePercentage(): void {
    const serverInfoSubscription = this.serverInfoService.serverInfo$.subscribe((serverInfo) => {
      this.serverInfo = serverInfo;
      this.freeDiskSpacePercentage =
        Math.round((serverInfo.fileSystem.freeSpace / serverInfo.fileSystem.totalSpace) * 1000) / 10;
    });
    this._subscriptions.add(serverInfoSubscription);
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

  private getCompiledAdapterList(): string[] {
    const compiledAdapterList: string[] = [];
    const adapters = ConfigurationFilter(this.adapters, this.selectedConfiguration, this.filter);
    for (const adapter of Object.values(adapters)) {
      const configuration = adapter.configuration;
      const adapterName = adapter.name;
      compiledAdapterList.push(`${configuration}/${adapterName}`);
    }
    return compiledAdapterList;
  }

  private determineShowContent(adapter: Adapter): boolean {
    return adapter.status == 'stopped' || (this.adapterName != '' && adapter.name == this.adapterName);
  }

  private updateAdapterShownContent(): void {
    for (const adapter in this.adapters) {
      if (!this.adapterShowContent.hasOwnProperty(adapter)) {
        this.adapterShowContent[adapter] = this.determineShowContent(this.adapters[adapter]);

        if (this.adapterName === this.adapters[adapter].name) {
          setTimeout(() => {
            document.querySelector(`#${this.adapterName}`)?.scrollIntoView();
          });
        }
      }
    }
  }
}
