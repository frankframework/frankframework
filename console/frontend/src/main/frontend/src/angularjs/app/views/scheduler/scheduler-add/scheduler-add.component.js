import { appModule } from "../../../app.module";

const SchedulerAddController = function ($scope, Api, $rootScope, appService) {
  const ctrl = this;

  ctrl.state = [];
  ctrl.selectedConfiguration = "";
  ctrl.form = {
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

  ctrl.$onInit = function () {
    ctrl.configurations = appService.configurations;
    appService.configurations$.subscribe(function () { ctrl.configurations = appService.configurations; });

    ctrl.adapters = appService.adapters;
    appService.adapters$.subscribe(function () { ctrl.adapters = appService.adapters; });
  };

  ctrl.submit = function () {
    var fd = new FormData();
    ctrl.state = [];

    fd.append("name", ctrl.form.name);
    fd.append("group", ctrl.form.group);
    fd.append("configuration", ctrl.selectedConfiguration);
    fd.append("adapter", ctrl.form.adapter);
    fd.append("listener", ctrl.form.listener);
    fd.append("cron", ctrl.form.cron);
    fd.append("interval", ctrl.form.interval);
    fd.append("message", ctrl.form.message);
    fd.append("description", ctrl.form.description);
    fd.append("locker", ctrl.form.locker);
    fd.append("lockkey", ctrl.form.lockkey);

    Api.Post("schedules", fd, function (data) {
      ctrl.addLocalAlert("success", "Successfully added schedule!");
      ctrl.selectedConfiguration = "";
      ctrl.form = {
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
    }, function (errorData, status, errorMsg) {
      var error = (errorData) ? errorData.error : errorMsg;
      ctrl.addLocalAlert("warning", error);
    }, false);
  };

  ctrl.addLocalAlert = function (type, message) {
    ctrl.state.push({ type: type, message: message });
  };
};

appModule.component('schedulerAdd', {
  controller: ['$scope', 'Api', '$rootScope', 'appService', SchedulerAddController],
  templateUrl: 'js/app/views/scheduler/scheduler-add-edit.component.html'
});
