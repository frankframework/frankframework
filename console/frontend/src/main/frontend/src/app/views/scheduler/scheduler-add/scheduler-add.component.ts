import { Component, OnInit } from '@angular/core';
import { SchedulerAddEditParent } from '../scheduler-add-edit-parent';
import { AppService } from 'src/app/app.service';

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
  selector: 'app-scheduler-add',
  templateUrl: '../scheduler-add-edit-parent.component.html',
  styleUrls: ['./scheduler-add.component.scss']
})
export class SchedulerAddComponent extends SchedulerAddEditParent implements OnInit {
  editMode: boolean = false;

  constructor(
    private appService: AppService,
  ) {
    super();
  };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.adapters = this.appService.adapters;
    this.appService.adapters$.subscribe(() => { this.adapters = this.appService.adapters; });
  };

  submit() {
    var fd = new FormData();
    this.state = [];

    fd.append("name", this.form.name);
    fd.append("group", this.form.group);
    fd.append("configuration", this.selectedConfiguration);
    fd.append("adapter", this.form.adapter);
    fd.append("listener", this.form.listener);
    fd.append("cron", this.form.cron);
    fd.append("interval", this.form.interval);
    fd.append("message", this.form.message);
    fd.append("description", this.form.description);
    fd.append("locker", this.form.locker.toString());
    fd.append("lockkey", this.form.lockkey);

    this.apiService.Post("schedules", fd, (data) => {
      this.addLocalAlert("success", "Successfully added schedule!");
      this.selectedConfiguration = "";
      this.form = {
        name: "",
        group: "",
        adapter: "",
        listener: "",
        cron: "",
        interval: "",
        message: "",
        description: "",
        locker: false,
        lockkey: "",
      };
    }, (errorData, status, errorMsg) => {
      var error = (errorData) ? errorData.error : errorMsg;
      this.addLocalAlert("warning", error);
    }, false);
  };
};
