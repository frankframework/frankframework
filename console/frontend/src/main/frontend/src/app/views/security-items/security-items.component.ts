import { Component, OnInit } from '@angular/core';
import { AppService, Pipe } from 'src/app/app.service';
import {
  AuthEntry,
  CertificateList,
  Datasource,
  JmsRealm,
  Link,
  SapSystem,
  SecurityItemsService,
  SecurityRole,
  supportedConnectionOptions,
} from './security-items.service';

@Component({
  selector: 'app-security-items',
  templateUrl: './security-items.component.html',
  styleUrls: ['./security-items.component.scss'],
})
export class SecurityItemsComponent implements OnInit {
  protected sapSystems: SapSystem[] = [];
  protected authEntries: AuthEntry[] = [];
  protected jmsRealms: JmsRealm[] = [];
  protected securityRoles: Record<string, SecurityRole> = {};
  protected certificates: CertificateList[] = [];
  protected xmlComponents: Record<string, string> = {};
  protected datasources: Datasource[] = [];
  protected supportedConnectionOptions: supportedConnectionOptions = {
    protocols: [],
    cyphers: [],
  };
  protected links: Link[] = [];

  constructor(
    private appService: AppService,
    private securityItemsService: SecurityItemsService,
  ) {}

  ngOnInit(): void {
    for (const adapter of Object.values(this.appService.adapters)) {
      if (adapter.pipes) {
        for (const p in adapter.pipes) {
          const pipe: Pipe = adapter.pipes[p];

          if (pipe.certificate) {
            this.certificates.push({
              adapter: adapter.name,
              pipe: p,
              certificate: pipe.certificate,
            });
          }
        }
      }
    }

    this.securityItemsService.getSecurityItems().subscribe((data) => {
      this.authEntries = data.authEntries;
      this.datasources = data.datasources;
      this.jmsRealms = data.jmsRealms;
      this.sapSystems = data.sapSystems;
      this.securityRoles = data.securityRoles;
      this.xmlComponents = data.xmlComponents;
      this.supportedConnectionOptions = data.supportedConnectionOptions;
    });

    this.securityItemsService.getEndpointsWithRoles().subscribe(({ links }) => {
      this.links = links;
    });
  }
}
