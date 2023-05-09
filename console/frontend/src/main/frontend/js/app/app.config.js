import pagesController from "./components/pages/pages.controller";
import storageController from "./views/storage/storage.controller";
import storageViewController from "./views/storage/storage-view/storage-view.controller";

angular.module('iaf.beheerconsole').config(['$cookiesProvider', '$locationProvider', '$stateProvider', '$urlRouterProvider', /*'$ocLazyLoadProvider',*/ 'IdleProvider', 'KeepaliveProvider', 'appConstants', 'laddaProvider', '$anchorScrollProvider',
	function config($cookiesProvider, $locationProvider, $stateProvider, $urlRouterProvider, /*$ocLazyLoadProvider,*/ IdleProvider, KeepaliveProvider, appConstants, laddaProvider, $anchorScrollProvider) {

	if(appConstants["console.idle.time"] && appConstants["console.idle.time"] > 0) {
		IdleProvider.idle(appConstants["console.idle.time"]);
		IdleProvider.timeout(appConstants["console.idle.timeout"]);
	}

	$urlRouterProvider.otherwise("/");
	$locationProvider.html5Mode(false);
	$anchorScrollProvider.disableAutoScrolling();

	$cookiesProvider.defaults.secure = (location.protocol == "https:");
	$cookiesProvider.defaults.samesite = 'strict';

	laddaProvider.setOption({
		style: 'expand-right'
	});

	$stateProvider
	.state('login', {
		url: "/login",
		templateUrl: "js/app/views/login/login.html",
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
		controller: pagesController,
		templateUrl: "js/app/components/pages/content.html",
	})
	.state('pages.status', {
		url: "/status?configuration&filter&search",
		templateUrl: "js/app/views/ShowConfigurationStatus.html",
		controller: 'StatusCtrl as status',
		reloadOnSearch: false,
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
		templateUrl: "js/app/views/ShowConfigurationStatus.html",
	})
	.state('pages.adapterstatistics', {
		url: "/adapter/:name/statistics",
		templateUrl: "js/app/views/adapterstatistics/adapter_statistics.html",
		data: {
			pageTitle: 'Adapter',
			breadcrumbs: 'Adapter > Statistics'
		},
		params: {
			id: 0,
		}
	})
	.state('pages.storage', {
		abstract: true,
		url: "/adapters/:adapter/:storageSource/:storageSourceName/",
		template: "<div ui-view ng-controller='StorageBaseCtrl'></div>",
		controller: storageController,
		params: {
			adapter: { value: '', squash: true},
			storageSourceName: { value: '', squash: true},
			processState: { value: '', squash: true},
			storageSource: { value: '', squash: true},
		},
		data: {
			pageTitle: '',
			breadcrumbs: ''
		},
	})
	.state('pages.storage.list', {
		url: "stores/:processState",
		templateUrl: "js/app/views/storage/adapter_storage_list.html",
	})
	.state('pages.storage.view', {
		url: "stores/:processState/messages/:messageId",
		templateUrl: "js/app/views/storage/storage-view/adapter_storage_view.html",
		params: {
			messageId: { value: '', squash: true},
		},
		controller: storageViewController,
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
		url: "/configurations?name&loaded",
		templateUrl: "views/ShowConfiguration.html",
		reloadOnSearch: false,
		data: {
			pageTitle: 'Configurations',
			breadcrumbs: 'Configurations > Show',
		},
		params: {
			name: { value: 'All', squash: true},
			loaded: { value: '', squash: true},
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
	.state('pages.logging_show', {
		url: "/logging?directory&file",
		templateUrl: "views/ShowLogging.html",
		data: {
			pageTitle: 'Logging',
			breadcrumbs: 'Logging > Log Files'
		},
		params : {
			directory : null,
			file : null
		}
	})
	.state('pages.logging_manage', {
		url: "/logging/settings",
		templateUrl: "views/ManageLogging.html",
		data: {
			pageTitle: 'Logging',
			breadcrumbs: 'Logging > Log Settings'
		},
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
			breadcrumbs: 'Testing > Test a PipeLine'
		}
	})
	.state('pages.test_servicelistener', {
		url: "/test-serviceListener",
		templateUrl: "views/TestServiceListener.html",
		data: {
			pageTitle: 'Test a ServiceListener',
			breadcrumbs: 'Testing > Test a ServiceListener'
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
	.state('pages.connection_overview', {
		url: "/connections",
		templateUrl: "views/ShowConnectionOverview.html",
		data: {
			pageTitle: 'Connection Overview',
			breadcrumbs: 'Connection Overview'
		}
	})
	.state('pages.inlinestore_overview', {
		url: "/inlinestores/overview",
		templateUrl: "views/ShowInlineMessageStoreOverview.html",
		data: {
			pageTitle: 'InlineStore Overview',
			breadcrumbs: 'InlineStore Overview'
		}
	})
	.state('pages.monitors', {
		url: "/monitors?configuration",
		templateUrl: "views/ShowMonitors.html",
		data: {
			pageTitle: 'Monitors',
			breadcrumbs: 'Monitors'
		},
		params: {
			configuration: { value: null, squash: true},
		},
	})
	.state('pages.monitors_editTrigger', {
		url: "/monitors/:monitor/triggers/:trigger?configuration",
		templateUrl: "views/EditMonitorTrigger.html",
		data: {
			pageTitle: 'Edit Trigger',
			breadcrumbs: 'Monitors > Triggers > Edit'
		},
		params: {
			configuration: { value: null, squash: true},
			monitor: "",
			trigger: "",
		},
	})
	.state('pages.monitors_addTrigger', {
		url: "/monitors/:monitor/triggers/new?configuration",
		templateUrl: "views/EditMonitorTrigger.html",
		data: {
			pageTitle: 'Add Trigger',
			breadcrumbs: 'Monitors > Triggers > Add'
		},
		params: {
			configuration: { value: null, squash: true},
			monitor: "",
		},
	})
	.state('pages.ibisstore_summary', {
		url: "/ibisstore-summary",
		templateUrl: "views/ShowIbisstoreSummary.html",
		data: {
			pageTitle: 'Ibisstore Summary',
			breadcrumbs: 'JDBC > Ibisstore Summary'
		}
	})
	.state('pages.liquibase', {
		url: "/liquibase",
		templateUrl: "views/ShowLiquibaseScript.html",
		data: {
			pageTitle: 'Liquibase Script',
			breadcrumbs: 'JDBC > Liquibase Script'
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
			breadcrumbs: 'Testing > Larva',
			iframe: true
		},
		controller: function($scope, Misc, $interval){
			$scope.url = Misc.getServerPath() + "iaf/larva";
		}
	})
	.state('pages.ladybug', {
		url: "/testing/ladybug",
		templateUrl: "views/iFrame.html",
		data: {
			pageTitle: 'Ladybug',
			breadcrumbs: 'Testing > Ladybug',
			iframe: true
		},
		controller: function($scope, Misc, $timeout){
			$scope.url = Misc.getServerPath() + "iaf/testtool";
		}
	})
	.state('pages.ladybug_beta', {
		url: "/testing/ladybug-beta",
		templateUrl: "views/iFrame.html",
		data: {
			pageTitle: 'Ladybug (beta)',
			breadcrumbs: 'Testing > Ladybug (beta)',
			iframe: true
		},
		controller: function($scope, Misc){
			$scope.url = Misc.getServerPath() + "iaf/ladybug";
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

}]).run(['$rootScope', '$state', 'Debug', function($rootScope, $state, Debug) {
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
}]);
