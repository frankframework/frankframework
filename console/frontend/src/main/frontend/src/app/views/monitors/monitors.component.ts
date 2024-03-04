import { Component, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { Monitor, MonitorsService, Trigger } from './monitors.service';
import {
  ActivatedRoute,
  ParamMap,
  Router,
  convertToParamMap,
} from '@angular/router';

@Component({
  selector: 'app-monitors',
  templateUrl: './monitors.component.html',
  styleUrls: ['./monitors.component.scss'],
})
export class MonitorsComponent implements OnInit {
  selectedConfiguration: string = '';
  monitors: Monitor[] = [];
  destinations: string[] = [];
  eventTypes: string[] = [];
  totalRaised: number = 0;
  configurations: Configuration[] = [];
  activeDestinations: string[] = [];

  private routeQueryParams: ParamMap = convertToParamMap({});

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private monitorsService: MonitorsService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      this.routeQueryParams = parameters;
      this.configurations = this.appService.configurations;

      if (this.configurations.length > 0) {
        this.updateConfigurations();
      }
    });

    this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;

      if (this.configurations.length > 0) {
        this.updateConfigurations();
      }
    });
  }

  updateConfigurations(): void {
    let configName = this.routeQueryParams.get('configuration'); // See if the configuration query param is populated
    if (!configName) configName = this.configurations[0].name; // Fall back to the first configuration
    this.changeConfiguration(configName); // Update the view
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    const configurationQueryParameter =
      this.routeQueryParams.get('configuration');

    if (
      configurationQueryParameter == '' ||
      configurationQueryParameter != name
    ) {
      // Update the URL
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { configuration: name },
      });
    }

    this.update();
  }

  update(): void {
    this.monitorsService
      .getMonitors(this.selectedConfiguration)
      .subscribe((data) => {
        this.destinations = data.destinations;
        this.eventTypes = data.eventTypes;
        this.monitors = data.monitors;

        this.totalRaised = 0;
        for (const monitor of this.monitors) {
          monitor.displayName = monitor.name;
          if (monitor.raised) this.totalRaised++;
          monitor.activeDestinations = [];
          for (const index in this.destinations) {
            const destination = this.destinations[index];
            monitor.activeDestinations[index] =
              monitor.destinations.includes(destination);
          }
        }
      });
  }

  raise(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .putMonitorOrTriggerAction(
        'raise',
        this.selectedConfiguration,
        monitor,
        trigger,
      )
      .subscribe(() => {
        this.update();
      });
  }

  clear(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .putMonitorOrTriggerAction(
        'clear',
        this.selectedConfiguration,
        monitor,
        trigger,
      )
      .subscribe(() => {
        this.update();
      });
  }

  edit(monitor: Monitor, trigger?: Trigger): void {
    const destinations = [];

    for (const destination in monitor.activeDestinations) {
      if (monitor.activeDestinations[destination]) {
        destinations.push(destination);
      }
    }

    this.monitorsService
      .putMonitorOrTriggerEdit(
        destinations,
        this.selectedConfiguration,
        monitor,
        trigger,
      )
      .subscribe(() => {
        this.update();
      });
  }

  deleteMonitor(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .deleteMonitorOrTrigger(this.selectedConfiguration, monitor, trigger)
      .subscribe(() => {
        this.update();
      });
  }

  deleteTrigger(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .deleteMonitorOrTrigger(this.selectedConfiguration, monitor, trigger)
      .subscribe(() => {
        this.update();
      });
  }

  downloadXML(monitorName?: string): void {
    let url = `${this.appService.getServerPath()}iaf/api/configurations/${
      this.selectedConfiguration
    }/monitors`;
    if (monitorName) {
      url += `/${monitorName}`;
    }
    window.open(`${url}?xml=true`, '_blank');
  }
}
