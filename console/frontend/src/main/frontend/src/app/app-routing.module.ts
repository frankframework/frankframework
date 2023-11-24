import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LogoutComponent } from './components/logout/logout.component';
import { StatusComponent } from './views/status/status.component';
import { AdapterstatisticsComponent } from './views/adapterstatistics/adapterstatistics.component';
import { StorageComponent } from './views/storage/storage.component';
import { StorageListComponent } from './views/storage/storage-list/storage-list.component';
import { StorageViewComponent } from './views/storage/storage-view/storage-view.component';
import { JmsBrowseQueueComponent } from './views/jms/jms-browse-queue/jms-browse-queue.component';
import { JmsSendMessageComponent } from './views/jms/jms-send-message/jms-send-message.component';
import { WebservicesComponent } from './views/webservices/webservices.component';
import { EnvironmentVariablesComponent } from './views/environment-variables/environment-variables.component';
import { SecurityItemsComponent } from './views/security-items/security-items.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';
import { LiquibaseComponent } from './views/liquibase/liquibase.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
import { IframeLadybugBetaComponent } from './views/iframe/iframe-ladybug-beta/iframe-ladybug-beta.component';
import { MonitorsComponent } from './views/monitors/monitors.component';
import { MonitorsAddEditComponent } from './views/monitors/monitors-add-edit/monitors-add-edit.component';
import { SchedulerComponent } from './views/scheduler/scheduler.component';
import { SchedulerAddComponent } from './views/scheduler/scheduler-add/scheduler-add.component';
import { SchedulerEditComponent } from './views/scheduler/scheduler-edit/scheduler-edit.component';
import { ConfigurationsShowComponent } from './views/configurations/configurations-show/configurations-show.component';
import { ConfigurationsManageComponent } from './views/configurations/configurations-manage/configurations-manage.component';
import { ConfigurationsManageDetailsComponent } from './views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component';
import { ConfigurationsUploadComponent } from './views/configurations/configurations-upload/configurations-upload.component';
import { ErrorComponent } from './views/error/error.component';
import { LoadingComponent } from './views/loading/loading.component';
import { NotificationsComponent } from './views/notifications/notifications.component';
import { LoggingComponent } from './views/logging/logging.component';
import { LoggingManageComponent } from './views/logging/logging-manage/logging-manage.component';
import { IafUpdateComponent } from './views/iaf-update/iaf-update.component';

const routes: Routes = [
  /* {
    path: 'login',
    component: LoginComponent,
    title: 'Login'
  }, */
  {
    path: 'logout',
    component: LogoutComponent,
    title: 'Logout'
  },
  {
    path: 'status',
    component: StatusComponent,
    title: 'Adapter Status',
    data: {
      breadcrumbs: 'Adapter > Status',
    },
  },
  {
    path: ":configuration/adapter/:name/statistics",
    component: AdapterstatisticsComponent,
    title: 'Adapter Statistics',
    data: {
      breadcrumbs: 'Adapter > Statistics'
    },
  },
  {
    path: ':configuration/adapters/:adapter/:storageSource/:storageSourceName',
    component: StorageComponent,
    title: '',
    data: {
      breadcrumbs: ''
    },
    children: [
      {
        path: 'stores/:processState',
        component: StorageListComponent,
      },
      {
        path: 'stores/:processState/messages/:messageId',
        component: StorageViewComponent,
      }
    ]
  },
  {
    path: 'notifications',
    component: NotificationsComponent,
    title: 'Notifications',
    data: {
      breadcrumbs: 'Notifications'
    },
  },
  {
    path: 'configurations',
    component: ConfigurationsShowComponent,
    title: 'Configurations',
    data: {
      breadcrumbs: 'Configurations > Show',
    },
  },
  {
    path: 'configurations/upload',
    component: ConfigurationsUploadComponent,
    title: 'Manage Configurations',
    data: {
      breadcrumbs: 'Configurations > Upload',
    }
  },
  {
    path: 'configurations/manage',
    component: ConfigurationsManageComponent,
    title: 'Manage Configurations',
    data: {
      breadcrumbs: 'Configurations > Manage',
    }
  },
  {
    path: 'configurations/manage/:name',
    component: ConfigurationsManageDetailsComponent,
    title: 'Manage Configurations',
    data: {
      breadcrumbs: 'Configurations > Manage',
    },
  },
  {
    path: 'logging',
    component: LoggingComponent,
    title: 'Logging',
    data: {
      breadcrumbs: 'Logging > Log Files'
    },
  },
  {
    path: 'logging/settings',
    component: LoggingManageComponent,
    title: 'Logging',
    data: {
      breadcrumbs: 'Logging > Log Settings'
    },
  },
  {
    path: 'jms/send-message',
    component: JmsSendMessageComponent,
    title: 'Send JMS Message',
    data: {
      breadcrumbs: 'JMS > Send Message'
    }
  },
  {
    path: 'jms/browse-queue',
    component: JmsBrowseQueueComponent,
    title: 'Browse JMS Queue',
    data: {
      breadcrumbs: 'JMS > Browse Queue'
    }
  },
  /* {
    path: "/test-pipeline",
    component: TestPipelineComponent,
    title: 'Test a PipeLine',
    data: {
      breadcrumbs: 'Testing > Test a PipeLine'
    }
  }, */
  /* {
    path: 'test-serviceListener',
    component: TestServiceListenerComponent,
    title: 'Test a ServiceListener',
    data: {
      breadcrumbs: 'Testing > Test a ServiceListener'
    }
  }, */
  {
    path: 'webservices',
    component: WebservicesComponent,
    title: 'Webservices',
    data: {
      breadcrumbs: 'Webservices'
    }
  },
  {
    path: 'scheduler',
    component: SchedulerComponent,
    title: 'Scheduler',
    data: {
      breadcrumbs: 'Scheduler'
    }
  },
  {
    path: 'scheduler/new',
    component: SchedulerAddComponent,
    title: 'Add Schedule',
    data: {
      breadcrumbs: 'Scheduler > Add Schedule'
    },
  },
  {
    path: 'scheduler/edit/:group/:name',
    component: SchedulerEditComponent,
    title: 'Edit Schedule',
    data: {
      breadcrumbs: 'Scheduler > Edit Schedule'
    },
  },
  {
    path: 'environment-variables',
    component: EnvironmentVariablesComponent,
    title: 'Environment Variables',
    data: {
      breadcrumbs: 'Environment Variables'
    }
  },
  {
    path: 'jdbc/execute-query',
    component: JdbcExecuteQueryComponent,
    title: 'Execute JDBC Query',
    data: {
      breadcrumbs: 'JDBC > Execute Query'
    }
  },
  {
    path: 'jdbc/browse-tables',
    component: JdbcBrowseTablesComponent,
    title: 'Browse JDBC Tables',
    data: {
      breadcrumbs: 'JDBC > Browse Tables'
    }
  },
  {
    path: 'security-items',
    component: SecurityItemsComponent,
    data: {
      pageTitle: 'Security Items',
      breadcrumbs: 'Security Items'
    }
  },
  /* {
    path: 'connections',
    component: ConnectionsComponent,
    title: 'Connection Overview',
    data: {
      breadcrumbs: 'Connection Overview'
    }
  }, */
  {
    path: 'inlinestores/overview',
    component: InlinestoreComponent,
    title: 'InlineStore Overview',
    data: {
      breadcrumbs: 'InlineStore Overview'
    }
  },
  {
    path: 'monitors',
    component: MonitorsComponent,
    title: 'Monitors',
    data: {
      breadcrumbs: 'Monitors'
    }
  },
  {
    path: 'monitors/:monitor/triggers/:trigger',
    component: MonitorsAddEditComponent,
    title: 'Edit Trigger',
    data: {
      breadcrumbs: 'Monitors > Triggers > Edit'
    },
  },
  {
    path: 'monitors/:monitor/triggers/new',
    component: MonitorsAddEditComponent,
    title: 'Add Trigger',
    data: {
      breadcrumbs: 'Monitors > Triggers > Add'
    },
  },
  {
    path: 'ibisstore-summary',
    component: IbisstoreSummaryComponent,
    title: 'Ibisstore Summary',
    data: {
      breadcrumbs: 'JDBC > Ibisstore Summary'
    }
  },
  {
    path: 'liquibase',
    component: LiquibaseComponent,
    title: 'Liquibase Script',
    data: {
      breadcrumbs: 'JDBC > Liquibase Script'
    }
  },
  {
    path: 'customView/:name',
    component: IframeCustomViewComponent,
    title: "Custom View",
    data: {
      breadcrumbs: 'Custom View',
      iframe: true
    },
  },
  {
    path: 'testing/larva',
    component: IframeLarvaComponent,
    title: 'Larva',
    data: {
      breadcrumbs: 'Testing > Larva',
      iframe: true
    },
  },
  {
    path: 'testing/ladybug',
    component: IframeLadybugComponent,
    title: 'Ladybug',
    data: {
      breadcrumbs: 'Testing > Ladybug',
      iframe: true
    },
  },
  {
    path: 'testing/ladybug-beta',
    component: IframeLadybugBetaComponent,
    title: 'Ladybug (beta)',
    data: {
      breadcrumbs: 'Testing > Ladybug (beta)',
      iframe: true
    },
  },
  /* {
    path: 'empty_page',
    templateUrl: "js/app/views/empty/empty_page.html",
    title: 'Empty Page'
  }, */
  {
    path: 'iaf-update',
    component: IafUpdateComponent,
    title: 'IAF Update',
    data: {
      breadcrumbs: 'IAF Update'
    },
  },
  {
    path: '',
    pathMatch: 'full',
    component: LoadingComponent,
  },
  {
    path: 'error',
    component: ErrorComponent,
    title: 'Error',
    data: {
      breadcrumbs: 'Error'
    },
  },
  {
    path: '**',
    redirectTo: 'status',
    data: {
      breadcrumbs: 'Loading'
    }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    useHash: true,
    enableTracing: true,
    paramsInheritanceStrategy: 'always'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
