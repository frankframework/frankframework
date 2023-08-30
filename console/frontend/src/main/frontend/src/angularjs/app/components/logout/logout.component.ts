import { appModule } from "../../app.module";
import { AuthService } from "../../services/authservice.service";
import { PollerService } from "../../services/poller.service";

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
