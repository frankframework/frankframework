import { Component, OnDestroy, OnInit } from '@angular/core';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';
import { AppService, ServerErrorResponse } from 'src/app/app.service';
import { SchedulerService } from '../scheduler.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-scheduler-add',
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-add.component.scss'],
  standalone: false,
})
export class SchedulerAddComponent extends SchedulerAddEditParent implements OnInit, OnDestroy {
  private subscriptions: Subscription = new Subscription();

  constructor(
    private appService: AppService,
    private schedulerService: SchedulerService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    const configurationsSubscription = this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
    });
    this.subscriptions.add(configurationsSubscription);

    this.adapters = this.appService.adapters;
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.adapters = this.appService.adapters;
    });
    this.subscriptions.add(adaptersSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  submit(): void {
    const fd = new FormData();
    this.state = [];

    fd.append('name', this.form.name);
    fd.append('group', this.form.group);
    fd.append('configuration', this.selectedConfiguration);
    fd.append('adapter', this.form.adapter?.name ?? '');
    fd.append('listener', this.form.listener);
    fd.append('cron', this.form.cron);
    fd.append('interval', this.form.interval);
    fd.append('message', this.form.message);
    fd.append('description', this.form.description);
    fd.append('locker', this.form.locker.toString());
    fd.append('lockkey', this.form.lockkey);

    this.schedulerService.postSchedule(fd).subscribe({
      next: () => {
        this.state = [];
        this.addLocalAlert('success', 'Successfully added schedule!');
        this.selectedConfiguration = '';
        this.form = {
          name: '',
          group: '',
          adapter: null,
          listener: '',
          cron: '',
          interval: '',
          message: '',
          description: '',
          locker: false,
          lockkey: '',
        };
      },
      error: (errorData: HttpErrorResponse) => {
        let error = '';
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          error = errorData.message;
        }
        this.addLocalAlert('warning', error);
      },
    }); //TODO no intercept
  }
}
