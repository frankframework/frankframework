import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
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

export type Resource = {
  name: string;
  info: string;
  connectionPoolProperties: string;
};
export type ResourceFactories = {
  name: string;
  resources: Resource[];
};

export type supportedConnectionOptions = {
  protocols: string[];
  cyphers: string[];
};

export type ExpiringCertificate = {
  name: string;
  certificates: string[];
};

export interface SecurityItems {
  securityRoles: SecurityRole[];
  resourceFactories: ResourceFactories[];
  authEntries: AuthEntry[];
  jmsRealms: Record<string, string>;
  sapSystems: SapSystem[];
  xmlComponents: Record<string, string>;
  supportedConnectionOptions: supportedConnectionOptions;
  expiringCertificates: ExpiringCertificate[];
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

export type LinkName =
  | 'changeMessagesProcessState'
  | 'getMonitors'
  | 'createLogDefinition'
  | 'getAdapterStatistics'
  | 'getServerConfiguration'
  | 'updateSchedule'
  | 'trigger'
  | 'getWebServices'
  | 'getLogDirectory'
  | 'getIbisStoreSummary'
  | 'getAdapters'
  | 'getFileContent'
  | 'addMonitor'
  | 'createSchedule'
  | 'getEnvironmentVariables'
  | 'testPipeLine'
  | 'getLogDefinitions'
  | 'getLogConfiguration'
  | 'getFrankHealth'
  | 'getTriggers'
  | 'uploadConfiguration'
  | 'manageConfiguration'
  | 'getConfigurationByName'
  | 'getAllConfigurations'
  | 'getClusterMembers'
  | 'setClusterMemberTarget'
  | 'addTrigger'
  | 'getQueueConnectionFactories'
  | 'getJdbcDataSources'
  | 'resendReceiverMessages'
  | 'createScheduleInJobGroup'
  | 'browseMessage'
  | 'getTrigger'
  | 'deleteMonitor'
  | 'getAdapterFlow'
  | 'getAdapterHealth'
  | 'getSchedules'
  | 'getConfigurationDetailsByName'
  | 'getAdapter'
  | 'updateLogDefinition'
  | 'postServiceListener'
  | 'browseMessages'
  | 'getConfigurationXML'
  | 'executeJdbcQuery'
  | 'generateSQL'
  | 'getConfigurationFlow'
  | 'updateMonitor'
  | 'getServerInformation'
  | 'getConnections'
  | 'getWsdl'
  | 'updateAdapters'
  | 'downloadActiveConfigurations'
  | 'deleteTrigger'
  | 'getSchedule'
  | 'resendReceiverMessage'
  | 'updateScheduler'
  | 'getServiceListeners'
  | 'updateLogConfiguration'
  | 'deleteReceiverMessage'
  | 'downloadMessage'
  | 'updateAdapter'
  | 'getClassInfo'
  | 'downloadScript'
  | 'downloadMessages'
  | 'putJmsMessage'
  | 'getSecurityItems'
  | 'updateConfiguration'
  | 'deleteReceiverMessages'
  | 'getConfigurationHealth'
  | 'updateTrigger'
  | 'updateReceiver'
  | 'browseJdbcTable'
  | 'getMessageBrowsers'
  | 'browseQueue'
  | 'deleteSchedules'
  | 'getOpenApiSpec'
  | 'deleteConfiguration'
  | 'getMonitor'
  | 'fullAction'
  | 'downloadConfiguration';

export type Link = {
  name: LinkName;
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
  private securityItemsCache: SecurityItems | null = null;
  private endpointsWithRolesCache: Links | null = null;
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  clearCache(): void {
    this.securityItemsCache = null;
    this.endpointsWithRolesCache = null;
  }

  getSecurityItems(): Observable<SecurityItems> {
    if (this.securityItemsCache) {
      return of(this.securityItemsCache);
    }

    return this.http
      .get<SecurityItems>(`${this.appService.absoluteApiPath}securityitems`)
      .pipe(tap((data) => (this.securityItemsCache = data)));
  }

  getEndpointsWithRoles(): Observable<Links> {
    if (this.endpointsWithRolesCache) {
      return of(this.endpointsWithRolesCache);
    }

    return this.http
      .get<Links>(`${this.appService.absoluteApiPath}?allowedRoles=true`)
      .pipe(tap((data) => (this.endpointsWithRolesCache = data)));
  }
}
