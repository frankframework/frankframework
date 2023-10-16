import { appModule } from "../../app.module";
import { AppService, Certificate, Pipe } from "../../app.service";
import { ApiService } from "../../services/api.service";

interface CertificateList {
  adapter: string,
  pipe: string,
  certificate: Certificate
}

class SecurityItemsController {
  sapSystems = [];
  serverProps: any;
  authEntries = [];
  jmsRealms = [];
  securityRoles = [];
  certificates: CertificateList[] = [];

  constructor(
    private Api: ApiService,
    private appService: AppService
  ) { };


  $onInit() {
    for (const a in this.appService.adapters) {
      var adapter = this.appService.adapters[a];

      if (adapter.pipes) {
        for (const p in adapter.pipes) {
          var pipe: Pipe = adapter.pipes[p];

          if (pipe.certificate) {
            this.certificates.push({
              adapter: a,
              pipe: p,
              certificate: pipe.certificate
            });
          };
        };
      };
    };

    this.Api.Get("securityitems", (data) => {
      Object.assign(this, data)
    });
  };
};

appModule.component('securityItems', {
  controller: ['Api', 'appService', SecurityItemsController],
  templateUrl: 'js/app/views/security-items/security-items.component.html'
});
