import { Component, OnInit } from '@angular/core';
import { AppService, Certificate, Pipe } from 'src/app/app.service';
import { AuthEntry, CertificateList, Datasource, JmsRealm, SapSystem, SecurityItemsService, SecurityRole, ServerProps } from './security-items.service';

@Component({
  selector: 'app-security-items',
  templateUrl: './security-items.component.html',
  styleUrls: ['./security-items.component.scss']
})
export class SecurityItemsComponent implements OnInit {
  sapSystems: SapSystem[] = [];
  serverProps: ServerProps = { maximumTransactionTimeout: "", totalTransactionLifetimeTimeout: "" };
  authEntries: AuthEntry[] = [];
  jmsRealms: JmsRealm[] = [];
  securityRoles: SecurityRole[] = [];
  certificates: CertificateList[] = [];
  xmlComponents: Record<string, string> = { };
  datasources: Datasource[] = [];

  constructor(
    private appService: AppService,
    private securityItemsService: SecurityItemsService
  ) { };

  ngOnInit(): void {
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

    this.securityItemsService.getSecurityItems().subscribe((data) => {
      this.authEntries = data.authEntries;
      this.datasources = data.datasources;
      this.jmsRealms = data.jmsRealms;
      this.sapSystems = data.sapSystems;
      this.securityRoles = data.securityRoles;
      this.serverProps = data.serverProps;
      this.xmlComponents = data.xmlComponents;
    });
  };
}
