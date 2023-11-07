import { appModule } from "../../app.module";

const InlineStoreController = function (Api, appService) {
    const ctrl = this;

	ctrl.$onInit = function () {
		Api.Get("inlinestores/overview", function (data) {
			ctrl.result = data;
		});

		ctrl.getProcessStateIcon = appService.getProcessStateIcon;
		ctrl.getProcessStateIconColor = appService.getProcessStateIconColor;
	};

};

appModule.component('inlineStore', {
    controller: ['Api', 'appService', InlineStoreController],
    templateUrl: 'js/app/views/inlinestore/inlinestore.component.html'
});
