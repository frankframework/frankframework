import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LogoutComponent } from './components/logout/logout.component';
import { StatusComponent } from './views/status/status.component';
import { AdapterstatisticsComponent } from './views/adapterstatistics/adapterstatistics.component';
import { StorageComponent } from './views/storage/storage.component';
import { StorageListComponent } from './views/storage/storage-list/storage-list.component';
import { StorageViewComponent } from './views/storage/storage-view/storage-view.component';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    title: 'Login'
  },
  {
    path: 'logout',
    component: LogoutComponent,
    title: 'Logout'
  },

  // maybe put all pages in a pages base route again, but maybe solve login in an auth guard instead
  // auth guard blocks all routes & prevents unauthorised access, is this good enough and what to do with this in app component

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
    component: ConfigurationsComponent,
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
  {
    path: "/test-pipeline",
    component: TestPipelineComponent,
    title: 'Test a PipeLine',
    data: {
      breadcrumbs: 'Testing > Test a PipeLine'
    }
  }
];

/*
      .state('pages.test_servicelistener', {
        url: "/test-serviceListener",
        component: "testServiceListener",
        data: {
          pageTitle: 'Test a ServiceListener',
          breadcrumbs: 'Testing > Test a ServiceListener'
        }
      })
      .state('pages.webservices', {
        url: "/webservices",
        component: "webservices",
        data: {
          pageTitle: 'Webservices',
          breadcrumbs: 'Webservices'
        }
      })
      .state('pages.scheduler', {
        url: "/scheduler",
        component: "scheduler",
        data: {
          pageTitle: 'Scheduler',
          breadcrumbs: 'Scheduler'
        }
      })
      .state('pages.add_schedule', {
        url: "/scheduler/new",
        component: "schedulerAdd",
        data: {
          pageTitle: 'Add Schedule',
          breadcrumbs: 'Scheduler > Add Schedule'
        },
      })
      .state('pages.edit_schedule', {
        url: "/scheduler/edit/:group/:name",
        component: "schedulerEdit",
        data: {
          pageTitle: 'Edit Schedule',
          breadcrumbs: 'Scheduler > Edit Schedule'
        },
        params: {
          name: "",
          group: ""
        }
      })
      .state('pages.environment_variables', {
        url: "/environment-variables",
        component: "environmentVariables",
        data: {
          pageTitle: 'Environment Variables',
          breadcrumbs: 'Environment Variables'
        }
      })
      .state('pages.execute_query', {
        url: "/jdbc/execute-query",
        component: 'jdbcExecuteQuery',
        data: {
          pageTitle: 'Execute JDBC Query',
          breadcrumbs: 'JDBC > Execute Query'
        }
      })
      .state('pages.browse_tables', {
        url: "/jdbc/browse-tables",
        component: "jdbcBrowseTables",
        data: {
          pageTitle: 'Browse JDBC Tables',
          breadcrumbs: 'JDBC > Browse Tables'
        }
      })
      .state('pages.security_items', {
        url: "/security-items",
        component: "securityItems",
        data: {
          pageTitle: 'Security Items',
          breadcrumbs: 'Security Items'
        }
      })
      .state('pages.connection_overview', {
        url: "/connections",
        component: "connections",
        data: {
          pageTitle: 'Connection Overview',
          breadcrumbs: 'Connection Overview'
        }
      })
      .state('pages.inlinestore_overview', {
        url: "/inlinestores/overview",
        component: "inlineStore",
        data: {
          pageTitle: 'InlineStore Overview',
          breadcrumbs: 'InlineStore Overview'
        }
      })
      .state('pages.monitors', {
        url: "/monitors?configuration",
        component: 'monitors',
        data: {
          pageTitle: 'Monitors',
          breadcrumbs: 'Monitors'
        },
        params: {
          configuration: { value: null, squash: true },
        },
      })
      .state('pages.monitors_editTrigger', {
        url: "/monitors/:monitor/triggers/:trigger?configuration",
        component: 'monitorsAddEdit',
        data: {
          pageTitle: 'Edit Trigger',
          breadcrumbs: 'Monitors > Triggers > Edit'
        },
        params: {
          configuration: { value: null, squash: true },
          monitor: "",
          trigger: "",
        },
      })
      .state('pages.monitors_addTrigger', {
        url: "/monitors/:monitor/triggers/new?configuration",
        component: 'monitorsAddEdit',
        data: {
          pageTitle: 'Add Trigger',
          breadcrumbs: 'Monitors > Triggers > Add'
        },
        params: {
          configuration: { value: null, squash: true },
          monitor: "",
        },
      })
      .state('pages.ibisstore_summary', {
        url: "/ibisstore-summary",
        component: "ibisStoreSummary",
        data: {
          pageTitle: 'Ibisstore Summary',
          breadcrumbs: 'JDBC > Ibisstore Summary'
        }
      })
      .state('pages.liquibase', {
        url: "/liquibase",
        component: "liquibase",
        data: {
          pageTitle: 'Liquibase Script',
          breadcrumbs: 'JDBC > Liquibase Script'
        }
      })
      .state('pages.customView', {
        url: "/customView/:name",
        component: "iframeCustomView",
        data: {
          pageTitle: "Custom View",
          breadcrumbs: 'Custom View',
          iframe: true
        },
        params: {
          name: { value: '', squash: true },
          url: { value: '', squash: true },
        },
      })
      .state('pages.larva', {
        url: "/testing/larva",
        component: "iframeLarva",
        data: {
          pageTitle: 'Larva',
          breadcrumbs: 'Testing > Larva',
          iframe: true
        },
      })
      .state('pages.ladybug', {
        url: "/testing/ladybug",
        component: "iframeLadybug",
        data: {
          pageTitle: 'Ladybug',
          breadcrumbs: 'Testing > Ladybug',
          iframe: true
        },
      })
      .state('pages.ladybug_beta', {
        url: "/testing/ladybug-beta",
        component: "iframeLadybugBeta",
        data: {
          pageTitle: 'Ladybug (beta)',
          breadcrumbs: 'Testing > Ladybug (beta)',
          iframe: true
        },
      })
      .state('pages.empty_page', {
        url: "/empty_page",
        templateUrl: "js/app/views/empty/empty_page.html",
        data: { pageTitle: 'Empty Page' }
      })
      .state('pages.iaf_update', {
        url: "/iaf-update",
        component: "iafUpdateStatus",
        data: { pageTitle: 'IAF Update' },
      })
      .state('pages.loading', {
        url: "/",
        component: "loading",
      })
      .state('pages.errorpage', {
        url: "/error",
        component: "error",
      });

*/

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    useHash: true,
    enableTracing: true
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
