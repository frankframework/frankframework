import { AppService } from "src/angularjs/app/app.service";
import { ApiService } from "src/angularjs/app/services/api.service";
import { appModule } from "../../../app.module";
import { StateParams } from '@uirouter/angularjs';

interface StateItem {
  type: string
  message: string
}

class SchedulerEditController {
  state: StateItem[] = [];
  editMode = true;
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
  configurations: any;
  adapters: any;
  url: string = "";

  constructor(
    private Api: ApiService,
    private $stateParams: StateParams,
    private appService: AppService
  ) { };

  $onInit() {
    this.url = "schedules/" + this.$stateParams['group'] + "/jobs/" + this.$stateParams['name'];

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

  submit(form: FormData) {
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

  addLocalAlert(type: string, message: string) {
    this.state.push({ type: type, message: message });
  };
};

appModule.component('schedulerEdit', {
  controller: ['Api', '$stateParams', 'appService', SchedulerEditController],
  templateUrl: 'js/app/views/scheduler/scheduler-add-edit.component.html'
});
