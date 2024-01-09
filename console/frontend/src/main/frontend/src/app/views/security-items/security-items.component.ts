import { Component, OnInit } from '@angular/core';
import { AppService, Pipe } from 'src/app/app.service';
import { AuthEntry, CertificateList, Datasource, JmsRealm, SapSystem, SecurityItemsService, SecurityRole } from './security-items.service';

@Component({
  selector: 'app-security-items',
  templateUrl: './security-items.component.html',
  styleUrls: ['./security-items.component.scss']
})
export class SecurityItemsComponent implements OnInit {
  sapSystems: SapSystem[] = [];
  authEntries: AuthEntry[] = [];
  jmsRealms: JmsRealm[] = [];
  securityRoles: Record<string, SecurityRole> = {};
  certificates: CertificateList[] = [];
  xmlComponents: Record<string, string> = { };
  datasources: Datasource[] = [];

  constructor(
    private appService: AppService,
    private securityItemsService: SecurityItemsService
  ) { };

  ngOnInit(): void {
    for (const adapter of Object.values(this.appService.adapters)) {

      if (adapter.pipes) {
        for (const p in adapter.pipes) {
          var pipe: Pipe = adapter.pipes[p];

          if (pipe.certificate) {
            this.certificates.push({
              adapter: adapter.name,
              pipe: p,
              certificate: pipe.certificate
            });
          };
        };
      };
    };

    this.securityItemsService.getSecurityItems().subscribe((data) => {
      this.authEntries = data.authEntries;
      this.datasources = data.datasources;
      this.jmsRealms = data.jmsRealms;
      this.sapSystems = data.sapSystems;
      this.securityRoles = data.securityRoles;
      this.xmlComponents = data.xmlComponents;
    });
  };
}
