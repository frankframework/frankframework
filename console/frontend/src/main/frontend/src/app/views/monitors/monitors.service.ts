import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';

export type Monitor = {
  hits: number;
  destinations: string[];
  name: string;
  raised: boolean;
  type: string;
  lastHit: string;
  triggers: Trigger[];
  displayName: string;
  activeDestinations: Record<string, boolean>;
  edit: boolean;
  alarm: Alarm;
};

export type NewMonitor = {
  name: string;
};

export type Alarm = {
  source: string;
  severity: string;
};

export type Trigger = {
  name: string;
  severity: string;
  filter: string;
  period: number;
  sources?: Record<string, string[]>;
  changedSources: string[];
  threshold: number;
  id: number;
  type: string;
  events: string[];
  adapters?: string[];
};

export type Event = {
  sources: Record<string, string[]>;
};

type MonitorsResponse = {
  destinations: string[];
  eventTypes: string[];
  monitors: Monitor[];
};

type TriggerResponse = {
  trigger: Trigger;
  events: Record<string, Event>;
  severities: string[];
};

@Injectable({
  providedIn: 'root',
})
export class MonitorsService {
  private http = inject(HttpClient);
  private appService = inject(AppService);

  getUrl(selectedConfiguration: string, monitor: Monitor, trigger?: Trigger): string {
    let url = `${this.appService.absoluteApiPath}configurations/${selectedConfiguration}/monitors/${monitor.name}`;
    if (trigger) url += `/triggers/${trigger.id}`;
    return url;
  }

  getTriggerUrl(selectedConfiguration: string, monitorName: string, triggerId: number): string {
    let url = `${
      this.appService.absoluteApiPath
    }configurations/${selectedConfiguration}/monitors/${monitorName}/triggers`;
    if (triggerId > -1) url += `/${triggerId}`;
    return url;
  }

  getMonitors(selectedConfiguration: string): Observable<MonitorsResponse> {
    return this.http.get<MonitorsResponse>(
      `${this.appService.absoluteApiPath}configurations/${selectedConfiguration}/monitors`,
    );
  }

  getTrigger(selectedConfiguration: string, monitorName: string, triggerId: number): Observable<TriggerResponse> {
    return this.http.get<TriggerResponse>(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId));
  }

  postMonitor(selectedConfiguration: string, monitor: NewMonitor): Observable<object> {
    return this.http.post<object>(
      `${this.appService.absoluteApiPath}configurations/${selectedConfiguration}/monitors`,
      monitor,
    );
  }

  postTrigger(
    selectedConfiguration: string,
    monitorName: string,
    triggerId: number,
    trigger: Trigger,
  ): Observable<object> {
    return this.http.post(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId), trigger);
  }

  putMonitorOrTriggerAction(
    action: string,
    selectedConfiguration: string,
    monitor: Monitor,
    trigger?: Trigger,
  ): Observable<object> {
    return this.http.put(this.getUrl(selectedConfiguration, monitor, trigger), {
      action,
    });
  }

  putMonitorOrTriggerEdit(
    destinations: string[],
    selectedConfiguration: string,
    monitor: Monitor,
    trigger?: Trigger,
  ): Observable<object> {
    return this.http.put(this.getUrl(selectedConfiguration, monitor, trigger), {
      action: 'edit',
      name: monitor.displayName,
      type: monitor.type,
      destinations,
    });
  }

  putTriggerUpdate(
    selectedConfiguration: string,
    monitorName: string,
    triggerId: number,
    trigger: Trigger,
  ): Observable<object> {
    return this.http.put(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId), trigger);
  }

  deleteMonitorOrTrigger(selectedConfiguration: string, monitor: Monitor, trigger?: Trigger): Observable<object> {
    return this.http.delete(this.getUrl(selectedConfiguration, monitor, trigger));
  }
}
