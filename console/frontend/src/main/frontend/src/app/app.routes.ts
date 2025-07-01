import { Routes } from '@angular/router';
import { WebsocketTestComponent } from './views/websocket-test/websocket-test.component';
import { LoginComponent } from './views/login/login.component';
import { LogoutComponent } from './components/logout/logout.component';
import { StatusComponent } from './views/status/status.component';
import { authGuard } from './guards/auth.guard';
import { AdapterstatisticsComponent } from './views/adapterstatistics/adapterstatistics.component';
import { StorageComponent } from './views/storage/storage.component';
import { StorageListComponent } from './views/storage/storage-list/storage-list.component';
import { StorageViewComponent } from './views/storage/storage-view/storage-view.component';
import { ConfigurationsShowComponent } from './views/configurations/configurations-show/configurations-show.component';
import { ConfigurationsUploadComponent } from './views/configurations/configurations-upload/configurations-upload.component';
import { ConfigurationsManageComponent } from './views/configurations/configurations-manage/configurations-manage.component';
import { ConfigurationsManageDetailsComponent } from './views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component';
import { LoggingManageComponent } from './views/logging/logging-manage/logging-manage.component';
import { LoggingAddComponent } from './views/logging/logging-add/logging-add.component';
import { LoggingComponent } from './views/logging/logging.component';
import { JmsSendMessageComponent } from './views/jms/jms-send-message/jms-send-message.component';
import { JmsBrowseQueueComponent } from './views/jms/jms-browse-queue/jms-browse-queue.component';
import { TestPipelineComponent } from './views/test-pipeline/test-pipeline.component';
import { TestServiceListenerComponent } from './views/test-service-listener/test-service-listener.component';
import { WebservicesComponent } from './views/webservices/webservices.component';
import { SchedulerComponent } from './views/scheduler/scheduler.component';
import { SchedulerAddComponent } from './views/scheduler/scheduler-add/scheduler-add.component';
import { SchedulerEditComponent } from './views/scheduler/scheduler-edit/scheduler-edit.component';
import { EnvironmentVariablesComponent } from './views/environment-variables/environment-variables.component';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { SecurityItemsComponent } from './views/security-items/security-items.component';
import { ConnectionsComponent } from './views/connections/connections.component';
import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { MonitorsComponent } from './views/monitors/monitors.component';
import { MonitorsAddEditComponent } from './views/monitors/monitors-add-edit/monitors-add-edit.component';
import { MonitorsNewComponent } from './views/monitors/monitors-new/monitors-new.component';
import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';
import { LiquibaseComponent } from './views/liquibase/liquibase.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
import { IframeLadybugLegacyComponent } from './views/iframe/iframe-ladybug-legacy/iframe-ladybug-legacy.component';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IafUpdateComponent } from './views/iaf-update/iaf-update.component';
import { LoadingComponent } from './views/loading/loading.component';
import { ErrorComponent } from './views/error/error.component';
import { conditionalOnPropertyGuard } from './guards/conditional-on-property.guard';

export const routes: Routes = [
  {
    path: 'websocket-test',
    component: WebsocketTestComponent,
    title: 'Websocket Test',
    data: {
      breadcrumbs: 'Websocket Test',
    },
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Login',
    data: {
      breadcrumbs: 'Login',
    },
  },
  {
    path: 'logout',
    component: LogoutComponent,
    title: 'Logout',
    data: {
      linkName: 'logout',
      breadcrumbs: 'Logout',
    },
  },
  {
    path: 'status',
    component: StatusComponent,
    title: 'Adapter Status',
    canActivate: [authGuard],
    data: {
      linkName: 'getAdapters',
      breadcrumbs: 'Adapter > Status',
    },
  },
  {
    path: ':configuration/adapter/:name/statistics',
    component: AdapterstatisticsComponent,
    title: 'Adapter Statistics',
    canActivate: [authGuard],
    data: {
      linkName: 'getAdapterStatistics',
      breadcrumbs: 'Adapter > Statistics',
    },
  },
  {
    path: ':configuration/adapters/:adapter/:storageSource/:storageSourceName',
    component: StorageComponent,
    title: '',
    canActivate: [authGuard],
    data: {
      breadcrumbIsCustom: true,
      linkName: 'browseMessages',
    },
    children: [
      {
        path: 'stores/:processState',
        component: StorageListComponent,
        canActivate: [authGuard],
        data: {
          linkName: 'browseMessage',
        },
      },
      {
        path: 'stores/:processState/messages/:messageId',
        component: StorageViewComponent,
        canActivate: [authGuard],
        data: {
          linkName: 'downloadMessage',
        },
      },
    ],
  },
  {
    path: 'configurations',
    component: ConfigurationsShowComponent,
    title: 'Configurations',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Configurations > Show',
      linkName: 'getConfigurationXML',
    },
  },
  {
    path: 'configurations/upload',
    component: ConfigurationsUploadComponent,
    title: 'Manage Configurations',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Configurations > Upload',
      linkName: 'uploadConfiguration',
    },
  },
  {
    path: 'configurations/manage',
    component: ConfigurationsManageComponent,
    title: 'Manage Configurations',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Configurations > Manage',
      linkName: 'getAllConfigurations',
    },
  },
  {
    path: 'configurations/manage/:name',
    component: ConfigurationsManageDetailsComponent,
    title: 'Manage Configurations',
    canActivate: [authGuard],
    data: {
      breadcrumbIsCustom: true,
      linkName: 'manageConfiguration',
    },
  },
  {
    path: 'logging/settings',
    component: LoggingManageComponent,
    title: 'Logging',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Logging > Log Settings',
      linkName: ['getLogConfiguration', 'getLogDefinitions'],
    },
  },
  {
    path: 'logging/settings/add',
    component: LoggingAddComponent,
    title: 'Logging',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Logging > Log Settings > Add Logger',
      linkName: 'createLogDefinition',
    },
  },
  {
    path: 'logging',
    component: LoggingComponent,
    title: 'Logging',
    canActivate: [authGuard],
    data: {
      breadcrumbIsCustom: true,
      linkName: 'getLogDirectory',
    },
  },
  {
    path: 'logging/:directory',
    component: LoggingComponent,
    title: 'Logging',
    canActivate: [authGuard],
    data: {
      breadcrumbIsCustom: true,
      linkName: 'getLogDirectory',
    },
  },
  {
    path: 'logging/:directory/:file',
    component: LoggingComponent,
    title: 'Logging',
    canActivate: [authGuard],
    data: {
      breadcrumbIsCustom: true,
      linkName: 'getFileContent',
    },
  },
  {
    path: 'jms/send-message',
    component: JmsSendMessageComponent,
    title: 'Send JMS Message',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JMS > Send Message',
      linkName: 'putJmsMessage',
    },
  },
  {
    path: 'jms/browse-queue',
    component: JmsBrowseQueueComponent,
    title: 'Browse JMS Queue',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JMS > Browse Queue',
      linkName: 'browseQueue',
    },
  },
  {
    path: 'test-pipeline',
    component: TestPipelineComponent,
    title: 'Test a PipeLine',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Testing > Test a PipeLine',
      linkName: 'testPipeLine',
    },
  },
  {
    path: 'test-service-listener',
    component: TestServiceListenerComponent,
    title: 'Test a ServiceListener',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Testing > Test a ServiceListener',
      linkName: 'postServiceListener',
    },
  },
  {
    path: 'webservices',
    component: WebservicesComponent,
    title: 'Webservices',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Webservices',
      linkName: 'getWebServices',
    },
  },
  {
    path: 'scheduler',
    component: SchedulerComponent,
    title: 'Scheduler',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Scheduler',
      linkName: 'getSchedule',
    },
  },
  {
    path: 'scheduler/new',
    component: SchedulerAddComponent,
    title: 'Add Schedule',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Scheduler > Add Schedule',
      linkName: 'createSchedule',
    },
  },
  {
    path: 'scheduler/edit/:group/:name',
    component: SchedulerEditComponent,
    title: 'Edit Schedule',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Scheduler > Edit Schedule',
      linkName: 'updateScheduler',
    },
  },
  {
    path: 'environment-variables',
    component: EnvironmentVariablesComponent,
    title: 'Environment Variables',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Environment Variables',
      linkName: 'getEnvironmentVariables',
    },
  },
  {
    path: 'jdbc/execute-query',
    component: JdbcExecuteQueryComponent,
    title: 'Execute JDBC Query',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JDBC > Execute Query',
      linkName: 'executeJdbcQuery',
    },
  },
  {
    path: 'jdbc/browse-tables',
    component: JdbcBrowseTablesComponent,
    title: 'Browse JDBC Tables',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JDBC > Browse Tables',
      linkName: 'browseJdbcTable',
    },
  },
  {
    path: 'security-items',
    component: SecurityItemsComponent,
    title: 'Security Items',
    canActivate: [authGuard],
    data: {
      pageTitle: 'Security Items',
      breadcrumbs: 'Security Items',
      linkName: 'getSecurityItems',
    },
  },
  {
    path: 'connections',
    component: ConnectionsComponent,
    title: 'Connection Overview',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Connection Overview',
      linkName: 'getConnections',
    },
  },
  {
    path: 'inlinestores/overview',
    component: InlinestoreComponent,
    title: 'InlineStore Overview',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'InlineStore Overview',
      linkName: 'getMessageBrowsers',
    },
  },
  {
    path: 'monitors',
    component: MonitorsComponent,
    title: 'Monitors',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'Monitors',
      linkName: 'getMonitors',
    },
  },
  {
    path: 'monitors/:monitor/triggers/new',
    component: MonitorsAddEditComponent,
    title: 'Add Trigger',
    canActivate: [authGuard],
    data: {
      linkName: 'addTrigger',
      breadcrumbs: 'Monitors > Triggers > Add',
    },
  },
  {
    path: 'monitors/:monitor/triggers/:trigger',
    component: MonitorsAddEditComponent,
    title: 'Edit Trigger',
    canActivate: [authGuard],
    data: {
      linkName: 'updateTrigger',
      breadcrumbs: 'Monitors > Triggers > Edit',
    },
  },
  {
    path: 'monitors/new',
    component: MonitorsNewComponent,
    title: 'New Monitor',
    canActivate: [authGuard],
    data: {
      linkName: 'addMonitor',
      breadcrumbs: 'Monitors > New',
    },
  },
  {
    path: 'ibisstore-summary',
    component: IbisstoreSummaryComponent,
    title: 'Ibisstore Summary',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JDBC > Ibisstore Summary',
      linkName: 'getIbisStoreSummary',
    },
  },
  {
    path: 'liquibase',
    component: LiquibaseComponent,
    title: 'Liquibase Script',
    canActivate: [authGuard],
    data: {
      breadcrumbs: 'JDBC > Liquibase Script',
      linkName: 'generateSQL',
    },
  },
  {
    path: 'customView/:name',
    component: IframeCustomViewComponent,
    title: 'Custom View',
    data: {
      breadcrumbs: 'Custom View',
      iframe: true,
    },
  },
  {
    path: 'testing/larva',
    component: IframeLarvaComponent,
    title: 'Larva',
    canActivate: [conditionalOnPropertyGuard],
    data: {
      onProperty: 'servlet.LarvaServlet.enabled',
      breadcrumbs: 'Testing > Larva',
      iframe: true,
    },
  },
  {
    path: 'testing/ladybug-legacy',
    component: IframeLadybugLegacyComponent,
    title: 'Ladybug (legacy)',
    data: {
      breadcrumbs: 'Testing > Ladybug (legacy)',
      iframe: true,
    },
  },
  {
    path: 'testing/ladybug',
    component: IframeLadybugComponent,
    title: 'Ladybug',
    data: {
      breadcrumbs: 'Testing > Ladybug',
      iframe: true,
    },
  },
  {
    path: 'iaf-update',
    component: IafUpdateComponent,
    title: 'FF! update',
    data: {
      breadcrumbs: 'FF! update',
      linkName: 'fullReload',
    },
  },
  {
    path: '',
    pathMatch: 'full',
    component: LoadingComponent,
    data: {
      breadcrumbs: 'Loading',
    },
  },
  {
    path: 'error',
    component: ErrorComponent,
    title: 'Error',
    data: {
      breadcrumbs: 'Error',
    },
  },
  {
    path: '**',
    redirectTo: '',
    data: {
      breadcrumbs: 'Redirecting',
    },
  },
];
