import { Component, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { ConfigurationsService } from '../../configurations.service';
import { ToastService } from 'src/app/services/toast.service';
import { SortEvent, ThSortableDirective, basicAnyValueTableSort } from 'src/app/components/th-sortable.directive';

@Component({
  selector: 'app-configurations-manage-details',
  templateUrl: './configurations-manage-details.component.html',
  styleUrls: ['./configurations-manage-details.component.scss'],
})
export class ConfigurationsManageDetailsComponent implements OnInit, OnDestroy {
  protected configuration: Configuration = {
    name: '',
    stubbed: false,
    state: 'STOPPED',
    type: 'DatabaseClassLoader',
    jdbcMigrator: false,
    version: '',
  };
  protected versionsSorted: Configuration[] = [];
  protected search: string = '';

  private promise: number = -1;
  private versions: Configuration[] = [];
  private lastSortEvent: SortEvent = { direction: null, column: '' };

  @ViewChildren(ThSortableDirective) headers!: QueryList<ThSortableDirective>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appService: AppService,
    private configurationsService: ConfigurationsService,
    private sweetalertService: SweetalertService,
    private toastService: ToastService,
  ) {
    const routeState = this.router.getCurrentNavigation()?.extras.state ?? {};
    if (!routeState['configuration']) {
      this.router.navigate(['..'], { relativeTo: this.route });
    }
    this.configuration = routeState['configuration'];
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((parameters) => {
      const nameParameter = parameters.get('name');
      if (nameParameter && nameParameter != '')
        this.appService.customBreadcrumbs(`Configurations > Manage > ${nameParameter}`);
      else this.router.navigate(['..'], { relativeTo: this.route });

      this.promise = window.setInterval(() => {
        this.update();
      }, 30_000);

      this.update();
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.promise);
  }

  update(): void {
    this.configurationsService.getConfigurationVersions(this.configuration.name).subscribe((data) => {
      for (const configs of data) {
        if (configs.active) {
          configs.actived = true;
        }
      }

      this.versions = data;
      this.versionsSorted = basicAnyValueTableSort(this.versions, this.headers, this.lastSortEvent);
    });
  }

  download(config: Configuration): void {
    window.open(
      `${this.appService.getServerPath()}iaf/api/configurations/${
        config.name
      }/versions/${encodeURIComponent(config.version!)}/download`,
    );
  }

  deleteConfig(config: Configuration): void {
    const message = config.version ? `Are you sure you want to remove version '${config.version}'?` : 'Are you sure?';

    this.sweetalertService.Confirm({ title: message }).then((result) => {
      if (result.isConfirmed) {
        this.configurationsService
          .deleteConfigurationVersion(config.name, encodeURIComponent(config.version!))
          .subscribe(() => {
            this.toastService.success(`Successfully removed version '${config.version}'`);
            this.update();
          });
      }
    });
  }

  activate(config: Configuration): void {
    for (const x in this.versions) {
      const configs = this.versions[x];
      if (configs.version != config.version) configs.actived = false;
    }
    this.configurationsService
      .updateConfigurationVersion(config.name, encodeURIComponent(config.version!), { activate: config.active! })
      .subscribe({
        next: () => {
          this.toastService.success(`Successfully changed startup config to version '${config.version}'`);
        },
        error: () => {
          this.update();
        },
      });
  }

  scheduleReload(config: Configuration): void {
    this.configurationsService
      .updateConfigurationVersion(config.name, encodeURIComponent(config.version!), { autoreload: config.autoreload! })
      .subscribe({
        next: () => {
          this.toastService.success(
            `Successfully ${config.autoreload ? 'enabled' : 'disabled'} Auto Reload for version '${config.version}'`,
          );
        },
        error: () => {
          this.update();
        },
      });
  }

  onSort(event: SortEvent): void {
    this.lastSortEvent = event;
    this.versionsSorted = basicAnyValueTableSort<Configuration>(this.versions, this.headers, event);
  }
}
