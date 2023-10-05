import { appModule } from "../../../app.module";

//** Ctrls **//
const ConfigurationsManagaController = function ($rootScope, Api, appService) {
	const ctrl = this;

	ctrl.$onInit = function () {
		ctrl.configurations = appService.configurations;
		ctrl.unregister$on = $rootScope.$on('configurations', function () { ctrl.configurations = appService.configurations; });
		Api.Get("server/configurations", function (data) {
			appService.updateConfigurations(data);
		});
	};

	ctrl.$onDestroy = function(){
		ctrl.unregister$on();
	}
};

appModule.component('configurationsManage', {
	controller: ['$rootScope', 'Api', 'appService', ConfigurationsManagaController],
	templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage.component.html",
});
