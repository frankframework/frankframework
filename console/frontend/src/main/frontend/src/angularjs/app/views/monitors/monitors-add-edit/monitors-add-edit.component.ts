import { ApiService } from "src/angularjs/app/services/api.service";
import { Adapter, AppService } from "src/app/app.service";
import { StateParams } from '@uirouter/angularjs';
import { StateService } from "@uirouter/angularjs";

interface Trigger {
  type: string
  filter: string
  events: Event[]
  sources: any
  adapters: any
}

interface Event {
  eventName: string
  sources: any
}

class MonitorsAddEditController {
  loading = true;
  selectedConfiguration = null;
  monitor = "";
  events: Event[] = [];
  severities = [];
  triggerId: number = -1;
  trigger: Trigger = {
    type: "Alarm",
    filter: "none",
    events: [],
    sources: {},
    adapters: {}
  }
  eventSources: any[] = [];
  url: string = "";

  constructor(
    private appService: AppService,
    private Api: ApiService,
    private $stateService: StateService,
    private $stateParams: StateParams,
  ) { };

  $onInit() {
    this.appService.loading$.subscribe(_ => this.loading = false);

    if (this.$stateParams['configuration'] == "" || this.$stateParams['monitor'] == "") {
      this.$stateService.go('pages.monitors');
    } else {
      this.selectedConfiguration = this.$stateParams['configuration'];
      this.monitor = this.$stateParams['monitor'];
      this.triggerId = this.$stateParams['trigger'] || "";
      this.url = "configurations/" + this.selectedConfiguration + "/monitors/" + this.monitor + "/triggers/" + this.triggerId;

      this.Api.Get(this.url, (data) => {
        $.extend(this, data);
        this.calculateEventSources();

        if (data.trigger && data.trigger.sources) {
          var sources = data.trigger.sources;
          this.trigger.sources = [];
          this.trigger.adapters = [];

          for (const adapter in sources) {
            if (data.trigger.filter == "SOURCE") {
              for (const i in sources[adapter]) {
                this.trigger.sources.push(adapter + "$$" + sources[adapter][i]);
              }
            } else {
              this.trigger.adapters.push(adapter);
            }
          }
        }
      }, () => {
        this.$stateService.go('pages.monitors', this.$stateParams);
      });
    }
  };

  getAdaptersForEvents(events: Event[]) {
    if (!events) return [];
    var adapters: Adapter[] = [];

    this.events.forEach(event => {
      if (events.indexOf(event) > -1) {
        let sourceList = event.sources;
        adapters = adapters.concat(sourceList);
      }
    });

    return Array.from(new Set(adapters));
  };

  calculateEventSources() {
    for (const eventCode in this.events) {
      var retVal = [];
      var eventSources = this.events[eventCode].sources;

      for (const adapter in eventSources) {
        for (const i in eventSources[adapter]) {
          retVal.push({ adapter: adapter, source: eventSources[adapter][i] });
        }
      }

      this.eventSources[eventCode] = retVal;
    }
  };

  getSourceForEvents(events: Event[]) {
    var retval: any[] = [];

    for (const eventCode in this.events) {
      retval = retval.concat(this.eventSources[eventCode]);
    };

    return retval;
  };

  submit(trigger: Trigger) {
    if (trigger.filter == "ADAPTER") {
      delete trigger.sources;
    } else if (trigger.filter == "SOURCE") {
      delete trigger.adapters;
      var sources = trigger.sources;
      trigger.sources = {};

      for (const i in sources) {
        var s = sources[i].split("$$");
        var adapter = s[0];
        var source = s[1];
        if (!trigger.sources[adapter]) trigger.sources[adapter] = [];
        trigger.sources[adapter].push(source);
      }
    }

    if (this.triggerId && this.triggerId > -1) {
      this.Api.Put(this.url, trigger, (returnData) => {
        this.$stateService.go('pages.monitors', this.$stateParams);
      });
    } else {
      this.Api.Post(this.url, JSON.stringify(trigger), (returnData) => {
        this.$stateService.go('pages.monitors', this.$stateParams);
      });
    }
  };
};

appModule.component('monitorsAddEdit', {
	controller: ['$scope', 'Api', '$state', MonitorsAddEditController],
	templateUrl: 'angularjs/app/views/monitors/monitors-add-edit/monitors-add-edit.component.html',
});
