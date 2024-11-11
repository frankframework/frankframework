import { Component, OnInit } from '@angular/core';
import { Event, MonitorsService, Trigger } from '../monitors.service';
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
  protected componentLoading = true;
  protected selectedConfiguration: string = '';
  protected monitor: string = '';
  protected eventsOptions: string[] = [];
  protected severities: string[] = [];
  protected trigger: Trigger = {
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
  protected disabled: boolean = false;
  protected pageTitle = '';

  private triggerId: number = -1;
  private events: Record<string, Event> = {};
  private eventSources: Record<string, EventSource[]> = {};

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private monitorsService: MonitorsService,
  ) {}

  ngOnInit(): void {
    this.route.title.subscribe((title) => {
      this.pageTitle = title ?? '';
    });

    this.route.paramMap.pipe(combineLatestWith(this.route.queryParamMap)).subscribe(([parameters, queryParameters]) => {
      if (queryParameters.has('configuration')) {
        this.selectedConfiguration = queryParameters.get('configuration')!;
        this.monitor = parameters.get('monitor')!;
        const triggerParameter = parameters.get('trigger')!;
        this.triggerId = triggerParameter === 'new' ? -1 : +triggerParameter;
      } else {
        this.router.navigate(['/monitors']);
      }
    });

    this.monitorsService.getTrigger(this.selectedConfiguration, this.monitor, this.triggerId).subscribe({
      next: (data) => {
        this.eventsOptions = Object.keys(data.events).sort();
        this.events = data.events;
        this.severities = data.severities;
        if (data.trigger) this.trigger = data.trigger;
        this.calculateEventSources();

        if (data.trigger && data.trigger.sources) {
          const sources = { ...data.trigger.sources };
          this.trigger.changedSources = [];
          this.trigger.adapters = [];

          for (const adapter in sources) {
            if (data.trigger.filter == 'SOURCE') {
              for (const index in sources[adapter]) {
                this.trigger.changedSources.push(`${adapter}$$${sources[adapter][index]}`);
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
  }

  navigateBack(): void {
    this.router.navigate(['monitors'], {
      queryParams: { configuration: this.selectedConfiguration },
    });
  }

  getAdaptersForEvents(events: string[]): string[] {
    if (!events) return [];
    let adapters: string[] = [];

    for (const item in this.events) {
      if (events.includes(item)) {
        const sourceList = this.events[item].sources;
        adapters = [...adapters, ...Object.keys(sourceList)];
      }
    }

    return [...new Set(adapters)];
  }

  calculateEventSources(): void {
    for (const eventCode in this.events) {
      const returnValue: EventSource[] = [];
      const eventSources = this.events[eventCode].sources;

      for (const adapter in eventSources) {
        for (const index in eventSources[adapter]) {
          returnValue.push({ adapter, source: eventSources[adapter][index] });
        }
      }

      this.eventSources[eventCode] = returnValue;
    }
  }

  getSourceForEvents(events: string[]): EventSource[] {
    let returnValue: EventSource[] = [];

    for (const eventCode of events) {
      returnValue = [...returnValue, ...this.eventSources[eventCode]];
    }

    return returnValue;
  }

  submit(trigger: Trigger): void {
    if (trigger.filter == 'ADAPTER') {
      delete trigger.sources;
    } else if (trigger.filter == 'SOURCE') {
      delete trigger.adapters;
      const sources = trigger.changedSources;
      trigger.sources = {};

      for (const item of sources) {
        const s = item.split('$$');
        // const adapter = s[0];
        const source = s[1];
        if (!trigger.sources[item]) trigger.sources[item] = [];
        trigger.sources[item] = [source];
      }
    }

    if (this.triggerId > -1) {
      this.monitorsService
        .putTriggerUpdate(this.selectedConfiguration, this.monitor, this.triggerId, trigger)
        .subscribe(() => {
          this.navigateBack();
        });
    } else {
      this.monitorsService
        .postTrigger(this.selectedConfiguration, this.monitor, this.triggerId, trigger)
        .subscribe(() => {
          this.navigateBack();
        });
    }
  }
}
