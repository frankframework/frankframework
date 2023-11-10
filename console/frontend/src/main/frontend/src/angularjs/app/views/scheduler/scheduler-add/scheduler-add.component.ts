import { AppService } from "src/angularjs/app/app.service";
import { ApiService } from "src/angularjs/app/services/api.service";
import { appModule } from "../../../app.module";

interface StateItem {
  type: string
  message: string
}

class SchedulerAddController {
  state: StateItem[] = [];
  selectedConfiguration = "";
  form = {
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
  configurations = {};
  adapters = {};

  constructor(
    private Api: ApiService,
    private appService: AppService,
  ) { };

  $onInit() {
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

    this.Api.Post("schedules", fd, (data) => {
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

  addLocalAlert(type: string, message: string) {
    this.state.push({ type: type, message: message });
  };
};

appModule.component('schedulerAdd', {
  controller: ['Api', 'appService', SchedulerAddController],
  templateUrl: 'js/app/views/scheduler/scheduler-add-edit.component.html'
});
