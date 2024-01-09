import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Adapter, AppService } from 'src/app/app.service';

export type Trigger = {
  name: string,
  cronExpression: string,
  repeatInterval: string,
  startTime: string,
  previousFireTime: string,
  nextFireTime: string
}

type Job = {
  name: string
  group: string
  listener: string
  cron: string
  interval: string
  message: string
  description: string
  locker: boolean
  lockkey: string
}

export interface JobForm extends Job {
  adapter: Adapter | null,
}

interface JobResponse extends Job {
  adapter: string,
  configuration: string,
  triggers: Trigger[]
}

@Injectable({
  providedIn: 'root'
})
export class SchedulerService {

  constructor(
    private http: HttpClient,
    private appService: AppService
  ) { }

  getJob(group: string, jobName: string){
    return this.http.get<JobResponse>(this.appService.absoluteApiPath + "schedules/" + group + "/jobs/" + jobName);
  }

  postSchedule(data: FormData){
    return this.http.post(this.appService.absoluteApiPath + "schedules", data);
  }

  putSchedulesAction(action: string){
    return this.http.put(this.appService.absoluteApiPath + "schedules", { action });
  }

  putScheduleJobAction(jobGroup: string, jobName: string, action: string){
    return this.http.put(this.appService.absoluteApiPath + "schedules/" + jobGroup + "/jobs/" + jobName, { action });
  }

  putJob(group: string, jobName: string, data: FormData) {
    return this.http.put(this.appService.absoluteApiPath + "schedules/" + group + "/jobs/" + jobName, data);
  }

  deleteScheduleJob(jobGroup: string, jobName: string) {
    return this.http.delete(this.appService.absoluteApiPath + "schedules/" + jobGroup + "/jobs/" + jobName);
  }


}
