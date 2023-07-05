import { appModule } from "../../../app.module";

//** Ctrls **//
const ConfigurationsManagaController = function ($rootScope, Api) {
	const ctrl = this;

	ctrl.$onInit = function () {
		$rootScope.$watch('configurations', function () { ctrl.configurations = $rootScope.configurations; });
		Api.Get("server/configurations", function (data) {
			$rootScope.updateConfigurations(data);
		});
	};
};

appModule.component('configurationsManage', {
	controller: ['$rootScope', 'Api', ConfigurationsManagaController],
	templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage.component.html",
});
