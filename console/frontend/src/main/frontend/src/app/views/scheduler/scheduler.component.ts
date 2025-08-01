import { Component, computed, OnDestroy, OnInit, Signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AppService, Message } from 'src/app/app.service';
import { PollerService } from 'src/app/services/poller.service';
import { SchedulerService, Trigger } from './scheduler.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { KeyValuePipe, NgClass } from '@angular/common';
import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';
import { LaddaModule } from 'angular2-ladda';
import { ToDateDirective } from '../../components/to-date.directive';
import { TabListComponent } from '../../components/tab-list/tab-list.component';
import { FormsModule } from '@angular/forms';
import { SearchFilterPipe } from '../../pipes/search-filter.pipe';
import { OrderByPipe } from '../../pipes/orderby.pipe';

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

export interface JobMessage extends Message {
  text: string;
}

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
  imports: [
    HasAccessToLinkDirective,
    LaddaModule,
    ToDateDirective,
    TabListComponent,
    FormsModule,
    KeyValuePipe,
    SearchFilterPipe,
    OrderByPipe,
    RouterLink,
    NgClass,
  ],
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss'],
})
export class SchedulerComponent implements OnInit, OnDestroy {
  protected jobGroups: Record<string, Job[]> = {};
  protected jobGroupNames: string[] = [];
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
  protected databaseSchedulesEnabled: Signal<boolean> = computed(
    () => this.appService.appConstants()['loadDatabaseSchedules.active'] === 'true',
  );
  protected jobShowContent: Record<keyof typeof this.jobGroups, boolean> = {};
  protected selectedJobGroup: string = 'All';

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
        this.jobGroupNames = Object.keys(this.jobGroups);
        this.sortJobMessages();

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

  private sortJobMessages(): void {
    for (const jobs of Object.values(this.jobGroups)) {
      for (const job of jobs) {
        job.messages.sort((a, b) => b.date - a.date);
      }
    }
  }
}
