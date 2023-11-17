import { Component, OnInit } from '@angular/core';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';
import { AppService } from 'src/app/app.service';
import { SchedulerService } from '../scheduler.service';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-scheduler-edit',
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-edit.component.scss']
})
export class SchedulerEditComponent extends SchedulerAddEditParent implements OnInit {
  override editMode = true;

  private groupName = "";
  private jobName = "";

  constructor(
    private route: ActivatedRoute,
    private appService: AppService,
    private schedulerService: SchedulerService
  ) {
    super();
  };

  ngOnInit() {

    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.adapters = this.appService.adapters;
    this.appService.adapters$.subscribe(() => { this.adapters = this.appService.adapters; });

    this.route.paramMap.subscribe(params => {
      this.groupName = params.get('group')!;
      this.jobName = params.get('name')!;

      this.schedulerService.getJob(this.groupName, this.jobName).subscribe((data) => {
        this.selectedConfiguration = data.configuration;
        this.form = {
          name: data.name,
          group: data.group,
          adapter: data.adapter,
          listener: data.listener,
          cron: data.triggers[0].cronExpression || "",
          interval: data.triggers[0].repeatInterval || "",
          message: data.message,
          description: data.description,
          locker: data.locker,
          lockkey: data.lockkey,
        };
      });
    });
  };

  submit() {
    var fd = new FormData();
    this.state = [];

    fd.append("name", this.form.name);
    fd.append("group", this.form.group);
    fd.append("configuration", this.selectedConfiguration);
    fd.append("adapter", this.form.adapter);
    fd.append("listener", this.form.listener);

    if (this.form.cron)
      fd.append("cron", this.form.cron);

    if (this.form.interval)
      fd.append("interval", this.form.interval);

    fd.append("message", this.form.message);
    fd.append("description", this.form.description);
    fd.append("locker", this.form.locker.toString());

    if (this.form.lockkey)
      fd.append("lockkey", this.form.lockkey);

    this.schedulerService.putJob(this.groupName, this.jobName, fd).subscribe({ next: (data) => {
      this.addLocalAlert("success", "Successfully edited schedule!");
    }, error: (errorData: HttpErrorResponse) => {
      var error = (errorData.error) ? errorData.error.error : errorData.message;
      this.addLocalAlert("warning", error);
    }}); // TODO no intercept
  };
}
