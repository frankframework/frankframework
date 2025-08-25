import { Component, inject } from '@angular/core';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';
import { ServerErrorResponse } from 'src/app/app.service';
import { SchedulerService } from '../scheduler.service';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbAlert } from '@ng-bootstrap/ng-bootstrap';

import { RouterLink } from '@angular/router';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { FormsModule } from '@angular/forms';
import { ConfigurationFilterPipe } from '../../../pipes/configuration-filter.pipe';
import { WithJavaListenerPipe } from '../../../pipes/with-java-listener.pipe';
import { TruncatePipe } from '../../../pipes/truncate.pipe';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';
import { faArrowAltCircleLeft } from '@fortawesome/free-regular-svg-icons';

@Component({
  selector: 'app-scheduler-add',
  imports: [
    NgbAlert,
    RouterLink,
    QuickSubmitFormDirective,
    FormsModule,
    ConfigurationFilterPipe,
    WithJavaListenerPipe,
    TruncatePipe,
    MonacoEditorComponent,
  ],
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-add.component.scss'],
})
export class SchedulerAddComponent extends SchedulerAddEditParent {
  protected readonly faArrowAltCircleLeft = faArrowAltCircleLeft;
  private readonly schedulerService: SchedulerService = inject(SchedulerService);

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
