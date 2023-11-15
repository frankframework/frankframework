import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

@Injectable({
  providedIn: 'root'
})
export class SchedulerService {

  constructor(
    private http: HttpClient,
    private appService: AppService
  ) { }

  putSchedulesAction(action: string){
    return this.http.put(this.appService.absoluteApiPath + "schedules", { action });
  }

  putScheduleJobAction(jobGroup: string, jobName: string, action: string){
    return this.http.put(this.appService.absoluteApiPath + "schedules/" + jobGroup + "/jobs/" + jobName, { action });
  }

  deleteScheduleJob(jobGroup: string, jobName: string) {
    return this.http.delete(this.appService.absoluteApiPath + "schedules/" + jobGroup + "/jobs/" + jobName);
  }


}
