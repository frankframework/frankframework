import { Component, OnDestroy, OnInit } from '@angular/core';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';
import { AppService } from 'src/app/app.service';
import { SchedulerService } from '../scheduler.service';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';
import { NgFor, NgIf } from '@angular/common';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { FormsModule } from '@angular/forms';
import { ConfigurationFilterPipe } from '../../../pipes/configuration-filter.pipe';
import { WithJavaListenerPipe } from '../../../pipes/with-java-listener.pipe';
import { TruncatePipe } from '../../../pipes/truncate.pipe';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';

@Component({
  selector: 'app-scheduler-edit',
  imports: [
    NgbAlert,
    NgFor,
    RouterLink,
    QuickSubmitFormDirective,
    FormsModule,
    ConfigurationFilterPipe,
    WithJavaListenerPipe,
    TruncatePipe,
    NgIf,
    MonacoEditorComponent,
  ],
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-edit.component.scss'],
})
export class SchedulerEditComponent extends SchedulerAddEditParent implements OnInit, OnDestroy {
  override editMode = true;

  private groupName = '';
  private jobName = '';
  private subscriptions: Subscription = new Subscription();

  constructor(
    private route: ActivatedRoute,
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

    this.route.paramMap.subscribe((parameters) => {
      this.groupName = parameters.get('group')!;
      this.jobName = parameters.get('name')!;

      this.schedulerService.getJob(this.groupName, this.jobName).subscribe((data) => {
        this.selectedConfiguration = data.configuration;
        this.form = {
          name: data.name,
          group: data.group,
          adapter: Object.values(this.adapters).find((adapter) => adapter.name === data.adapter) ?? null,
          listener: data.listener,
          cron: data.triggers[0].cronExpression || '',
          interval: data.triggers[0].repeatInterval || '',
          message: data.message,
          description: data.description,
          locker: data.locker,
          lockkey: data.lockkey,
        };
      });
    });
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
    if (this.form.cron !== '') fd.append('cron', this.form.cron);
    if (this.form.interval !== '') fd.append('interval', this.form.interval);
    fd.append('message', this.form.message);
    fd.append('description', this.form.description);
    fd.append('locker', this.form.locker.toString());
    if (this.form.lockkey !== '') fd.append('lockkey', this.form.lockkey);

    this.schedulerService.putJob(this.groupName, this.jobName, fd).subscribe({
      next: () => {
        this.addLocalAlert('success', 'Successfully edited schedule!');
      },
      error: (errorData: HttpErrorResponse) => {
        const error = errorData.error ? errorData.error.error : errorData.message;
        this.addLocalAlert('warning', error);
      },
    }); // TODO no intercept
  }
}
