import { appModule } from "../../../app.module";

//** Ctrls **//
const ConfigurationsManagaController = function (Api, appService) {
	const ctrl = this;

	ctrl.$onInit = function () {
		ctrl.configurations = appService.configurations;
    appService.configurations$.subscribe(function () { ctrl.configurations = appService.configurations; });
		Api.Get("server/configurations", function (data) {
			appService.updateConfigurations(data);
		});
	};
};

appModule.component('configurationsManage', {
	controller: ['Api', 'appService', ConfigurationsManagaController],
	templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage.component.html",
});
