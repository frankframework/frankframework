import { Component, OnInit } from '@angular/core';
import { ApiService } from "src/angularjs/app/services/api.service";
import { Adapter, AppService } from "src/app/app.service";
import { StateParams } from '@uirouter/angularjs';
import { StateService } from "@uirouter/angularjs";
import { Trigger } from '../monitors.component';
import { state } from '@angular/animations';

interface EventSource {
  adapter: string,
  source: string
}

interface Event {
  sources: Record<string, string[]>;
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
    type: "ALARM",
    events: [],
    adapters: []
  }
  eventSources: Record<string, EventSource[]> = {};
  url: string = "";
  disabled: boolean = false;
  pageTitle = "";

  constructor(
    private appService: AppService,
    private apiService: ApiService,
    private $state: StateService,
    private stateParams: StateParams,
  ) { };

  ngOnInit(): void {
    this.appService.loading$.subscribe(_ => this.loading = false);
    this.pageTitle = this.$state.current.data.pageTitle;

    if (this.stateParams['configuration'] == "" || this.stateParams['monitor'] == "") {
      this.$state.go('pages.monitors');
    } else {
      this.selectedConfiguration = this.stateParams['configuration'];
      this.monitor = this.stateParams['monitor'];
      this.triggerId = this.stateParams['trigger'] || "";
      this.url = "configurations/" + this.selectedConfiguration + "/monitors/" + this.monitor + "/triggers/" + this.triggerId;

      this.apiService.Get(this.url, (data) => {
        // Object.assign(this, data);
        this.events = data.events;
        this.severities = data.severities;
        this.trigger = data.trigger;
        this.calculateEventSources();

        if (data.trigger && data.trigger.sources) {
          let sources = data.trigger.sources;
          this.trigger.sources = {};
          this.trigger.adapters = [];

          for (const adapter in sources) {
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
        this.$state.go('pages.monitors', this.stateParams);
      });
    }
  };

  getAdaptersForEvents(events: string[]) {
    if (!events) return [];
    let adapters: string[] = [];

    for (const item in this.events) {
      if (events.indexOf(item) > -1) {
        let sourceList = this.events[item].sources;
        adapters = adapters.concat(Object.keys(sourceList));
      }
    };

    return Array.from(new Set(adapters));
  };

  calculateEventSources() {
    for (const eventCode in this.events) {
      let retVal: EventSource[] = [];
      let eventSources = this.events[eventCode].sources;

      for (const adapter in eventSources) {
        for (const i in eventSources[adapter]) {
          retVal.push({ adapter: adapter, source: eventSources[adapter][i] });
        }
      }

      this.eventSources[eventCode] = retVal;
    };
  };

  getSourceForEvents(events: string[]) {
    let retval: EventSource[] = [];

    for (const eventCode of events) {
      retval = retval.concat(this.eventSources[eventCode]);
    };

    return retval;
  };

  submit(trigger: Trigger) {
    if (trigger.filter == "ADAPTER") {
      delete trigger.sources;
    } else if (trigger.filter == "SOURCE") {
      delete trigger.adapters;
      let sources = trigger.sources;
      trigger.sources = {};

      for (const item in sources) {
        let s = item.split("$$");
        let adapter = s[0];
        let source = s[1];
        if (!trigger.sources[item]) trigger.sources[item] = [];
        trigger.sources[item] = [source];
      };
    };

    if (this.triggerId && this.triggerId > -1) {
      this.apiService.Put(this.url, trigger, (returnData) => {
        this.$state.go('pages.monitors', this.stateParams);
      });
    } else {
      this.apiService.Post(this.url, JSON.stringify(trigger), (returnData) => {
        this.$state.go('pages.monitors', this.stateParams);
      });
    }
  };
};
