function config($locationProvider, $stateProvider, $urlRouterProvider, $ocLazyLoadProvider, IdleProvider, KeepaliveProvider, appConstants) {

    if(appConstants["console.idle.time"] && appConstants["console.idle.time"] > 0) {
        IdleProvider.idle(appConstants["console.idle.time"]);
        IdleProvider.timeout(appConstants["console.idle.timeout"]);
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
        controller: function($scope, authService, Hooks) {
            authService.loggedin(); //Check if the user is logged in.
            $scope.monitoring = false;
            Hooks.register("appConstants:once", function(data) {
                $scope.monitoring = (data["monitoring.enabled"] === 'true');
            });
        },
        templateUrl: "views/common/content.html",
    })
    .state('pages.status', {
        url: "/status",
        templateUrl: "views/adapter_status.html",
        controller: 'StatusCtrl as status',
        data: {
            pageTitle: 'Adapter Status',
            breadcrumbs: 'Adapter > Status',
        }
        //parent: "pages"
    })
    .state('pages.adapter', {
        url: "/adapter",
        templateUrl: "views/adapter_status.html",
    })
    .state('pages.adapterstatistics', {
        url: "/adapter/:name/statistics",
        templateUrl: "views/adapter_statistics.html",
        data: {
            pageTitle: 'Adapter',
            breadcrumbs: 'Adapter > Statistics'
        },
        params: {
            id: 0,
        },
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
        templateUrl: "views/ShowConfiguration.html",
        data: {
            pageTitle: 'Configuration',
            breadcrumbs: 'Show Configuration',
        }
    })
    .state('pages.logging', {
        url: "/logging",
        templateUrl: "views/ShowLogging.html",
        data: {
            pageTitle: 'Logging',
            breadcrumbs: 'Show Logging'
        }
    })
    .state('pages.send_message', {
        url: "/jms/send-message",
        templateUrl: "views/SendJmsMessage.html",
        data: {
            pageTitle: 'Send JMS Message',
            breadcrumbs: 'JMS > Send Message'
        }
    })
    .state('pages.browse_queue', {
        url: "/jms/browse-queue",
        templateUrl: "views/BrowseJmsQueue.html",
        data: {
            pageTitle: 'Browse JMS Queue',
            breadcrumbs: 'JMS > Browse Queue'
        }
    })
    .state('pages.test_pipeline', {
        url: "/test-pipeline",
        templateUrl: "views/TestPipeline.html",
        data: {
            pageTitle: 'Test a PipeLine',
            breadcrumbs: 'Test > PipeLine'
        }
    })
    .state('pages.test_servicelistener', {
        url: "/test-serviceListener",
        templateUrl: "views/TestServiceListener.html",
        data: {
            pageTitle: 'Test a ServiceListener',
            breadcrumbs: 'Test > ServiceListener'
        }
    })
    .state('pages.webservices', {
        url: "/webservices",
        templateUrl: "views/Webservices.html",
        data: {
            pageTitle: 'Webservices',
            breadcrumbs: 'Webservices'
        }
    })
    .state('pages.scheduler', {
        url: "/scheduler",
        templateUrl: "views/ShowScheduler.html",
        data: {
            pageTitle: 'Scheduler',
            breadcrumbs: 'Scheduler'
        }
    })
    .state('pages.environment_variables', {
        url: "/environment-variables",
        templateUrl: "views/ShowEnvironmentVariables.html",
        data: {
            pageTitle: 'Environment Variables',
            breadcrumbs: 'Environment Variables'
        }
    })
    .state('pages.execute_query', {
        url: "/jdbc/execute-query",
        templateUrl: "views/ExecuteJdbcQuery.html",
        data: {
            pageTitle: 'Execute JDBC Query',
            breadcrumbs: 'JDBC > Execute Query'
        }
    })
    .state('pages.browse_tables', {
        url: "/jdbc/browse-tables",
        templateUrl: "views/BrowseJdbcTable.html",
        data: {
            pageTitle: 'Browse JDBC Tables',
            breadcrumbs: 'JDBC > Browse Tables'
        },
        resolve: {
            loadPlugin: function ($ocLazyLoad) {
                return $ocLazyLoad.load([ {
                    files: ['css/plugins/iCheck/custom.css','js/plugins/iCheck/icheck.min.js']
                } ]);
            }
        }
    })
    .state('pages.security_items', {
        url: "/security-items",
        templateUrl: "views/ShowSecurityItems.html",
        data: {
            pageTitle: 'Security Items',
            breadcrumbs: 'Security Items'
        }
    })
    .state('pages.monitors', {
        url: "/monitors",
        templateUrl: "views/ShowMonitors.html",
        data: {
            pageTitle: 'Monitors',
            breadcrumbs: 'Monitors'
        }
    })
    .state('pages.ibisstore_summary', {
        url: "/ibisstore-summary",
        templateUrl: "views/ShowIbisstoreSummary.html",
        data: {
            pageTitle: 'Ibisstore Summary',
            breadcrumbs: 'JDBC > Ibisstore Summary'
        }
    })
    .state('pages.larva', {
        url: "/testing/larva",
        templateUrl: "views/iFrame.html",
        data: {
            pageTitle: 'Larva',
            breadcrumbs: 'Test > Larva'
        },
        controller: function($scope, Misc, $interval){
            $scope.url = Misc.getServerPath() + "larva";
            var iframe = angular.element("iframe");
            var container = iframe.parent();
            container.css({"margin-left":"-15px", "margin-right":"-15px", "padding-bottom":"50px"});

            iframe[0].onload = function() {
                $interval(function(){
                    var height = iframe[0].contentWindow.document.body.clientHeight + 50;
                    iframe.css("height", height);
                }, 50);
            };
        }
    })
    .state('pages.ladybug', {
        url: "/testing/ladybug",
        templateUrl: "views/iFrame.html",
        data: {
            pageTitle: 'Ladybug',
            breadcrumbs: 'Test > Ladybug'
        },
        controller: function($scope, Misc, $timeout){
            $scope.url = Misc.getServerPath() + "testtool";
            var iframe = angular.element("iframe");
            var container = iframe.parent();
            container.css({"margin-left":"-15px", "margin-right":"-15px", "padding-bottom":"50px", "background-color":"#b4e2ff"});
            iframe.css({"height":"800px"});
            iframe[0].onload = function() {
                var iframeBody = $(iframe[0].contentWindow.document.body);
                $timeout(function() {
                    var c_13_content_c_14 = iframeBody.children("form").find("#c_13_content_c_14");
                    c_13_content_c_14.css("padding-right", "12px");
                }, 500);
            };
        }
    })
    .state('pages.empty_page', {
        url: "/empty_page",
        templateUrl: "views/empty_page.html",
        data: { pageTitle: 'Empty Page' }
    })
    .state('pages.iaf_update', {
        url: "/iaf-update",
        templateUrl: "views/iaf-update.html",
        data: { pageTitle: 'IAF Update' },
        controller: function($scope, $location) {
            if($scope.release == undefined)
                $location.path("status");
        }
    });

    $locationProvider.html5Mode(false);

}
angular
    .module('iaf.beheerconsole')
    .config(config)
    .run(function($rootScope, $state) {
        $rootScope.$state = $state;
    });
