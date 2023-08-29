import { appModule } from "../../../app.module";


const SchedulerEditController = function ($scope, Api, $stateParams, $rootScope, appService) {
    const ctrl = this;

    ctrl.state = [];
	ctrl.editMode = true;
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
        var url = "schedules/" + $stateParams.group + "/jobs/" + $stateParams.name;

        ctrl.configurations = appService.configurations;
      appService.configurations$.subscribe(function () { ctrl.configurations = appService.configurations; });

        ctrl.adapters = appService.adapters;
      appService.adapters$.subscribe(function () { ctrl.adapters = appService.adapters; });

        Api.Get(url, function (data) {
            ctrl.selectedConfiguration = data.configuration;
            ctrl.form = {
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

	ctrl.submit = function (form) {
		var fd = new FormData();
		ctrl.state = [];

		fd.append("name", ctrl.form.name);
		fd.append("group", ctrl.form.group);
		fd.append("configuration", ctrl.selectedConfiguration);
		fd.append("adapter", ctrl.form.adapter);
		fd.append("listener", ctrl.form.listener);

		if (ctrl.form.cron)
			fd.append("cron", ctrl.form.cron);

		if (ctrl.form.interval)
			fd.append("interval", ctrl.form.interval);

		fd.append("message", ctrl.form.message);
		fd.append("description", ctrl.form.description);
		fd.append("locker", ctrl.form.locker);

		if (ctrl.form.lockkey)
			fd.append("lockkey", ctrl.form.lockkey);

		Api.Put(url, fd, function (data) {
			ctrl.addLocalAlert("success", "Successfully edited schedule!");
		}, function (errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			ctrl.addLocalAlert("warning", error);
		}, false);
	};

    ctrl.addLocalAlert = function (type, message) {
        ctrl.state.push({ type: type, message: message });
    };
};

appModule.component('schedulerEdit', {
    controller: ['$scope', 'Api', '$stateParams', '$rootScope', 'appService', SchedulerEditController],
    templateUrl: 'js/app/views/scheduler/scheduler-add-edit.component.html'
});
