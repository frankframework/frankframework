import { ApiService } from "src/app/services.types";
import { appModule } from "../../app.module";
import { StateService } from "@uirouter/angularjs";

class LoadingController {
  constructor(
    private $scope: angular.IScope,
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
  controller: ['$scope', 'Api', '$state', LoadingController],
  templateUrl: 'angularjs/app/views/loading/loading.component.html'
});
