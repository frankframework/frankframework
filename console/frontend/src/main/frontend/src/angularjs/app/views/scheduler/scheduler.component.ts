import { appModule } from "../../app.module";
import { AppService } from "../../app.service";
import { ApiService } from "../../services/api.service";
import { PollerService } from "../../services/poller.service";
import { SweetAlertService } from "../../services/sweetalert.service";
import { StateService } from "@uirouter/angularjs";

class SchedulerController {
  jobs = {};
  scheduler = {};
  searchFilter = "";
  refreshing = false;
  databaseSchedulesEnabled = this.appService.databaseSchedulesEnabled;

  constructor(
    private Api: ApiService,
    private Poller: PollerService,
    private $state: StateService,
    private SweetAlert: SweetAlertService,
    private appService: AppService,
  ) { };

  $onInit() {
    this.Poller.add("schedules", (data) => {
      Object.assign(this, data);
      this.refreshing = false;
    }, true, 5000);

    this.appService.databaseSchedulesEnabled$.subscribe(() => {
      this.databaseSchedulesEnabled = this.appService.databaseSchedulesEnabled;
    });
  };

  $onDestroy() {
    this.Poller.remove("schedules");
  };

  start() {
    this.refreshing = true;
    this.Api.Put("schedules", { action: "start" });
  };

  pauseScheduler() {
    this.refreshing = true;
    this.Api.Put("schedules", { action: "pause" });
  };

  pause(jobGroup: string, jobName: string) {
    this.Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "pause" });
  };

  resume(jobGroup: string, jobName: string) {
    this.Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "resume" });
  };

  remove(jobGroup: string, jobName: string) {
    this.SweetAlert.Confirm({ title: "Please confirm the deletion of '" + jobName + "'" }, (imSure: boolean) => {
      if (imSure) {
        this.Api.Delete("schedules/" + jobGroup + "/jobs/" + jobName);
      }
    });
  };

  trigger(jobGroup: string, jobName: string) {
    this.Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "trigger" });
  };

  edit(jobGroup: string, jobName: string) {
    this.$state.go('pages.edit_schedule', { name: jobName, group: jobGroup });
  };
};

appModule.component('scheduler', {
  controller: ['$rootScope', '$timeout', 'Api', 'Poller', '$state', 'SweetAlert', 'appService', SchedulerController],
  templateUrl: 'js/app/views/scheduler/scheduler.component.html'
});
