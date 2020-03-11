angular.module('iaf.beheerconsole').config(['$locationProvider', '$stateProvider', '$urlRouterProvider', '$ocLazyLoadProvider', 'IdleProvider', 'KeepaliveProvider', 'appConstants', 'laddaProvider',
	function config($locationProvider, $stateProvider, $urlRouterProvider, $ocLazyLoadProvider, IdleProvider, KeepaliveProvider, appConstants, laddaProvider) {

	if(appConstants["console.idle.time"] && appConstants["console.idle.time"] > 0) {
		IdleProvider.idle(appConstants["console.idle.time"]);
		IdleProvider.timeout(appConstants["console.idle.timeout"]);
	}

	$urlRouterProvider.otherwise("/");

	$ocLazyLoadProvider.config({
		modules: [{
			name: 'toaster',
			files: ['js/plugins/toastr/toastr.min.js', 'css/plugins/toastr/toastr.min.css']
		}, {
			name: 'datatables',
			serie: true,
			files: [
				'js/plugins/dataTables/datatables.v1.10.20.min.js',
				'css/plugins/dataTables/datatables.v1.10.20.min.css',
				'js/plugins/dataTables/angular-datatables.v0.6.2.min.js',
				'js/plugins/dataTables/angular-datatables.buttons.min.js'
			]
		}, {
			serie: true,
			name: 'chartjs',
			files: ['js/plugins/chartJs/Chart.min.js', 'js/plugins/chartJs/angular-chart.min.js', 'css/plugins/chartJs/Chart.min.css']
		}],
		// Set to true if you want to see what and when is dynamically loaded
		debug: true
	});

	laddaProvider.setOption({
		style: 'expand-right'
	});

	$stateProvider
	.state('login', {
		url: "/login",
		templateUrl: "views/login.html",
		controller: 'LoginCtrl',
		data: {
			pageTitle: 'Login'
		}
	})
	.state('logout', {
		url: "/logout",
		controller: 'LogoutCtrl',
		data: {
			pageTitle: 'Logout'
		}
	})

	.state('pages', {
		abstract: true,
		controller: function($scope, authService, $location, $state) {
			authService.loggedin(); //Check if the user is logged in.
			$scope.monitoring = false;
			$scope.config_database = false;

			angular.element(".main").show();
			angular.element(".loading").remove();
		},
		templateUrl: "views/common/content.html",
	})
	.state('pages.status', {
		url: "/status?configuration&filter&search",
		templateUrl: "views/ShowConfigurationStatus.html",
		controller: 'StatusCtrl as status',
		data: {
			pageTitle: 'Adapter Status',
			breadcrumbs: 'Adapter > Status',
		},
		params: {
			configuration: { value: 'All', squash: true},
			filter: { value: 'started+stopped+warning', squash: true},
			search: { value: '', squash: true},
			adapter: { value: '', squash: true},
		},
		//parent: "pages"
	})
	.state('pages.adapter', {
		url: "/adapter",
		templateUrl: "views/ShowConfigurationStatus.html",
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
		resolve: {
			loadPlugin: function($ocLazyLoad) {
				return $ocLazyLoad.load('chartjs');
			},
		},
	})
	.state('pages.errorstorage', {
		abstract: true,
		url: "/adapter/:adapter/:receiver/",
		template: "<div ui-view></div>",
		controller: 'ErrorStorageBaseCtrl',
		data: {
			pageTitle: 'ErrorStorage',
			breadcrumbs: 'Adapter > ErrorStorage'
		},
		params: {
			adapter: { value: '', squash: true},
			receiver: { value: '', squash: true},
		},
	})
	.state('pages.errorstorage.list', {
		url: "errorstorage",
		templateUrl: "views/txstorage/adapter_errorstorage_list.html",
		resolve: {
			loadPlugin: function($ocLazyLoad) {
				return $ocLazyLoad.load('datatables');
			},
		},
	})
	.state('pages.errorstorage.view', {
		url: "errorstorage/:messageId",
		templateUrl: "views/txstorage/adapter_errorstorage_view.html",
		params: {
			messageId: { value: '', squash: true},
		},
		controller: function($state) {
			$state.current.data.breadcrumbs = "Adapter > ErrorStorage > View Message "+$state.params.messageId;
		}
	})
	.state('pages.messagelog', {
		abstract: true,
		url: "/adapter/:adapter/",
		template: "<div ui-view></div>",
		controller: 'MessageLogBaseCtrl',
		data: {
			pageTitle: 'Adapter',
			breadcrumbs: 'Adapter > MessageLog'
		},
		params: {
			adapter: { value: '', squash: true},
			receiver: { value: '', squash: true},
		},
	})
	.state('pages.messagelog.list', {
		url: "receiver/:receiver/messagelog",
		templateUrl: "views/txstorage/adapter_messagelog_list.html",
		resolve: {
			loadPlugin: function($ocLazyLoad) {
				return $ocLazyLoad.load('datatables');
			},
		},
	})
	.state('pages.messagelog.view', {
		url: "receiver/:receiver/messagelog/:messageId",
		templateUrl: "views/txstorage/adapter_messagelog_view.html",
		params: {
			messageId: { value: '', squash: true},
		},
		controller: function($state) {
			$state.current.data.breadcrumbs = "Adapter > MessageLog > View Message "+$state.params.messageId;
		}
	})
	.state('pages.pipemessagelog', {
		abstract: true,
		url: "/adapter/:adapter/pipe/:pipe",
		template: "<div ui-view></div>",
		controller: 'PipeMessageLogBaseCtrl',
		data: {
			pageTitle: 'Adapter',
			breadcrumbs: 'Adapter > Pipe > MessageLog'
		},
		params: {
			adapter: { value: '', squash: true},
			pipe: { value: '', squash: true},
		},
	})
	.state('pages.pipemessagelog.list', {
		url: "/messagelog",
		templateUrl: "views/txstorage/pipe_messagelog_list.html",
		resolve: {
			loadPlugin: function($ocLazyLoad) {
				return $ocLazyLoad.load('datatables');
			},
		},
	})
	.state('pages.pipemessagelog.view', {
		url: "/messagelog/:messageId",
		templateUrl: "views/txstorage/pipe_messagelog_view.html",
		params: {
			messageId: { value: '', squash: true},
		},
		controller: function($state) {
			$state.current.data.breadcrumbs = "Adapter > Pipe > MessageLog > View Message "+$state.params.messageId;
		}
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
		controller: 'NotificationsCtrl'
	})
	.state('pages.configuration', {
		url: "/configurations",
		templateUrl: "views/ShowConfiguration.html",
		data: {
			pageTitle: 'Configurations',
			breadcrumbs: 'Configuration > Show',
		}
	})
	.state('pages.upload_configuration', {
		url: "/configurations/upload",
		templateUrl: "views/ManageConfigurationsUpload.html",
		data: {
			pageTitle: 'Manage Configurations',
			breadcrumbs: 'Configurations > Upload',
		}
	})
	.state('pages.manage_configurations', {
		url: "/configurations/manage",
		templateUrl: "views/ManageConfigurations.html",
		data: {
			pageTitle: 'Manage Configurations',
			breadcrumbs: 'Configurations > Manage',
		}
	})
	.state('pages.manage_configuration_details', {
		url: "/configurations/manage/:name",
		templateUrl: "views/ManageConfigurationDetails.html",
		data: {
			pageTitle: 'Manage Configurations',
			breadcrumbs: 'Configurations > Manage',
		},
		params: {
			name: "",
		},
		controller: function($state) {
			if($state.params && $state.params.name && $state.params.name != "")
				$state.$current.data.breadcrumbs = "Configurations > Manage > " + $state.params.name;
			else
				$state.go("pages.manage_configurations");
		}
	})
	.state('pages.logging', {
		url: "/logging?directory&file",
		templateUrl: "views/ShowLogging.html",
		data: {
			pageTitle: 'Logging',
			breadcrumbs: 'Show Logging'
		},
		params : {
			directory : null,
			file : null
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
	.state('pages.add_schedule', {
		url: "/scheduler/new",
		templateUrl: "views/AddEditSchedule.html",
		data: {
			pageTitle: 'Add Schedule',
			breadcrumbs: 'Scheduler > Add Schedule'
		},
		controller: 'AddScheduleCtrl'
	})
	.state('pages.edit_schedule', {
		url: "/scheduler/edit/:group/:name",
		templateUrl: "views/AddEditSchedule.html",
		data: {
			pageTitle: 'Edit Schedule',
			breadcrumbs: 'Scheduler > Edit Schedule'
		},
		controller: 'EditScheduleCtrl',
		params: {
			name:"",
			group:""
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
	.state('pages.customView', {
		url: "/customView/:name",
		templateUrl: "views/iFrame.html",
		data: {
			pageTitle: "Custom View",
			breadcrumbs: 'Custom View',
			iframe: true
		},
		params: {
			name: { value: '', squash: true},
			url: { value: '', squash: true},
		},
		controller: function($scope, Misc, $state, $window) {
			if($state.params.url == "")
				$state.go('pages.status');

			if($state.params.url.indexOf("http") > -1) {
				$window.open($state.params.url, $state.params.name);
				$scope.redirectURL = $state.params.url;
			}
			else
				$scope.url = Misc.getServerPath() + $state.params.url;
		}
	})
	.state('pages.larva', {
		url: "/testing/larva",
		templateUrl: "views/iFrame.html",
		data: {
			pageTitle: 'Larva',
			breadcrumbs: 'Test > Larva',
			iframe: true
		},
		controller: function($scope, Misc, $interval){
			$scope.url = Misc.getServerPath() + "larva";
		}
	})
	.state('pages.ladybug', {
		url: "/testing/ladybug",
		templateUrl: "views/iFrame.html",
		data: {
			pageTitle: 'Ladybug',
			breadcrumbs: 'Test > Ladybug',
			iframe: true
		},
		controller: function($scope, Misc, $timeout){
			$scope.url = Misc.getServerPath() + "testtool";
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
		controller: function($scope, $location, Session) {
			$scope.release = Session.get("IAF-Release");
			if($scope.release == undefined)
				$location.path("status");
		}
	})
	.state('pages.loading', {
		url: "/",
		templateUrl: "views/common/loading.html",
	})
	.state('pages.errorpage', {
		url: "/error",
		templateUrl: "views/common/errorpage.html",
	});

	$locationProvider.html5Mode(false);

}]).run(['$rootScope', '$state', 'Debug', 'gTag', function($rootScope, $state, Debug, gTag) {
	// Set this asap on localhost to capture all debug data
	if(location.hostname == "localhost")
		Debug.setLevel(3);

	$rootScope.$state = $state;

	$rootScope.foist = function(callback) {
		Debug.warn("Dirty injection!", callback);
		try {
			callback($rootScope);
		}
		catch(err) {
			Debug.error("Failed to execute injected code!", err);
		}
		finally {
			$rootScope.$apply();
		}
	};

	$rootScope.setLogLevel = function(level) {
		Debug.setLevel(level);
	};

	gTag.setTrackingId("UA-111373008-1");
}]);
