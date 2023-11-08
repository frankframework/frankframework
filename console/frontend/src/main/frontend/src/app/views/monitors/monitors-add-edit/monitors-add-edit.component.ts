import { Component, OnInit } from '@angular/core';
import { ApiService } from "src/angularjs/app/services/api.service";
import { Adapter, AppService } from "src/app/app.service";
import { StateParams } from '@uirouter/angularjs';
import { StateService } from "@uirouter/angularjs";
import { Event, Trigger } from 'src/angularjs/app/app.service';

interface RetVal {
  adapter: string,
  source: string
}

@Component({
  selector: 'app-monitors-add-edit',
  templateUrl: './monitors-add-edit.component.html',
  styleUrls: ['./monitors-add-edit.component.scss']
})
export class MonitorsAddEditComponent implements OnInit {
  loading: boolean = true;
  selectedConfiguration: string = "";
  monitor: string = "";
  events: Record<string, Event> = {};
  severities: string[] = [];
  triggerId: number = -1;
  trigger: Trigger = {
    name: "",
    severity: "",
    filter: "",
    period: 0,
    sources: {},
    threshold: 0,
    id: 0,
    type: "",
    events: {},
    adapters: []
  }
  eventSources: RetVal[] = [];
  url: string = "";
  disabled: boolean = false;

  constructor(
    private appService: AppService,
    private apiService: ApiService,
    private stateService: StateService,
    private stateParams: StateParams,
  ) { };

  ngOnInit(): void {
    this.appService.loading$.subscribe(_ => this.loading = false);

    if (this.stateParams['configuration'] == "" || this.stateParams['monitor'] == "") {
      this.stateService.go('pages.monitors');
    } else {
      this.selectedConfiguration = this.stateParams['configuration'];
      this.monitor = this.stateParams['monitor'];
      this.triggerId = this.stateParams['trigger'] || "";
      this.url = "configurations/" + this.selectedConfiguration + "/monitors/" + this.monitor + "/triggers/" + this.triggerId;

      this.apiService.Get(this.url, (data) => {
        Object.assign(this, data);
        this.calculateEventSources();

        if (data.trigger && data.trigger.sources) {
          var sources = data.trigger.sources;
          this.trigger.sources = {};
          this.trigger.adapters = [];

          for (const adapter of sources) {
            if (data.trigger.filter == "SOURCE") {
              for (const i in sources[adapter]) {
                this.trigger.sources[adapter] = [adapter + "$$" + sources[adapter][i]];
              }
            } else {
              this.trigger.adapters.push(adapter);
            }
          }
        }
      }, () => {
        this.stateService.go('pages.monitors', this.stateParams);
      });
    }
  };

  getAdaptersForEvents(events: Record<string, Event>) {
    if (!events) return [];
    var adapters: string[] = [];

    for (const item in this.events) {
      if (events[item]) {
        let sourceList = events[item].sources;
        adapters = adapters.concat(sourceList);
      }
    };

    return Array.from(new Set(adapters));
  };

  calculateEventSources() {
    // for (const eventCode in this.events) {
    //   var retVal: RetVal[] = [];
    //   var eventSources = this.events[eventCode].sources;

    //   for (const adapter in eventSources) {
    //     for (const i of eventSources[adapter]) {
    //       retVal.push({ adapter: adapter, source: eventSources[adapter] });
    //     }
    //   }

    //   this.eventSources[eventCode] = retVal;
    // };
  };

  getSourceForEvents(events: Record<string, Event>) {
    var retval: RetVal[] = [];

    // for (const eventCode in events) {
    //   retval = retval.concat(this.eventSources[eventCode]);
    // };

    return retval;
  };

  submit(trigger: Trigger) {
    if (trigger.filter == "ADAPTER") {
      delete trigger.sources;
    } else if (trigger.filter == "SOURCE") {
      delete trigger.adapters;
      var sources = trigger.sources;
      trigger.sources = {};

      for (const item in sources) {
        var s = item.split("$$");
        var adapter = s[0];
        var source = s[1];
        if (!trigger.sources[item]) trigger.sources[item] = [];
        trigger.sources[item] = [source];
      };
    };

    if (this.triggerId && this.triggerId > -1) {
      this.apiService.Put(this.url, trigger, (returnData) => {
        this.stateService.go('pages.monitors', this.stateParams);
      });
    } else {
      this.apiService.Post(this.url, JSON.stringify(trigger), (returnData) => {
        this.stateService.go('pages.monitors', this.stateParams);
      });
    }
  };
};
