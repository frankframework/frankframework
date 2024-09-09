import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppService, JobMessage } from 'src/app/app.service';
import { PollerService } from 'src/app/services/poller.service';
import { SchedulerService, Trigger } from './scheduler.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';

type Scheduler = {
  name: string;
  version: string;
  started: boolean;
  state: string;
  runningSince: string;
  jobsExecuted: string[];
  isSchedulerRemote: boolean;
  instanceId: string;
  jobStoreSupportsPersistence: boolean;
  threadPoolSize: number;
  schedulerClass: string;
  jobStoreClass: string;
};

export type JobState = 'NONE' | 'NORMAL' | 'PAUSED' | 'COMPLETE' | 'ERROR' | 'BLOCKED';

export type Job = {
  name: string;
  description: string;
  state: JobState;
  type?: string;
  jobGroupName?: string;
  stateful?: boolean;
  durable?: boolean;
  messages: JobMessage[];
  triggers: Trigger[];
};

@Component({
  selector: 'app-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss'],
})
export class SchedulerComponent implements OnInit, OnDestroy {
  protected jobGroups: Record<string, Job[]> = {};
  protected scheduler: Scheduler = {
    name: '',
    version: '',
    started: false,
    state: '',
    runningSince: '',
    jobsExecuted: [],
    isSchedulerRemote: false,
    instanceId: '',
    jobStoreSupportsPersistence: false,
    threadPoolSize: 0,
    schedulerClass: '',
    jobStoreClass: '',
  };
  protected searchFilter: string = '';
  protected refreshing: boolean = false;
  protected databaseSchedulesEnabled: boolean = this.appService.databaseSchedulesEnabled;
  protected jobShowContent: Record<keyof typeof this.jobGroups, boolean> = {};

  private initialized = false;

  constructor(
    private router: Router,
    private pollerService: PollerService,
    private sweetAlertService: SweetalertService,
    private appService: AppService,
    private schedulerService: SchedulerService,
  ) {}

  ngOnInit(): void {
    this.pollerService.add(
      'schedules',
      (data) => {
        const result = data as {
          scheduler: Scheduler;
          jobs: Record<string, Job[]>;
        };
        this.scheduler = result.scheduler;
        this.jobGroups = result.jobs;

        this.refreshing = false;
        if (!this.initialized) {
          for (const job of Object.keys(this.jobGroups)) {
            this.jobShowContent[job] = true;
          }
          this.initialized = true;
        }
      },
      5000,
    );

    this.appService.databaseSchedulesEnabled$.subscribe(() => {
      this.databaseSchedulesEnabled = this.appService.databaseSchedulesEnabled;
    });
  }

  ngOnDestroy(): void {
    this.pollerService.remove('schedules');
  }

  showContent(job: keyof typeof this.jobGroups): boolean {
    return this.jobShowContent[job];
  }

  start(): void {
    this.refreshing = true;
    this.schedulerService.putSchedulesAction('start').subscribe();
  }

  pauseScheduler(): void {
    this.refreshing = true;
    this.schedulerService.putSchedulesAction('pause').subscribe();
  }

  pause(jobGroup: string, jobName: string): void {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, 'pause').subscribe();
  }

  resume(jobGroup: string, jobName: string): void {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, 'resume').subscribe();
  }

  remove(jobGroup: string, jobName: string): void {
    this.sweetAlertService.Confirm({ title: `Please confirm the deletion of '${jobName}'` }).then((result) => {
      if (result.isConfirmed) {
        this.schedulerService.deleteScheduleJob(jobGroup, jobName).subscribe();
      }
    });
  }

  trigger(jobGroup: string, jobName: string): void {
    this.schedulerService.putScheduleJobAction(jobGroup, jobName, 'trigger').subscribe();
  }

  edit(jobGroup: string, jobName: string): void {
    this.router.navigate(['scheduler', 'edit', jobGroup, jobName]);
  }
}
