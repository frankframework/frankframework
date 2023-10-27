import { Component, OnInit } from '@angular/core';
import { AppService, Certificate, Pipe } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';

interface CertificateList {
  adapter: string
  pipe: string
  certificate: Certificate
}

interface SecurityRole {
  allowed: boolean
  'role-name': string
  specialSubjects: string
  groups: string
}

interface AuthEntry {
  alias: string
  username: string
  password: string
}

interface SapSystem {
  name: string
  info: string
}

interface JmsRealm {
  name: string
  datasourceName: string
  queueConnectionFactoryName: string
  topicConnectionFactoryName: string
  info: string
  connectionPoolProperties: string
}

interface ServerProps {
  maximumTransactionTimeout: string
  totalTransactionLifetimeTimeout: string
}

interface Datasource {
  datasourceName: string
  info: string
  connectionPoolProperties: string
}

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
    private apiService: ApiService,
    private appService: AppService
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

    this.apiService.Get("securityitems", (data) => {
      Object.assign(this, data)
    });
  };
}
