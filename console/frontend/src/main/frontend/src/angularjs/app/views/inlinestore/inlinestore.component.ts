import { appModule } from "../../app.module";
import { ApiService } from "../../services/api.service";

class InlineStoreController {
	result: any;
	getProcessStateIcon?: "fa-server" | "fa-gears" | "fa-sign-in" | "fa-pause-circle" | "fa-times-circle";
	getProcessStateIconColor?: "success" | "warning" | "danger";

	constructor(
		private Api: ApiService,
		private appService: any
	) { };

	$onInit() {
		this.Api.Get("inlinestores/overview", (data) => {
			this.result = data;
		});

		this.getProcessStateIcon = this.appService.getProcessStateIcon;
		this.getProcessStateIconColor = this.appService.getProcessStateIconColor;
	};
};

appModule.component('inlineStore', {
	controller: ['Api', InlineStoreController],
	templateUrl: 'angularjs/app/views/inlinestore/inlinestore.component.html'
});
