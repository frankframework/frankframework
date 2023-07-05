import { AppComponent } from "./app.component";
import { appModule } from "./app.module";
import configurationsManageDetailsStateController from "./views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details-state.controller";
import iafUpdateStatusController from "./views/iaf-update/iaf-update-status.controller";
import iframeCustomViewStateController from "./views/iframe/iframe-custom-view/iframe-custom-view-state.controller";
import iframeLadybugBetaStateController from "./views/iframe/iframe-ladybug-beta/iframe-ladybug-beta-state.controller";
import iframeLadybugStateController from "./views/iframe/iframe-ladybug/iframe-ladybug-state.controller";
import iframeLarvaStateController from "./views/iframe/iframe-larva/iframe-larva-state.controller";
import storageStateController from "./views/storage/storage-state.controller";
import storageViewStateController from "./views/storage/storage-view/storage-view-state.controller";

appModule.config(['$httpProvider', function ($httpProvider) {
	$httpProvider.interceptors.push(['appConstants', '$q', 'Misc', 'Toastr', '$location', function (appConstants, $q, Misc, Toastr, $location) {
		var errorCount = 0;
		return {
			request: function (config) {
				//First check if we can append the version, then if it's an HTML file, and lastly if it's ours!
				if (ff_version != null && config.url.indexOf('.html') !== -1 && config.url.indexOf('views/') !== -1) {
					config.url = config.url + '?v=' + ff_version;
				}
				return config;
			},
			responseError: function (rejection) {
				if (rejection.config) { //It should always have a config object, but just in case!
					if (rejection.config.url && rejection.config.url.indexOf(Misc.getServerPath()) < 0) return $q.reject(rejection); //Don't capture non-api requests

					switch (rejection.status) {
						case -1:
							fetch(rejection.config.url, { redirect: "manual" }).then((res) => {
								if (res.type === "opaqueredirect") {
									// if the request ended in a redirect that failed, then login
									login_url = Misc.getServerPath() + 'iaf/';
									window.location.href = login_url;
								}
							});

							if (appConstants.init == 1) {
								if (rejection.config.headers["Authorization"] != undefined) {
									console.warn("Authorization error");
								} else {
									Toastr.error("Failed to connect to backend!");
								}
							}
							else if (appConstants.init == 2 && rejection.config.poller) {
								console.warn("Connection to the server was lost!");
								errorCount++;
								if (errorCount == 3) {
									Toastr.error({
										title: "Server Error",
										body: "Connection to the server was lost! Click to refresh the page.",
										timeout: 0,
										showCloseButton: true,
										clickHandler: function (_, isCloseButton) {
											if (isCloseButton !== true) {
												window.location.reload();
											}
											return true;
										}
									});
								}
							}
							break;
						case 400:
							Toastr.error("Request failed", "Bad Request, check the application logs for more information.");
							break;
						case 401:
							sessionStorage.clear();
							$location.path("login");
							break;
						case 403:
							Toastr.error("Forbidden", "You do not have the permissions to complete this operation.");
							break;
						case 500:
							if (rejection.config.intercept != undefined && rejection.config.intercept === false) return $q.reject(rejection); //Don't capture when explicitly disabled
							if (rejection.data != null && rejection.data.error != null) //When formatted data is returned, Toast it!
								Toastr.error("Server Error", rejection.data.error);
							break;
					}
				}
				// otherwise, default behaviour
				return $q.reject(rejection);
			}
		};
	}]);
}]).config(['$cookiesProvider', '$locationProvider', '$stateProvider', '$urlRouterProvider', /*'$ocLazyLoadProvider',*/ 'IdleProvider', 'KeepaliveProvider', 'appConstants', 'laddaProvider', '$anchorScrollProvider',
	function config($cookiesProvider, $locationProvider, $stateProvider, $urlRouterProvider, /*$ocLazyLoadProvider,*/ IdleProvider, KeepaliveProvider, appConstants, laddaProvider, $anchorScrollProvider) {

		if (appConstants["console.idle.time"] && appConstants["console.idle.time"] > 0) {
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
				component: 'app',
			})
			.state('pages.status', {
				url: "/status?configuration&filter&search",
				component: 'status',
				reloadOnSearch: false,
				data: {
					pageTitle: 'Adapter Status',
					breadcrumbs: 'Adapter > Status',
				},
				params: {
					configuration: { value: 'All', squash: true },
					filter: { value: 'started+stopped+warning', squash: true },
					search: { value: '', squash: true },
					adapter: { value: '', squash: true },
				},
				//parent: "pages"
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
				controller: storageStateController,
				params: {
					adapter: { value: '', squash: true },
					storageSourceName: { value: '', squash: true },
					processState: { value: '', squash: true },
					storageSource: { value: '', squash: true },
				},
				data: {
					pageTitle: '',
					breadcrumbs: ''
				},
			})
			.state('pages.storage.list', {
				url: "stores/:processState",
				templateUrl: "js/app/views/storage/storage-list/adapter_storage_list.html",
			})
			.state('pages.storage.view', {
				url: "stores/:processState/messages/:messageId",
				templateUrl: "js/app/views/storage/storage-view/adapter_storage_view.html",
				params: {
					messageId: { value: '', squash: true },
				},
				controller: storageViewStateController,
			})
			.state('pages.notifications', {
				url: "/notifications",
				templateUrl: "js/app/views/notifications/notifications.html",
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
				component: 'configurationsOverview',
				reloadOnSearch: false,
				data: {
					pageTitle: 'Configurations',
					breadcrumbs: 'Configurations > Show',
				},
				params: {
					name: { value: 'All', squash: true },
					loaded: { value: '', squash: true },
				}
			})
			.state('pages.upload_configuration', {
				url: "/configurations/upload",
				templateUrl: "js/app/views/configurations/configurations-upload/ManageConfigurationsUpload.html",
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Upload',
				}
			})
			.state('pages.manage_configurations', {
				url: "/configurations/manage",
				templateUrl: "js/app/views/configurations/configurations-manage/ManageConfigurations.html",
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Manage',
				}
			})
			.state('pages.manage_configuration_details', {
				url: "/configurations/manage/:name",
				templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage-details/ManageConfigurationDetails.html",
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Manage',
				},
				params: {
					name: "",
				},
				controller: configurationsManageDetailsStateController
			})
			.state('pages.logging_show', {
				url: "/logging?directory&file",
				templateUrl: "js/app/views/logging/ShowLogging.html",
				data: {
					pageTitle: 'Logging',
					breadcrumbs: 'Logging > Log Files'
				},
				params: {
					directory: null,
					file: null
				}
			})
			.state('pages.logging_manage', {
				url: "/logging/settings",
				templateUrl: "js/app/views/logging/logging-manage/ManageLogging.html",
				data: {
					pageTitle: 'Logging',
					breadcrumbs: 'Logging > Log Settings'
				},
			})
			.state('pages.send_message', {
				url: "/jms/send-message",
				component: "jmsSendMessage",
				data: {
					pageTitle: 'Send JMS Message',
					breadcrumbs: 'JMS > Send Message'
				}
			})
			.state('pages.browse_queue', {
				url: "/jms/browse-queue",
				component: "jmsBrowseQueue",
				data: {
					pageTitle: 'Browse JMS Queue',
					breadcrumbs: 'JMS > Browse Queue'
				}
			})
			.state('pages.test_pipeline', {
				url: "/test-pipeline",
				templateUrl: "js/app/views/test-pipeline/TestPipeline.html",
				data: {
					pageTitle: 'Test a PipeLine',
					breadcrumbs: 'Testing > Test a PipeLine'
				}
			})
			.state('pages.test_servicelistener', {
				url: "/test-serviceListener",
				templateUrl: "js/app/views/test-service-listener/TestServiceListener.html",
				data: {
					pageTitle: 'Test a ServiceListener',
					breadcrumbs: 'Testing > Test a ServiceListener'
				}
			})
			.state('pages.webservices', {
				url: "/webservices",
				templateUrl: "js/app/views/webservices/Webservices.html",
				data: {
					pageTitle: 'Webservices',
					breadcrumbs: 'Webservices'
				}
			})
			.state('pages.scheduler', {
				url: "/scheduler",
				templateUrl: "js/app/views/scheduler/ShowScheduler.html",
				data: {
					pageTitle: 'Scheduler',
					breadcrumbs: 'Scheduler'
				}
			})
			.state('pages.add_schedule', {
				url: "/scheduler/new",
				templateUrl: "js/app/views/scheduler/AddEditSchedule.html",
				data: {
					pageTitle: 'Add Schedule',
					breadcrumbs: 'Scheduler > Add Schedule'
				},
				controller: 'AddScheduleCtrl'
			})
			.state('pages.edit_schedule', {
				url: "/scheduler/edit/:group/:name",
				templateUrl: "js/app/views/scheduler/AddEditSchedule.html",
				data: {
					pageTitle: 'Edit Schedule',
					breadcrumbs: 'Scheduler > Edit Schedule'
				},
				controller: 'EditScheduleCtrl',
				params: {
					name: "",
					group: ""
				}
			})
			.state('pages.environment_variables', {
				url: "/environment-variables",
				templateUrl: "js/app/views/environment-variables/ShowEnvironmentVariables.html",
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
				templateUrl: "js/app/views/security-items/ShowSecurityItems.html",
				data: {
					pageTitle: 'Security Items',
					breadcrumbs: 'Security Items'
				}
			})
			.state('pages.connection_overview', {
				url: "/connections",
				templateUrl: "js/app/views/connections/ShowConnectionOverview.html",
				data: {
					pageTitle: 'Connection Overview',
					breadcrumbs: 'Connection Overview'
				}
			})
			.state('pages.inlinestore_overview', {
				url: "/inlinestores/overview",
				templateUrl: "js/app/views/inlinestore/ShowInlineMessageStoreOverview.html",
				data: {
					pageTitle: 'InlineStore Overview',
					breadcrumbs: 'InlineStore Overview'
				}
			})
			.state('pages.monitors', {
				url: "/monitors?configuration",
				templateUrl: "js/app/views/monitors/ShowMonitors.html",
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
				templateUrl: "js/app/views/monitors-add-edit/EditMonitorTrigger.html",
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
				templateUrl: "js/app/views/monitors-add-edit/EditMonitorTrigger.html",
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
				templateUrl: "js/app/views/iframe/iFrame.html",
				data: {
					pageTitle: "Custom View",
					breadcrumbs: 'Custom View',
					iframe: true
				},
				params: {
					name: { value: '', squash: true },
					url: { value: '', squash: true },
				},
				controller: iframeCustomViewStateController
			})
			.state('pages.larva', {
				url: "/testing/larva",
				templateUrl: "js/app/views/iframe/iFrame.html",
				data: {
					pageTitle: 'Larva',
					breadcrumbs: 'Testing > Larva',
					iframe: true
				},
				controller: iframeLarvaStateController
			})
			.state('pages.ladybug', {
				url: "/testing/ladybug",
				templateUrl: "js/app/views/iframe/iFrame.html",
				data: {
					pageTitle: 'Ladybug',
					breadcrumbs: 'Testing > Ladybug',
					iframe: true
				},
				controller: iframeLadybugStateController
			})
			.state('pages.ladybug_beta', {
				url: "/testing/ladybug-beta",
				templateUrl: "js/app/views/iframe/iFrame.html",
				data: {
					pageTitle: 'Ladybug (beta)',
					breadcrumbs: 'Testing > Ladybug (beta)',
					iframe: true
				},
				controller: iframeLadybugBetaStateController
			})
			.state('pages.empty_page', {
				url: "/empty_page",
				templateUrl: "js/app/views/empty/empty_page.html",
				data: { pageTitle: 'Empty Page' }
			})
			.state('pages.iaf_update', {
				url: "/iaf-update",
				templateUrl: "js/app/views/iaf-update/iaf-update.html",
				data: { pageTitle: 'IAF Update' },
				controller: iafUpdateStatusController
			})
			.state('pages.loading', {
				url: "/",
				templateUrl: "js/app/views/loading/loading.html",
			})
			.state('pages.errorpage', {
				url: "/error",
				templateUrl: "js/app/views/error/errorpage.html",
			});

	}]).run(['$rootScope', '$state', 'Debug', '$trace', function ($rootScope, $state, Debug, $trace) {
		// Set this asap on localhost to capture all debug data
		if (location.hostname == "localhost")
			Debug.setLevel(3);

		$rootScope.$state = $state;
		// $trace.enable('TRANSITION');

		$rootScope.foist = function (callback) {
			Debug.warn("Dirty injection!", callback);
			try {
				callback($rootScope);
			}
			catch (err) {
				Debug.error("Failed to execute injected code!", err);
			}
			finally {
				$rootScope.$apply();
			}
		};

		$rootScope.setLogLevel = function (level) {
			Debug.setLevel(level);
		};
	}]);
