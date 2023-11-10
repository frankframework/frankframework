import { AppService } from "../../app.service";
import { ApiService } from "../../services/api.service";
import { MiscService } from "../../services/misc.service";
import { StateParams } from '@uirouter/angularjs';

interface Monitor {
  name: string
  displayName: string
  type: string
  raised: boolean
  destinations: string[]
  activeDestinations: boolean[]
}

class MonitorsController {
  selectedConfiguration: string = "";
  monitors: Monitor[] = [];
  destinations = [];
  eventTypes = [];
  totalRaised = 0;
  configurations: any;

  constructor(
    private Api: ApiService,
    private $stateParams: StateParams,
    private Misc: MiscService,
    private appService: AppService,
  ) { };

  $onInit() {
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
    var configName = this.$stateParams['configuration']; //See if the configuration query param is populated
    if (!configName) configName = this.configurations[0].name; //Fall back to the first configuration
    this.changeConfiguration(configName); //Update the view
  };

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;

    if (this.$stateParams['configuration'] == "" || this.$stateParams['configuration'] != name) { //Update the URL
      this.$stateParams['configuration'].transitionTo('pages.monitors', { configuration: name }, { notify: false, reload: false });
    }

    this.update();
  };

  update() {
    this.Api.Get("configurations/" + this.selectedConfiguration + "/monitors", (data) => {
      $.extend(this, data);

      this.totalRaised = 0;
      for (const i in this.monitors) {
        if (this.monitors[i].raised) this.totalRaised++;
        var monitor = this.monitors[i];
        monitor.activeDestinations = [];
        for (const j in this.destinations) {
          var destination = this.destinations[j];
          monitor.activeDestinations[destination] = (monitor.destinations.indexOf(destination) > -1);
        }
      }
    });
  };

  getUrl(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    var url = "configurations/" + this.selectedConfiguration + "/monitors/" + monitor.name;
    if (trigger != undefined && trigger != "") url += "/triggers/" + trigger.id;
    return url;
  };

  raise(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    this.Api.Put(this.getUrl(monitor, trigger), { action: "raise" }, () => {
      this.update();
    });
  };

  clear(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    this.Api.Put(this.getUrl(monitor, trigger), { action: "clear" }, () => {
      this.update();
    });
  };

  edit(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    var destinations = [];

    for (const dest in monitor.activeDestinations) {
      if (monitor.activeDestinations[dest]) {
        destinations.push(dest);
      }
    }

    this.Api.Put(this.getUrl(monitor, trigger), { action: "edit", name: monitor.displayName, type: monitor.type, destinations: destinations }, () => {
      this.update();
    });
  };

  deleteMonitor(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    this.Api.Delete(this.getUrl(monitor, trigger), () => {
      this.update();
    });
  };

  deleteTrigger(monitor: Monitor, trigger: any) { // TODO: Add trigger type after merge
    this.Api.Delete(this.getUrl(monitor, trigger), () => {
      this.update();
    });
  };

  downloadXML(monitorName: string) {
    var url = this.Misc.getServerPath() + "iaf/api/configurations/" + this.selectedConfiguration + "/monitors";
    if (monitorName) {
      url += "/" + monitorName;
    }
    window.open(url + "?xml=true", "_blank");
  };
}
