import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Adapter, AppService } from 'src/app/app.service';

export type Trigger = {
  name: string;
  cronExpression: string;
  repeatInterval: string;
  startTime: string;
  previousFireTime: string;
  nextFireTime: string;
};

type Job = {
  name: string;
  group: string;
  listener: string;
  cron: string;
  interval: string;
  message: string;
  description: string;
  locker: boolean;
  lockkey: string;
};

export interface JobForm extends Job {
  adapter: Adapter | null;
}

interface JobResponse extends Job {
  adapter: string;
  configuration: string;
  triggers: Trigger[];
}

@Injectable({
  providedIn: 'root',
})
export class SchedulerService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  getJob(group: string, jobName: string): Observable<JobResponse> {
    return this.http.get<JobResponse>(`${this.appService.absoluteApiPath}schedules/${group}/jobs/${jobName}`);
  }

  postSchedule(data: FormData): Observable<object> {
    return this.http.post(`${this.appService.absoluteApiPath}schedules`, data);
  }

  putSchedulesAction(action: string): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}schedules`, {
      action,
    });
  }

  putScheduleJobAction(jobGroup: string, jobName: string, action: string): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}schedules/${jobGroup}/jobs/${jobName}`, { action });
  }

  putJob(group: string, jobName: string, data: FormData): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}schedules/${group}/jobs/${jobName}`, data);
  }

  deleteScheduleJob(jobGroup: string, jobName: string): Observable<object> {
    return this.http.delete(`${this.appService.absoluteApiPath}schedules/${jobGroup}/jobs/${jobName}`);
  }
}
