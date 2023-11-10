import { Component, OnDestroy, OnInit } from '@angular/core';
import { Adapter, AppService, Job } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { PollerService } from 'src/angularjs/app/services/poller.service';
import { SweetAlertService } from 'src/angularjs/app/services/sweetalert.service';
import { StateService } from "@uirouter/angularjs";

interface Scheduler {
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
    private apiService: ApiService,
    private pollerService: PollerService,
    private stateService: StateService,
    private sweetAlertService: SweetAlertService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.pollerService.add("schedules", (data) => {
      Object.assign(this, data);
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
    this.apiService.Put("schedules", { action: "start" });
  };

  pauseScheduler() {
    this.refreshing = true;
    this.apiService.Put("schedules", { action: "pause" });
  };

  pause(jobGroup: string, jobName: string) {
    this.apiService.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "pause" });
  };

  resume(jobGroup: string, jobName: string) {
    this.apiService.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "resume" });
  };

  remove(jobGroup: string, jobName: string) {
    this.sweetAlertService.Confirm({ title: "Please confirm the deletion of '" + jobName + "'" }, (imSure: boolean) => {
      if (imSure) {
        this.apiService.Delete("schedules/" + jobGroup + "/jobs/" + jobName);
      }
    });
  };

  trigger(jobGroup: string, jobName: string) {
    this.apiService.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "trigger" });
  };

  edit(jobGroup: string, jobName: string) {
    this.stateService.go('pages.edit_schedule', { name: jobName, group: jobGroup });
  };
}
