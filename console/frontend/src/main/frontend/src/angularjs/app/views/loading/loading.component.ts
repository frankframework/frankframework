import { appModule } from "../../app.module";
import { StateService } from "@uirouter/angularjs";
import { ApiService } from "../../services/api.service";

class LoadingController {
  constructor(
    private Api: ApiService,
    private $state: StateService
  ) { }

  $onInit() {
    this.Api.Get("server/health", () => {
      this.$state.go("pages.status");
    }, (data, statusCode) => {
      if (statusCode == 401) return;

      if (data.status == "SERVICE_UNAVAILABLE") {
        this.$state.go("pages.status");
      } else {
        this.$state.go("pages.errorpage");
      }
    });
  };
};

appModule.component('loading', {
  controller: ['Api', '$state', LoadingController],
  templateUrl: 'angularjs/app/views/loading/loading.component.html'
});
