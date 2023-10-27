import { appModule } from "../../app.module";
import { ApiService } from "../../services/api.service";
import { MiscService } from "../../services/misc.service";

class WebservicesController {
  rootURL: string = "";

  constructor(
    private Api: ApiService,
    private Misc: MiscService
  ) { };

  $onInit() {
    this.rootURL = this.Misc.getServerPath();

    this.Api.Get("webservices", (data) => {
      Object.assign(this, data);
    });
  };

  compileURL(apiListener: any) {
    return this.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
  };
};

appModule.component('webservices', {
  controller: ['Api', 'Misc', WebservicesController],
  templateUrl: 'js/app/views/webservices/webservices.component.html'
});
