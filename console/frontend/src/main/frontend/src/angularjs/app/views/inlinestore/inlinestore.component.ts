import { appModule } from "../../app.module";
import { AppService } from "../../app.service";
import { ApiService } from "../../services/api.service";

class InlineStoreController {
	result: any;
	getProcessStateIcon?: (processState: string) => "fa-server" | "fa-gears" | "fa-sign-in" | "fa-pause-circle" | "fa-times-circle";
	getProcessStateIconColor?: (processState: string) => "success" | "warning" | "danger";

	constructor(
		private Api: ApiService,
		private appService: AppService
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
