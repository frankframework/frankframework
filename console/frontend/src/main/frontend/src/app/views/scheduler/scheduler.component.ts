import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppService, JobMessage, RunState } from 'src/app/app.service';
import { PollerService } from 'src/app/services/poller.service';
import { SchedulerService, Trigger } from './scheduler.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';

type Scheduler = {
  name: string
  version: string
  started: boolean
  state: string
  runningSince: string
  jobsExecuted: string[]
  isSchedulerRemote: boolean
  instanceId: string
  jobStoreSupportsPersistence: boolean
  threadPoolSize: number
  schedulerClass: string
  jobStoreClass: string
}

export type JobState = 'NONE' | 'NORMAL' | 'PAUSED' | 'COMPLETE' | 'ERROR' | 'BLOCKED';

export type Job = {
  name: string,
  description: string,
  state: JobState,
  type?: string,
  jobGroupName?: string,
  stateful?: boolean,
  durable?: boolean,
  messages: JobMessage[],
  triggers: Trigger[]
}

@Component({
  selector: 'app-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss']
})
export class SchedulerComponent implements OnInit, OnDestroy {
  jobs: Record<string, Job[]> = {};
  scheduler: Scheduler = {
    name: "",
    version: "",
    started: false,
    state: "",
    runningSince: "",
    jobsExecuted: [],
    isSchedulerRemote: false,
    instanceId: "",
    jobStoreSupportsPersistence: false,
    threadPoolSize: 0,
    schedulerClass: "",
    jobStoreClass: ""
  };
  searchFilter: string = "";
  refreshing: boolean = false;
  databaseSchedulesEnabled: boolean = this.appService.databaseSchedulesEnabled;
  jobShowContent: Record<keyof typeof this.jobs, boolean> = {}

  private initialized = false;

  constructor(
    private router: Router,
    private pollerService: PollerService,
    private sweetAlertService: SweetalertService,
    private appService: AppService,
    private schedulerService: SchedulerService
  ) { };

  ngOnInit(): void {
    this.pollerService.add("schedules", (data) => {
      this.scheduler = data.scheduler;
      this.jobs = data.jobs;

      this.refreshing = false;
      if (!this.initialized) {
        for (const job of Object.keys(this.jobs)) {
          this.jobShowContent[job] = true;
        }
        this.initialized = true;
      }
    }, true, 5000);

    this.appService.databaseSchedulesEnabled$.subscribe(() => {
      this.databaseSchedulesEnabled = this.appService.databaseSchedulesEnabled;
    });
  };

  ngOnDestroy() {
    this.pollerService.remove("schedules");
  };

  showContent(job: keyof typeof this.jobs) {
    return this.jobShowContent[job];
  }

  start() {
    this.refreshing = true;
    this.schedulerService.putSchedulesAction("start").subscribe();
  };

  pauseScheduler() {
    this.refreshing = true;
    this.schedulerService.putSchedulesAction("pause").subscribe();
  };

  pause(jobGroup: string, jobName: string) {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, "pause").subscribe();
  };

  resume(jobGroup: string, jobName: string) {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, "resume").subscribe();
  };

  remove(jobGroup: string, jobName: string) {
    this.sweetAlertService.Confirm({ title: "Please confirm the deletion of '" + jobName + "'" }).then((result) => {
      if (result.isConfirmed) {
        this.schedulerService.deleteScheduleJob(jobGroup, jobName).subscribe();
      }
    });
  };

  trigger(jobGroup: string, jobName: string) {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, "trigger").subscribe();
  };

  edit(jobGroup: string, jobName: string) {
    this.router.navigate(['scheduler', 'edit', jobGroup, jobName]);
  };
}
