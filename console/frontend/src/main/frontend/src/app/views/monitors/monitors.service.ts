import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

export type Monitor = {
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

export type Alarm = {
  source: string
  severity: string
}

export type Trigger = {
  name: string,
  severity: string,
  filter: string,
  period: number,
  sources?: Record<string, string[]>,
  changedSources: string[];
  threshold: number,
  id: number,
  type: string,
  events: string[],
  adapters?: string[]
}

export type Event = {
  sources: Record<string, string[]>;
}

type MonitorsResponse = {
  destinations: string[],
  eventTypes: string[],
  monitors: Monitor[]
}

type TriggerResponse = {
  trigger: Trigger,
  events: Record<string, Event>,
  severities: string[]
}

@Injectable({
  providedIn: 'root'
})
export class MonitorsService {

  constructor(
    private http: HttpClient,
    private appService: AppService
  ) { }

  getUrl(selectedConfiguration: string, monitor: Monitor, trigger?: Trigger) {
    let url = this.appService.absoluteApiPath + "configurations/" + selectedConfiguration + "/monitors/" + monitor.name;
    if (trigger) url += "/triggers/" + trigger.id;
    return url;
  };

  getTriggerUrl(selectedConfiguration: string, monitorName: string, triggerId: number){
    return this.appService.absoluteApiPath + "configurations/" + selectedConfiguration + "/monitors/" + monitorName + "/triggers/" + (triggerId > -1 ? triggerId : "");
  }

  getMonitors(selectedConfiguration: string){
    return this.http.get<MonitorsResponse>(this.appService.absoluteApiPath + "configurations/" + selectedConfiguration + "/monitors");
  }

  getTrigger(selectedConfiguration: string, monitorName: string, triggerId: number){
    return this.http.get<TriggerResponse>(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId));
  }

  postTrigger(selectedConfiguration: string, monitorName: string, triggerId: number, trigger: Trigger) {
    return this.http.post(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId), trigger);
  }

  putMonitorOrTriggerAction(action: string, selectedConfiguration: string, monitor: Monitor, trigger?: Trigger){
    return this.http.put(this.getUrl(selectedConfiguration, monitor, trigger), { action });
  }

  putMonitorOrTriggerEdit(destinations: string[], selectedConfiguration: string, monitor: Monitor, trigger?: Trigger) {
    return this.http.put(this.getUrl(selectedConfiguration, monitor, trigger), { action: "edit", name: monitor.displayName, type: monitor.type, destinations });
  }

  putTriggerUpdate(selectedConfiguration: string, monitorName: string, triggerId: number, trigger: Trigger) {
    return this.http.put(this.getTriggerUrl(selectedConfiguration, monitorName, triggerId), trigger);
  }

  deleteMonitorOrTrigger(selectedConfiguration: string, monitor: Monitor, trigger?: Trigger){
    return this.http.delete(this.getUrl(selectedConfiguration, monitor, trigger));
  }
}
