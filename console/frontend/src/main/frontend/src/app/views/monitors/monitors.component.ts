import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { StateParams, StateService } from '@uirouter/angularjs';
import { MiscService } from 'src/angularjs/app/services/misc.service';
import { AppService, Configuration } from 'src/angularjs/app/app.service';

interface Monitor {
  hits: number
  destinations: string[]
  name: string
  raised: boolean
  type: string
  lastHit: string
  triggers: Trigger[]
  displayName: string
  activeDestinations: boolean[]
  edit: boolean
  alarm: Alarm
}

interface Alarm {
  source: string
  severity: string
}

export type Trigger = {
  name: string,
  severity: string,
  filter: string,
  period: number,
  sources?: Record<string, string[]>,
  threshold: number,
  id: number,
  type: string,
  events: string[],
  adapters?: string[]
}

@Component({
  selector: 'app-monitors',
  templateUrl: './monitors.component.html',
  styleUrls: ['./monitors.component.scss']
})
export class MonitorsComponent implements OnInit {
  selectedConfiguration: string = "";
  monitors: Monitor[] = [];
  destinations: string[] = [];
  eventTypes: string[] = [];
  totalRaised: number = 0;
  configurations: Configuration[] = [];
  activeDestinations: string[] = [];

  constructor(
    private apiService: ApiService,
    private stateParams: StateParams,
    private $state: StateService,
    private miscService: MiscService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;

      if (this.configurations.length > 0) {
        this.updateConfigurations();
      }
    });

    if (this.configurations.length > 0) {
      this.updateConfigurations();
    }
  };

  updateConfigurations() {
    let configName = this.stateParams['configuration'];         // See if the configuration query param is populated
    if (!configName) configName = this.configurations[0].name;  // Fall back to the first configuration
    this.changeConfiguration(configName);                       // Update the view
  };

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;

    if (this.stateParams['configuration'] == "" || this.stateParams['configuration'] != name) { // Update the URL
      this.$state.transitionTo('pages.monitors', { configuration: name }, { notify: false, reload: false });
    }

    this.update();
  };

  update() {
    this.apiService.Get("configurations/" + this.selectedConfiguration + "/monitors", (data) => {
      Object.assign(this, data);

      this.totalRaised = 0;
      for (const monitor of this.monitors) {
        monitor.displayName = monitor.name;
        if (monitor.raised) this.totalRaised++;
        monitor.activeDestinations = [];
        for (const j in this.destinations) {
          const destination = this.destinations[j];
          monitor.activeDestinations[j] = (monitor.destinations.indexOf(destination) > -1);
        }
      }
    });
  };

  getUrl(monitor: Monitor, trigger?: Trigger) {
    let url = "configurations/" + this.selectedConfiguration + "/monitors/" + monitor.name;
    if (trigger) url += "/triggers/" + trigger.id;
    return url;
  };

  raise(monitor: Monitor, trigger?: Trigger) {
    this.apiService.Put(this.getUrl(monitor, trigger), { action: "raise" }, () => {
      this.update();
    });
  };

  clear(monitor: Monitor, trigger?: Trigger) {
    this.apiService.Put(this.getUrl(monitor, trigger), { action: "clear" }, () => {
      this.update();
    });
  };

  edit(monitor: Monitor, trigger?: Trigger) {
    let destinations = [];

    for (const dest in monitor.activeDestinations) {
      if (monitor.activeDestinations[dest]) {
        destinations.push(dest);
      }
    }

    this.apiService.Put(this.getUrl(monitor, trigger), { action: "edit", name: monitor.displayName, type: monitor.type, destinations: destinations }, () => {
      this.update();
    });
  };

  deleteMonitor(monitor: Monitor, trigger?: Trigger) {
    this.apiService.Delete(this.getUrl(monitor, trigger), () => {
      this.update();
    });
  };

  deleteTrigger(monitor: Monitor, trigger?: Trigger) {
    this.apiService.Delete(this.getUrl(monitor, trigger), () => {
      this.update();
    });
  };

  downloadXML(monitorName?: string) {
    let url = this.miscService.getServerPath() + "iaf/api/configurations/" + this.selectedConfiguration + "/monitors";
    if (monitorName) {
      url += "/" + monitorName;
    }
    window.open(url + "?xml=true", "_blank");
  };
}
