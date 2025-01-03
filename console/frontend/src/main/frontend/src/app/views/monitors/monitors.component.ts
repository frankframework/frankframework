import { Component, OnDestroy, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { Monitor, MonitorsService, Trigger } from './monitors.service';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-monitors',
  templateUrl: './monitors.component.html',
  styleUrls: ['./monitors.component.scss'],
  standalone: false,
})
export class MonitorsComponent implements OnInit, OnDestroy {
  protected selectedConfiguration: string = '';
  protected monitors: Monitor[] = [];
  protected destinations: string[] = [];
  protected eventTypes: string[] = [];
  protected totalRaised: number = 0;
  protected configurations: Configuration[] = this.appService.configurations;

  private routeQueryParams: ParamMap = convertToParamMap({});
  private subscriptions: Subscription = new Subscription();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private monitorsService: MonitorsService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      this.routeQueryParams = parameters;
      if (this.configurations.length > 0) {
        this.updateConfigurations();
      }
    });
    const configurationsSubscription = this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
      if (this.configurations.length > 0) {
        this.updateConfigurations();
      }
    });
    this.subscriptions.add(configurationsSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  updateConfigurations(): void {
    let configName = this.routeQueryParams.get('configuration'); // See if the configuration query param is populated
    if (!configName) configName = this.configurations[0].name; // Fall back to the first configuration
    this.changeConfiguration(configName); // Update the view
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.update();
  }

  update(): void {
    this.monitorsService.getMonitors(this.selectedConfiguration).subscribe((data) => {
      this.destinations = data.destinations;
      this.eventTypes = data.eventTypes;
      this.monitors = data.monitors;

      this.totalRaised = 0;
      for (const monitor of this.monitors) {
        monitor.displayName = monitor.name;
        if (monitor.raised) this.totalRaised++;
        monitor.activeDestinations = {};
        for (const index in this.destinations) {
          const destination = this.destinations[index];
          monitor.activeDestinations[destination] = monitor.destinations.includes(destination);
        }
      }
    });
  }

  raise(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .putMonitorOrTriggerAction('raise', this.selectedConfiguration, monitor, trigger)
      .subscribe(() => {
        this.update();
      });
  }

  clear(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService
      .putMonitorOrTriggerAction('clear', this.selectedConfiguration, monitor, trigger)
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
      .putMonitorOrTriggerEdit(destinations, this.selectedConfiguration, monitor, trigger)
      .subscribe(() => {
        this.update();
      });
  }

  deleteMonitor(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService.deleteMonitorOrTrigger(this.selectedConfiguration, monitor, trigger).subscribe(() => {
      this.update();
    });
  }

  deleteTrigger(monitor: Monitor, trigger?: Trigger): void {
    this.monitorsService.deleteMonitorOrTrigger(this.selectedConfiguration, monitor, trigger).subscribe(() => {
      this.update();
    });
  }

  downloadXML(monitorName?: string): void {
    let url = `${this.appService.getServerPath()}iaf/api/configurations/${this.selectedConfiguration}/monitors`;
    if (monitorName) {
      url += `/${monitorName}`;
    }
    window.open(`${url}?xml=true`, '_blank');
  }
}
