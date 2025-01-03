import { Component, inject, OnInit } from '@angular/core';
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
  standalone: false,
})
export class SecurityItemsComponent implements OnInit {
  protected sapSystems: SapSystem[] = [];
  protected authEntries: AuthEntry[] = [];
  protected jmsRealms: JmsRealm[] = [];
  protected securityRoles: SecurityRole[] = [];
  protected certificates: CertificateList[] = [];
  protected xmlComponents: Record<string, string> = {};
  protected datasources: Datasource[] = [];
  protected supportedConnectionOptions: supportedConnectionOptions = {
    protocols: [],
    cyphers: [],
  };
  protected links: Link[] = [];

  private readonly appService: AppService = inject(AppService);
  private readonly securityItemsService: SecurityItemsService = inject(SecurityItemsService);

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
