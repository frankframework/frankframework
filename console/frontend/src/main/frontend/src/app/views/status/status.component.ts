import { Component, computed, inject, OnDestroy, OnInit, Signal, TrackByFunction } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ConfigurationFilter, ConfigurationFilterPipe } from 'src/app/pipes/configuration-filter.pipe';
import { StatusService } from './status.service';
import { Adapter, AdapterStatus, Alert, AppService, Configuration, MessageLog } from 'src/app/app.service';
import { ServerInfo, ServerInfoService } from '../../services/server-info.service';
import { KeyValue, KeyValuePipe, NgClass } from '@angular/common';
import { ServerWarningsComponent } from './server-warnings/server-warnings.component';
import { ConfigurationTabListComponent } from '../../components/tab-list/configuration-tab-list.component';
import { ConfigurationSummaryComponent } from './configuration-summary/configuration-summary.component';
import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';
import { FormsModule } from '@angular/forms';
import { ConfigurationMessagesComponent } from './configuration-messages/configuration-messages.component';
import { AdapterStatusComponent } from './adapter-status/adapter-status.component';
import { SearchFilterPipe } from '../../pipes/search-filter.pipe';

type Filter = Record<AdapterStatus, boolean>;

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss'],
  imports: [
    ServerWarningsComponent,
    ConfigurationTabListComponent,
    ConfigurationSummaryComponent,
    HasAccessToLinkDirective,
    NgClass,
    FormsModule,
    ConfigurationMessagesComponent,
    AdapterStatusComponent,
    ConfigurationFilterPipe,
    SearchFilterPipe,
    KeyValuePipe,
  ],
})
export class StatusComponent implements OnInit, OnDestroy {
  protected filter: Filter = {
    started: true,
    stopped: true,
    warning: true,
  };
  protected searchText = '';
  protected selectedConfiguration = 'All';
  protected reloading = false;
  protected configurationFlowDiagram: string | null = null;
  protected isConfigStubbed: Record<string, boolean> = {};
  protected isConfigReloading: Record<string, boolean> = {};
  protected isConfigAutoReloadable: Record<string, boolean> = {};
  protected adapterShowContent: Record<string, boolean> = {};
  protected loadFlowInline = true;
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected readonly configurations: Signal<Configuration[]> = computed(() => {
    const configurations = this.appService.configurations();
    this.check4StubbedConfigs(configurations);
    this.updateConfigurationFlowDiagram(this.selectedConfiguration);
    return configurations;
  });
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected readonly adapters: Signal<Record<string, Adapter>> = computed(() => {
    const adapters = this.appService.adapters();
    if (this.hasExpendedAdaptersLoaded) {
      this.updateAdapterShownContent(adapters);
      return adapters;
    }
    const adaptersList = Object.values(adapters);
    if (adaptersList.length > 0 && 'messages' in adaptersList[0]) {
      this.hasExpendedAdaptersLoaded = true;
      this.updateAdapterShownContent(adapters);
    }
    return adapters;
  });
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected freeDiskSpacePercentage: Signal<number | null> = computed(() => {
    const serverInfo = this.serverInfo();
    if (serverInfo) return Math.round((serverInfo.fileSystem.freeSpace / serverInfo.fileSystem.totalSpace) * 1000) / 10;
    return null;
  });

  private adapterName = '';
  private _subscriptions = new Subscription();
  private hasExpendedAdaptersLoaded = false;

  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly router: Router = inject(Router);
  private readonly statusService: StatusService = inject(StatusService);
  private readonly serverInfoService: ServerInfoService = inject(ServerInfoService);
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected serverInfo: Signal<ServerInfo | null> = this.serverInfoService.serverInfo;
  private readonly appService: AppService = inject(AppService);
  protected readonly alerts: Signal<Alert[]> = this.appService.alerts;
  protected readonly messageLog: Signal<Record<string, MessageLog>> = this.appService.messageLog;

  ngOnInit(): void {
    const fragmentSubscription = this.route.fragment.subscribe((fragment) => {
      if (fragment && fragment != '' && this.adapterName == '') {
        //If the adapter param hasn't explicitly been set
        this.adapterName = fragment;
      }
    });
    this._subscriptions.add(fragmentSubscription);

    const queryParameterSubscription = this.route.queryParamMap.subscribe((parameters) => {
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
      if (configurationParameter) this.changeConfiguration(configurationParameter);
    });
    this._subscriptions.add(queryParameterSubscription);
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
    const transitionObject: Record<string, string | null> = {};
    transitionObject['filter'] = filterCount < 3 ? filterString.join('+') : null;
    transitionObject['search'] = this.searchText;

    const fragment = this.adapterName === '' ? undefined : this.adapterName;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
      queryParamsHandling: 'merge',
      replaceUrl: true,
      fragment,
    });
  }

  collapseAll(): void {
    this.loadFlowInline = true;
    for (const adapter of Object.keys(this.adapters())) {
      this.adapterShowContent[adapter] = false;
    }
  }

  expandAll(): void {
    this.loadFlowInline = false;
    for (const adapter of Object.keys(this.adapters())) {
      this.adapterShowContent[adapter] = true;
    }
  }

  stopAll(): void {
    this.allAction('stop');
  }

  startAll(): void {
    this.allAction('start');
  }

  reloadConfiguration(): void {
    if (this.selectedConfiguration == 'All') return;

    this.isConfigReloading[this.selectedConfiguration] = true;
    this.statusService.updateSelectedConfiguration(this.selectedConfiguration, 'reload').subscribe(() => {
      this.isConfigReloading[this.selectedConfiguration] = false;
    });
  }

  fullReload(): void {
    this.reloading = true;
    this.statusService.updateConfigurations('fullreload').subscribe(() => {
      this.reloading = false;
    });
  }

  showReferences(): void {
    window.open(this.configurationFlowDiagram!);
  }

  updateConfigurationFlowDiagram(configurationName: string): void {
    const flowUrl = configurationName == 'All' ? '?flow=true' : `/${configurationName}/flow`;
    this.configurationFlowDiagram = this.statusService.getConfigurationFlowDiagramUrl(flowUrl);
  }

  check4StubbedConfigs(configurations: Configuration[]): void {
    for (const config of configurations) {
      this.isConfigStubbed[config.name] = config.stubbed;
      this.isConfigAutoReloadable[config.name] = config.autoreload ?? false;
      this.isConfigReloading[config.name] = config.state == 'STARTING' || config.state == 'STOPPING'; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
    }
  }

  // Commented out in template, so unused
  closeAlert(index: number): void {
    const alerts = this.alerts();
    alerts.splice(index, 1);
    this.appService.alerts.set(alerts);
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.appService.updateAdapterSummary(name, true);
    this.updateConfigurationFlowDiagram(name);
  }

  private allAction(action: string): void {
    if (this.searchText != '') {
      this.statusService.updateAdapters(action, this.getCompiledAdapterList()).subscribe();
    } else if (this.selectedConfiguration === 'All') {
      this.statusService.updateConfigurations(action).subscribe();
    } else {
      this.statusService.updateSelectedConfiguration(this.selectedConfiguration, action).subscribe();
    }
  }

  private getCompiledAdapterList(): string[] {
    const compiledAdapterList: string[] = [];
    const adapters = ConfigurationFilter(this.adapters(), this.selectedConfiguration, this.filter, this.searchText);
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

  private updateAdapterShownContent(adapters: Record<string, Adapter>): void {
    for (const adapter in adapters) {
      if (!this.adapterShowContent.hasOwnProperty(adapter)) {
        this.adapterShowContent[adapter] = this.determineShowContent(adapters[adapter]);

        if (this.adapterName === adapters[adapter].name) {
          setTimeout(() => {
            document.querySelector(`#${this.adapterName}`)?.scrollIntoView();
          });
        }
      }
    }
  }
}
