import { ApiService } from "src/app/services.types";
import { appModule } from "../../app.module";
import { StateService } from "angular-ui-router";

class ErrorController {
  constructor(
    private $scope: angular.IScope,
    private Api: ApiService,
    private $state: StateService,
    private $interval: angular.IIntervalService,
    private $rootScope: angular.IRootScopeService,
    private $timeout: angular.ITimeoutService,
    private appService
  ) { }

  cooldownCounter = 0;
  viewStackTrace = false;
  stackTrace: any;

  $onInit() {
    this.checkState();
  };

  cooldown(data) {
    this.cooldownCounter = 60;

    if (data.status == "error" || data.status == "INTERNAL_SERVER_ERROR") {
      this.appService.updateStartupError(data.error);
      this.stackTrace = data.stackTrace;

      var interval = this.$interval(() => {
        this.cooldownCounter--;
        if (this.cooldownCounter < 1) {
          this.$interval.cancel(interval);
          this.checkState();
        }
      }, 1000);
    } else if (data.status == "SERVICE_UNAVAILABLE") {
      this.$state.go("pages.status");
    }
  };

  checkState() {
    this.Api.Get("server/health", () => {
      this.$state.go("pages.status");
      this.$timeout(function () { window.location.reload(); }, 50);
    }, (data) => this.cooldown(data));
  };
};

appModule.component('error', {
  controller: ['$scope', 'Api', '$state', '$interval', '$rootScope', '$timeout', 'appService', ErrorController],
  templateUrl: 'angularjs/app/views/error/error.component.html'
});
