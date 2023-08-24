import { AuthService, PollerService } from "src/app/services.types";
import { appModule } from "../../app.module";

class LogoutController {
  constructor(
    private Poller: PollerService,
    private authService: AuthService,
    private Idle: angular.idle.IIdleService
  ){}

  $onInit() {
    this.Poller.getAll().remove();
    this.Idle.unwatch();
    this.authService.logout();
  };
};

appModule.component('logout', {
    controller: ['Poller', 'authService', 'Idle', LogoutController],
});
