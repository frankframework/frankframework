import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService, Certificate } from 'src/app/app.service';

export type CertificateList = {
  adapter: string;
  pipe: string;
  certificate: Certificate;
};

export type SecurityRole = {
  allowed: boolean;
  name: string;
};

export type AuthEntry = {
  alias: string;
  username: string;
  password: string;
};

export type SapSystem = {
  name: string;
  info: string;
};

export type JmsRealm = {
  name: string;
  info: string | null;
  datasourceName?: string;
  connectionPoolProperties?: string;
  queueConnectionFactoryName?: string;
  topicConnectionFactoryName?: string;
};

export type Datasource = {
  datasourceName: string;
  info: string;
  connectionPoolProperties: string;
};

export type supportedConnectionOptions = {
  protocols: string[];
  cyphers: string[];
};

interface SecurityItems {
  securityRoles: Record<string, SecurityRole>;
  datasources: Datasource[];
  authEntries: AuthEntry[];
  jmsRealms: JmsRealm[];
  sapSystems: SapSystem[];
  xmlComponents: Record<string, string>;
  supportedConnectionOptions: supportedConnectionOptions;
}

export type HttpRequestMethodType =
  | 'GET'
  | 'HEAD'
  | 'POST'
  | 'PUT'
  | 'DELETE'
  | 'CONNECT'
  | 'OPTIONS'
  | 'TRACE'
  | 'PATCH';

export type Link = {
  name: string;
  rel: string;
  description: string;
  href: string;
  type: HttpRequestMethodType;
  roles: string[];
};

export type Links = { links: Link[] };

@Injectable({
  providedIn: 'root',
})
export class SecurityItemsService {
  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  getSecurityItems(): Observable<SecurityItems> {
    return this.http.get<SecurityItems>(`${this.appService.absoluteApiPath}securityitems`);
  }

  getEndpointsWithRoles(): Observable<Links> {
    return this.http.get<Links>(`${this.appService.absoluteApiPath}?allowedRoles=true`);
  }
}
