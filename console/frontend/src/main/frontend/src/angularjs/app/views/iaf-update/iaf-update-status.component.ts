import { appModule } from "../../app.module";
import { SessionService } from "../../services/session.service";

interface Release {
  name: string
  html_url: string
  created_at: string
}

class IafUpdateStatusController {
  release: Release = {
    name: "",
    html_url: "",
    created_at: ""
  };

  constructor(
    private $location: angular.ILocationService,
    private Session: SessionService,
  ) { };

  $onInit() {
    this.release = this.Session.get("IAF-Release");

    if (this.release == undefined)
      this.$location.path("status");
  };
}

appModule.component('iafUpdateStatus', {
  controller: ['$location', 'Session', IafUpdateStatusController],
  templateUrl: 'js/app/views/iaf-update/iaf-update-status.component.html'
});
