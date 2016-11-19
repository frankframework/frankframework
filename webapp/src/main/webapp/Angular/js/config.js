function config($locationProvider, $stateProvider, $urlRouterProvider, $ocLazyLoadProvider, IdleProvider, KeepaliveProvider, appConstants) {

    if(appConstants.idleTime && appConstants.idleTime > 0) {
        IdleProvider.idle(appConstants.idleTime);
        IdleProvider.timeout(appConstants.idleTimeout);
    }

    $urlRouterProvider.otherwise("/status");

    $ocLazyLoadProvider.config({
        modules: [
        {
            name: 'toaster',
            files: ['js/plugins/toastr/toastr.min.js', 'css/plugins/toastr/toastr.min.css']
        }
        ],
        // Set to true if you want to see what and when is dynamically loaded
        debug: true
    });

    $stateProvider
    .state('login', {
        url: "/login",
        templateUrl: "views/login.html",
        controller: LoginCtrl,
        data: {
            pageTitle: 'Login',
            specialClass: 'gray-bg'
        }
    })
    .state('logout', {
        url: "/logout",
        controller: LogoutCtrl,
        data: {
            pageTitle: 'Logout',
            specialClass: 'gray-bg'
        }
    })

    .state('pages', {
        abstract: true,
        controller: function(authService) {
            authService.loggedin(); //Check if the user is logged in.
        },
        templateUrl: "views/common/content.html",
    })
    .state('pages.status', {
        url: "/status",
        templateUrl: "views/adapter_status.html",
        controller: 'StatusCtrl as status',
        data: {
            pageTitle: 'Adapter Status',
            breadcrumbs: 'Adapter Status',
        }
        //parent: "pages"
    })
    .state('pages.adapter', {
        url: "/adapter",
        templateUrl: "views/adapter_status.html",
    })
    .state('pages.adapter.filter', {
        url: "{uri:.*}",
        data: {
            pageTitle: 'Adapter Status',
            breadcrumbs: 'Adapter Status > Filter'
        },
    })
    .state('pages.adapter.get', {
        url: "{uri:.*}",
        controller: StatusCtrl,
    })
    .state('pages.notifications', {
        url: "/notifications",
        templateUrl: "views/notifications.html",
        data: {
            pageTitle: 'Notifications',
            breadcrumbs: 'Notifications'
        },
        params: {
            id: 0,
        },
        controller: NotificationsCtrl
    })
    .state('pages.configuration', {
        url: "/configuration",
        templateUrl: "views/show_configuration.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration',
        }
    })
    .state('pages.logging', {
        url: "/logging",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            //Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.send_message', {
        url: "/jms/send-message",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            //Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.browse_queue', {
        url: "/jms/browse-queue",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.test_pipeline', {
        url: "/test-pipeline",
        templateUrl: "views/test_pipeline.html",
        data: {
            pageTitle: 'Test a PipeLine',
            breadcrumbs: 'Test a PipeLine'
        }
    })
    .state('pages.test_serviceListner', {
        url: "/test-serviceListner",
        templateUrl: "views/test_serviceListner.html",
        data: {
            pageTitle: 'Test a Service',
            breadcrumbs: 'Test a Service'
        }
    })
    .state('pages.webservices', {
        url: "/webservices",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.scheduler', {
        url: "/scheduler",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.environment_variables', {
        url: "/environment-variables",
        templateUrl: "views/environment_variables.html",
        data: {
            pageTitle: 'Environment Variables',
            breadcrumbs: 'Environment Variables'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.execute_query', {
        url: "/jdbc/execute-query",
        templateUrl: "views/jdbc_execute_query.html",
        data: {
            pageTitle: 'Execute JDBC Query',
            breadcrumbs: 'JDBC > Execute Query'
        }
    })
    .state('pages.browse_tables', {
        url: "/jdbc/browse-tables",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Browse JDBC Tables',
            breadcrumbs: 'JDBC > Browse Tables'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.console', {
        url: "/ibis-console",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.security_items', {
        url: "/security-items",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.monitors', {
        url: "/monitors",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.ibisstore_summary', {
        url: "/ibisstore-summary",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.larva', {
        url: "/testing/larva",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.ladybug', {
        url: "/testing/ladybug",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.information', {
        url: "/information",
        templateUrl: "views/empty_page.html",
        data: {
            pageTitle: 'Show Configuration',
            breadcrumbs: 'Show Configuration'
        },
        controller: function(Notification){
            Notification.add('fa-exclamation-circle', "Test notification", "asfkasgf");
        }
    })
    .state('pages.empty_page', {
        url: "/empty_page",
        templateUrl: "views/empty_page.html",
        data: { pageTitle: 'Empty Page' }
    });
    
    $locationProvider.html5Mode(false);

}
angular
    .module('iaf.beheerconsole')
    .config(config)
    .run(function($rootScope, $state) {
        $rootScope.$state = $state;
    });
