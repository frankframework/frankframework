import { Component, OnInit } from '@angular/core';
import { Adapter, AppService, Configuration } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { StateParams } from '@uirouter/angularjs';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';

interface StateItem {
  type: string
  message: string
}

interface Form {
  name: string
  group: string
  adapter: string
  listener: string
  cron: string
  interval: string
  message: string
  description: string
  locker: boolean
  lockkey: string
}

@Component({
  selector: 'app-scheduler-edit',
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-edit.component.scss']
})
export class SchedulerEditComponent extends SchedulerAddEditParent implements OnInit {
  url: string = "";
  editMode: boolean = true;

  constructor(
    private Api: ApiService,
    private stateParams: StateParams,
    private appService: AppService
  ) {
    super();
  };

  ngOnInit(): void {
    this.url = "schedules/" + this.stateParams['group'] + "/jobs/" + this.stateParams['name'];

    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.adapters = this.appService.adapters;
    this.appService.adapters$.subscribe(() => { this.adapters = this.appService.adapters; });

    this.Api.Get(this.url, (data) => {
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

    this.Api.Put(this.url, fd, (data) => {
      this.addLocalAlert("success", "Successfully edited schedule!");
    }, (errorData, status, errorMsg) => {
      var error = (errorData) ? errorData.error : errorMsg;
      this.addLocalAlert("warning", error);
    }, false);
  };
}
