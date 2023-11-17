import { appModule } from "src/angularjs/app/app.module";
import { ApiService } from "src/angularjs/app/services/api.service";
import { AppService } from "src/app/app.service";

class ConfigurationsManagaController {
  configurations = {};

  constructor(
    private Api: ApiService,
    private appService: AppService,
  ) { };

  $onInit() {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.Api.Get("server/configurations", (data) => {
      this.appService.updateConfigurations(data);
    });
  };
};

appModule.component('configurationsManage', {
  controller: ['Api', 'appService', ConfigurationsManagaController],
  templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage.component.html",
});
