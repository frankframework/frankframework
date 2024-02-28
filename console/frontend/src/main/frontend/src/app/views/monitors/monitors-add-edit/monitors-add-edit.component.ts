import { Component, OnInit } from '@angular/core';
import { Event, MonitorsService, Trigger } from '../monitors.service';
import { AppService } from 'src/app/app.service';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatestWith } from 'rxjs';

type EventSource = {
  adapter: string;
  source: string;
};

@Component({
  selector: 'app-monitors-add-edit',
  templateUrl: './monitors-add-edit.component.html',
  styleUrls: ['./monitors-add-edit.component.scss'],
})
export class MonitorsAddEditComponent implements OnInit {
  loading: boolean = true;
  componentLoading = true;
  selectedConfiguration: string = '';
  monitor: string = '';
  events: Record<string, Event> = {};
  eventsOptions: string[] = [];
  severities: string[] = [];
  triggerId: number = -1;
  trigger: Trigger = {
    name: '',
    severity: '',
    filter: '',
    period: 0,
    sources: {},
    changedSources: [],
    threshold: 0,
    id: 0,
    type: '',
    events: [],
    adapters: [],
  };
  eventSources: Record<string, EventSource[]> = {};
  disabled: boolean = false;
  pageTitle = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private monitorsService: MonitorsService,
  ) {}

  ngOnInit(): void {
    this.appService.loading$.subscribe((_) => (this.loading = false));
    this.route.title.subscribe((title) => {
      this.pageTitle = title ?? '';
    });

    this.route.paramMap
      .pipe(combineLatestWith(this.route.queryParamMap))
      .subscribe(([parameters, queryParameters]) => {
        if (queryParameters.has('configuration')) {
          this.selectedConfiguration = queryParameters.get('configuration')!;
          this.monitor = parameters.get('monitor')!;
          const triggerParameter = parameters.get('trigger')!;
          this.triggerId = triggerParameter === 'new' ? -1 : +triggerParameter;

          this.monitorsService
            .getTrigger(
              this.selectedConfiguration,
              this.monitor,
              this.triggerId,
            )
            .subscribe({
              next: (data) => {
                this.eventsOptions = Object.keys(data.events).sort();
                this.events = data.events;
                this.severities = data.severities;
                if (data.trigger) this.trigger = data.trigger;
                this.calculateEventSources();

                if (data.trigger && data.trigger.sources) {
                  let sources = { ...data.trigger.sources };
                  this.trigger.changedSources = [];
                  this.trigger.adapters = [];

                  for (const adapter in sources) {
                    if (data.trigger.filter == 'SOURCE') {
                      for (const index in sources[adapter]) {
                        this.trigger.changedSources.push(
                          adapter + '$$' + sources[adapter][index],
                        );
                      }
                    } else {
                      this.trigger.adapters.push(adapter);
                    }
                  }
                }
                this.componentLoading = false;
              },
              error: () => this.navigateBack(),
            });
        } else {
          this.router.navigate(['/monitors']);
        }
      });
  }

  navigateBack() {
    this.router.navigate(['monitors'], {
      queryParams: { configuration: this.selectedConfiguration },
    });
  }

  getAdaptersForEvents(events: string[]) {
    if (!events) return [];
    let adapters: string[] = [];

    for (const item in this.events) {
      if (events.includes(item)) {
        let sourceList = this.events[item].sources;
        adapters = [...adapters, ...Object.keys(sourceList)];
      }
    }

    return [...new Set(adapters)];
  }

  calculateEventSources() {
    for (const eventCode in this.events) {
      let returnValue: EventSource[] = [];
      let eventSources = this.events[eventCode].sources;

      for (const adapter in eventSources) {
        for (const index in eventSources[adapter]) {
          returnValue.push({ adapter, source: eventSources[adapter][index] });
        }
      }

      this.eventSources[eventCode] = returnValue;
    }
  }

  getSourceForEvents(events: string[]) {
    let returnValue: EventSource[] = [];

    for (const eventCode of events) {
      returnValue = [...returnValue, ...this.eventSources[eventCode]];
    }

    return returnValue;
  }

  submit(trigger: Trigger) {
    if (trigger.filter == 'ADAPTER') {
      delete trigger.sources;
    } else if (trigger.filter == 'SOURCE') {
      delete trigger.adapters;
      let sources = trigger.changedSources;
      trigger.sources = {};

      for (const item of sources) {
        let s = item.split('$$');
        let adapter = s[0];
        let source = s[1];
        if (!trigger.sources[item]) trigger.sources[item] = [];
        trigger.sources[item] = [source];
      }
    }

    if (this.triggerId && this.triggerId > -1) {
      this.monitorsService
        .putTriggerUpdate(
          this.selectedConfiguration,
          this.monitor,
          this.triggerId,
          trigger,
        )
        .subscribe(() => {
          this.navigateBack();
        });
    } else {
      this.monitorsService
        .postTrigger(
          this.selectedConfiguration,
          this.monitor,
          this.triggerId,
          trigger,
        )
        .subscribe(() => {
          this.navigateBack();
        });
    }
  }
}
