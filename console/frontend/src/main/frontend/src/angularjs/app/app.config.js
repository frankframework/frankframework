import { AppComponent } from "./app.component";
import { appModule } from "./app.module";

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
				component: "login",
				data: {
					pageTitle: 'Login'
				}
			})
			.state('logout', {
				url: "/logout",
				component: 'logout',
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
				url: "/:configuration/adapter/:name/statistics",
				component: "adapterstatistics",
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
				url: "/:configuration/adapters/:adapter/:storageSource/:storageSourceName/",
				component: "storage",
				params: {
					configuration: { value: '', squash: true },
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
				component: "storageList",
			})
			.state('pages.storage.view', {
				url: "stores/:processState/messages/:messageId",
				component: "storageView",
				params: {
					messageId: { value: '', squash: true },
				},
			})
			.state('pages.notifications', {
				url: "/notifications",
				component: "notifications",
				data: {
					pageTitle: 'Notifications',
					breadcrumbs: 'Notifications'
				},
				params: {
					id: 0,
				},
			})
			.state('pages.configuration', {
				url: "/configurations?name&loaded",
				component: 'configurationsShow',
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
				component: 'configurationsUpload',
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Upload',
				}
			})
			.state('pages.manage_configurations', {
				url: "/configurations/manage",
				component: 'configurationsManage',
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Manage',
				}
			})
			.state('pages.manage_configuration_details', {
				url: "/configurations/manage/:name",
				component: 'configurationsManageDetails',
				data: {
					pageTitle: 'Manage Configurations',
					breadcrumbs: 'Configurations > Manage',
				},
				params: {
					name: "",
				},
			})
			.state('pages.logging_show', {
				url: "/logging?directory&file",
				component: "logging",
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
				component: "loggingManage",
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
				component: "testPipeline",
				data: {
					pageTitle: 'Test a PipeLine',
					breadcrumbs: 'Testing > Test a PipeLine'
				}
			})
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
