/**
 * MainCtrl - controller
 * Used on all pages except login/logout
 *
 */
function MainCtrl($scope, appConstants, Api, Hooks, $state, $location, Poller, Notification, dateFilter, $interval, Idle, $http, $uibModal) {
    $scope.loading = true;
    Pace.on("done", function() {
        if(appConstants.init == 0) {
            Api.Get("server/info", function(data) {
                appConstants.init = 2;
                if(!($location.path().indexOf("login") >= 0)) {
                    Idle.watch();
                    angular.element("body").removeClass("gray-bg");
                    angular.element(".main").show();
                    angular.element(".loading").hide();
                }

                var serverTime = Date.parse(new Date(data.serverTime).toUTCString());
                var localTime  = Date.parse(new Date().toUTCString());
                appConstants.timeOffset = serverTime - localTime;
                //setTime(appConstants.timeOffset);

                function updateTime() {
                    var serverDate = new Date();
                    serverDate.setTime(serverDate.getTime() - appConstants.timeOffset);
                    $scope.serverTime = dateFilter(serverDate, appConstants["console.dateFormat"]);
                }
                $interval(updateTime, 1000);
                updateTime();

                angular.element(".iaf-info").html("IAF " + data.version + ": " + data.name );

                $scope.configurations = data.configurations;

                //Was it able to retrieve the serverinfo without logging in?
                if(!$scope.loggedin) {
                    Idle.setTimeout(false);
                }
            });
            appConstants.init = 1;
            Api.Get("environmentvariables", function(data) {
                if(data["Application Constants"]) {
                    appConstants = $.extend(appConstants, data["Application Constants"]);
                    Hooks.call("appConstants", data);
                    var idleTime = (parseInt(appConstants["console.idle.time"]) > 0) ? parseInt(appConstants["console.idle.time"]) : false;
                    var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
                    Idle.setIdle(idleTime);
                    Idle.setTimeout(idleTimeout);
                }
            });
        }
    });

    var token = sessionStorage.getItem('authToken');
    $scope.loggedin = (token != null && token != "null") ? true : false;

    $scope.reloadRoute = function() {
        $state.reload();
    };

    $scope.alerts = [];

    $scope.addAlert = function(type, message) {
        var exists = false;
        for(alert in $scope.alerts) {
            if( $scope.alerts[alert].message == message)
                exists = true;
        }
        if(!exists)
            $scope.alerts.push({type: type, message: message});
    };

    $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
    };

    $scope.adapterSummary = {
        started:0,
        stopped:0,
        starting:0,
        stopping:0,
        error:0
    };
    $scope.receiverSummary = {
        started:0,
        stopped:0,
        starting:0,
        stopping:0,
        error:0
    };
    $scope.messageSummary = {
        info:0,
        warn:0,
        error:0
    };

    Hooks.register("appConstants:once", function() {
        Api.Get("server/warnings", function(warnings) {
            for(i in warnings) {
                var warning = warnings[i];
                var type = "warning";
                if(warning.type && (warning.type == "exception" || warning.type == "severe")) {
                    type = "danger";
                }
                $scope.addAlert(type, warning.message);
            }
        });

        Api.Get("adapters", function(allAdapters) {
            Hooks.call("adaptersLoaded", allAdapters);
            
            $scope.adapters = allAdapters;
            $scope.displayAdapters = [];
            for(adapter in allAdapters) {
                $scope.adapterSummary[allAdapters[adapter].state] += 1;
                Poller.add("adapters/" + adapter, function(data) {
                    var oldAdapterData = $scope.adapters[data.name];
                    if(oldAdapterData != data) {
                        if(oldAdapterData.state != data.state) {
                            //Is it up or down? Something has happened.
                            $scope.adapterSummary[oldAdapterData.state] -= 1;
                            $scope.adapterSummary[data.state] += 1;
                        }
                        data.receiverStopped = false;
                        for(x in data.receivers) {
                            var oldReceiverData = ($scope.adapters[data.name].receivers) ? $scope.adapters[data.name].receivers[x] : {state: "unknown"};
                            if(oldReceiverData.state != data.receivers[x].state) {
                                $scope.receiverSummary[oldReceiverData.state] -= 1;
                                $scope.receiverSummary[data.receivers[x].state] += 1;
                            }
                            if(data.receivers[x].started == false)
                                data.receiverStopped = true;
                        }
                        data.hasSender = false;
                        for(x in data.pipes) {
                            if(data.pipes[x].sender) {
                                data.hasSender = true;
                            }
                        }

                        data.status = data.started ? ((data.receiverStopped) ? 'warning' : 'started') : 'stopped';
                        $scope.adapters[data.name] = data;

                        updateMessageSummary();
                        Hooks.call("adapterUpdated", data);
                    }
                });
            }
        }, function() {
            $scope.addAlert('danger', "An error occured while trying to load adapters!");
        });
    });

    function updateMessageSummary() {
        var summary = {
            info:0,
            warn:0,
            error:0
        };
        for(adapter in $scope.adapters) {
            var adapter = $scope.adapters[adapter];
            for(i in adapter.messages) {
                var level = adapter.messages[i].level.toLowerCase();
                summary[level]++;
            }
        }
        $scope.messageSummary = summary;
    }

    Hooks.register("adapterUpdated:once", function(adapter) {
        if($location.hash()) {
            angular.element("#"+$location.hash())[0].scrollIntoView();
        }
        $scope.loading = false;
    });
    Hooks.register("adapterUpdated", function(adapter) {
        var name = adapter.name;
        if(name.length > 20)
            name = name.substring(0, 17) + "...";
        if(adapter.started == true) {
            for(x in adapter.receivers) {
                if(adapter.receivers[x].started == false) {
                    Notification.add('fa-exclamation-circle', "Receiver '"+name+"' stopped!", false, function() {
                        $location.path("status");
                        $location.hash(adapter.name);
                    });
                }
            }
        }
        else {
            Notification.add('fa-exclamation-circle', "Adapter '"+name+"' stopped!", false, function() {
                $location.path("status");
                $location.hash(adapter.name);
            });
        }
    });

    $scope.resetNotificationCount = function() { Notification.resetCount(); };
    $scope.$watch(function () { return Notification.getCount(); }, function () {
        $scope.notificationCount = Notification.getCount();
        $scope.notificationList = Notification.getLatest(5);
    });

    $scope.$on('IdleStart', function () {
        var pollerObj = Poller.getAll();
        for(x in pollerObj) {
            Poller.changeInterval(pollerObj[x], appConstants["console.idle.pollerInterval"]);
        }

        var idleTimeout = (parseInt(appConstants["console.idle.timeout"]) > 0) ? parseInt(appConstants["console.idle.timeout"]) : false;
        if(!idleTimeout) return;

        swal({
            title: "Idle timer...",
            text: "Your session will be terminated in <span class='idleTimer'>60:00</span> minutes.",
            type: "warning",
            showConfirmButton: false,
            showCloseButton: true
        });
    });

    $scope.$on('IdleWarn', function (e, time) {
        var minutes = Math.floor(time/60);
        var seconds  = Math.round(time%60);
        if(minutes < 10) minutes = "0" + minutes;
        if(seconds < 10) seconds = "0" + seconds;
        var elm = angular.element(".swal2-container").find(".idleTimer");
        elm.text(minutes + ":" + seconds);
    });

    $scope.$on('IdleTimeout', function () {
        swal({
            title: "Idle timer...",
            text: "You have been logged out due to inactivity.",
            type: "info",
            showCloseButton: true
        });
        $location.path("logout");
    });

    $scope.$on('IdleEnd', function () {
        var elm = angular.element(".swal2-container").find(".swal2-close");
        elm.click();

        var pollerObj = Poller.getAll();
        for(x in pollerObj) {
            Poller.changeInterval(pollerObj[x], 2000);
        }
    });

    $scope.openInfoModel = function () {
        $uibModal.open({
            templateUrl: 'views/information.html',
//            size: 'sm',
            controller: InformationCtrl
        });
    };

    checkVersion($http);
};

function InformationCtrl ($scope, $uibModalInstance, Api) {
    Api.Get("server/info", function(data) {
        $.extend( $scope, data );
    });
    $scope.close = function () {
        $uibModalInstance.close();
    };
};

function checkVersion($http) {
    /*
     * API call to github.
     * Since no releases have been published it wont show the latest version on git yet :L
    $http.get("https://api.github.com/repos/ibissource/iaf/releases").then(function(data) {
        console.log(data);
    });
    */
}

function StatusCtrl($scope, Api, Hooks) {
    this.filter = {
        "started": true,
        "stopped": true,
        "warning": true
    }
    $scope.filter = this.filter;
    $scope.hideAdapter = {};
    $scope.applyFilter = function(filter) {
        $scope.filter = filter;
        applyStatusFilter();
    };
    function applyStatusFilter() {
        for(adapter in $scope.adapters) {
            var adapter = $scope.adapters[adapter];
            $scope.hideAdapter[adapter.name] = false;
            for(x in $scope.filter) {
                if($scope.filter[x] == false && adapter.status == x) {
                    $scope.hideAdapter[adapter.name] = true;
                }
            }
        }
        applyConfigFilter();
    }
    function applyConfigFilter() {
        for(adapter in $scope.adapters) {
            var adapter = $scope.adapters[adapter];
            if($scope.hideAdapter[adapter.name] === true) continue;
            $scope.hideAdapter[adapter.name] = (adapter.configuration == $scope.selectedConfiguration || $scope.selectedConfiguration == "All") ? false : true;
        }
    }

    $scope.collapseAll = function() {
        $(".adapters").each(function(i,e) {
            var ibox = $(e);
            var icon = ibox.find(".ibox-tools").find('i:first');
            var content = ibox.find('div.ibox-content');
            content.slideUp(200);
            icon.removeClass('fa-chevron-down').addClass('fa-chevron-up');
        });
    };
    $scope.expandAll = function() {
        $(".adapters").each(function(i,e) {
            var ibox = $(e);
            var icon = ibox.find(".ibox-tools").find('i:first');
            var content = ibox.find('div.ibox-content');
            content.slideDown(200);
            icon.addClass('fa-chevron-down').removeClass('fa-chevron-up');
        });
    };
    $scope.stopAll = function() {
        var adapters = Array();
        for(adapter in $scope.adapters) {
            if($scope.hideAdapter[adapter] === true) continue;
           adapters.push(adapter);
        }
        Api.Put("adapters", {"action": "stop", "adapters": adapters});
    };
    $scope.startAll = function() {
        var adapters = Array();
        for(adapter in $scope.adapters) {
            if($scope.hideAdapter[adapter] === true) continue;
           adapters.push(adapter);
        }
        Api.Put("adapters", {"action": "start", "adapters": adapters});
    };
    $scope.reloadConfiguration = function() {
        swal("Method not yet implemented!");
    };
    $scope.fullReload = function() {
        swal("Method not yet implemented!");
    };
    $scope.showReferences = function() {
        swal("Method not yet implemented!");
    };

    $scope.selectedConfiguration = "All";
    $scope.changeConfiguration = function(name) {
        $scope.selectedConfiguration = name;
        applyStatusFilter();
    };

    Hooks.register("adapterUpdated:1", function(adapter) {
        $scope.hideAdapter[adapter.name] = false;
        for(x in $scope.filter) {
            if($scope.filter[x] == false && adapter.status == x) {
                $scope.hideAdapter[adapter.name] = true;
            }
        }
    });

    $scope.startAdapter = function(adapter) {
        Api.Put("adapters/" + adapter, {"action": "start"});
    };
    $scope.stopAdapter = function(adapter) {
        Api.Put("adapters/" + adapter, {"action": "stop"});
    };
    $scope.startReceiver = function(adapter, receiver) {
        Api.Put("adapters/" + adapter + "/receivers/" + receiver, {"action": "start"});
    };
    $scope.stopReceiver = function(adapter, receiver) {
        Api.Put("adapters/" + adapter + "/receivers/" + receiver, {"action": "stop"});
    };
};

function LoadingCtrl($scope) {
    $scope.$on('loading', function(event, loading) { $scope.loading = loading; });
};
function LogoutCtrl($scope, Poller, authService, Idle) {
    var pollerObj = Poller.getAll();
    for(x in pollerObj) {
        Poller.remove(pollerObj[x]);
    }
    Idle.unwatch();
    authService.logout();
};
function LoginCtrl($scope, authService, $timeout,  appConstants, Alert, $interval) {
    /*$interval(function() {
        $scope.notifications = Alert.get(true);
    }, 200);*/
    $timeout(function() {
        $scope.notifications = Alert.get();
        angular.element(".main").show();
        angular.element(".loading").hide();
        angular.element("body").addClass("gray-bg");
    }, 500);
    authService.loggedin(); //Check whether or not the client is logged in.
    $scope.credentials = {};
    $scope.login = function(credentials) {
        authService.login(credentials.username, credentials.password);
    };
};

function NotificationsCtrl($scope, Api, $stateParams, Hooks, Notification) {
    if($stateParams.id > 0) {
        $scope.notification = Notification.get($stateParams.id);
    }
    else {
        $scope.text = ("Showing a list with all notifications!");
    }

    Hooks.register("adapterUpdated:2", function(adapter) {
        console.warn("What is the scope of: ", adapter);
    });
};

function ExecuteJdbcQuery($scope, Api, $timeout) {
    $scope.error = "";
    Api.Get("jdbc/query", function(data) {
        $scope.jmsRealms = data.jmsRealms;
        $scope.resultTypes = data.resultTypes;
    });
    $scope.submit = function(formData) {
        if(!formData || !formData.realm || !formData.resultType || !formData.query) {
            $scope.error = "Please specify a jms realm, resulttype and query!";
            return;
        }
        Api.Post("jdbc/query", JSON.stringify(formData), function(returnData) {
            $scope.error = "";
            $scope.result = returnData;
        }, function(errorData, status, errorMsg) {
            var error = (errorData) ? errorData : errorMsg;
            $scope.error = error;
            $scope.result = "";
        });
    };
    $scope.reset = function() {
        $scope.form.realm = "";
        $scope.form.resultType = "";
        $scope.form.query = "";
        $scope.result = "";
    };
    $scope.cancel = function() {
        swal({
                title: "What does this do?",
                type: "info",
                showConfirmButton: false,
                showCloseButton: true
            });
    };
};
function ShowConfiguration($scope, Api) {
    this.configurationRadio = 'true';
    $scope.selectedConfiguration = "All";
    $scope.loadedConfiguration = true;

    $scope.loadedConfig = function(bool) {
        $scope.loadedConfiguration = (bool == "true") ? true : false;
        getConfiguration();
    };

    $scope.changeConfiguration = function(name) {
        $scope.selectedConfiguration = name;
        getConfiguration();
    };

    getConfiguration = function() {
        var uri = "configuration";
        if($scope.selectedConfiguration != "All") uri += "/" + $scope.selectedConfiguration;
        if($scope.loadedConfiguration) uri += "?loadedConfiguration=true";
        Api.Get(uri, function(data) {
            $scope.configuration = data;
        });
    };
    getConfiguration();
};

function environment_variables($scope, Api, appConstants) {
    $scope.variables = [];
    Api.Get("environmentvariables", function(data) {
        $scope.variables = data;
    });
};

function test_pipeline($scope, Api, Alert, $interval) {
    $scope.state = [];
    $scope.file = null;
    $scope.addNote = function(type, message, removeQueue) {
        $scope.state.push({type:type, message: message});
    };
    $scope.handleFile = function(files) {
        if(files.length == 0) {
            $scope.file = null;
            return;
        }
        $scope.file = files[0]; //Can only parse 1 file!
    };
    $scope.submit = function(formData) {
        $scope.result = "";
        $scope.state = [];
        if(!formData) return;

        var fd = new FormData();
        if(formData.adapter && formData.adapter != "")
            fd.append("adapter", formData.adapter);
        if(formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);
        if(formData.message && formData.message != "")
            fd.append("message", formData.message);
        if($scope.file)
            fd.append("file", $scope.file, $scope.file.name);
        
        if(!formData) {
            $scope.addNote("warning", "Please specify an adapter and message!");
            return;
        }
        if(!formData.adapter) {
            $scope.addNote("warning", "Please specify an adapter!");
            return;
        }
        if(!formData.message && !formData.file) {
            $scope.addNote("warning", "Please specify a file or message!");
            return;
        }
        Api.Post("test-pipeline", fd, { 'Content-Type': undefined }, function(returnData) {
            var warnLevel = "success";
            if(returnData.state == "ERROR") warnLevel = "danger";
            $scope.addNote(warnLevel, returnData.state);
            $scope.result = (returnData.result);
        }, function(returnData) {
            $scope.addNote("danger", returnData.state);
            $scope.result = (returnData.result);
        });
    };
};


function testServiceListner($scope, Api, Alert, $interval) {
    $scope.state = [];
    $scope.file = null;
    $scope.addNote = function(type, message, removeQueue) {
        $scope.state.push({type:type, message: message});
    };
    $scope.handleFile = function(files) {
        if(files.length == 0) {
            $scope.file = null;
            return;
        }
        $scope.file = files[0]; //Can only parse 1 file!
    };
    $scope.submit = function(formData) {
        $scope.result = "";
        $scope.state = [];
        if(!formData) return;

        var fd = new FormData();
        if(formData.service && formData.service != "")
            fd.append("service", formData.service);
        if(formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);
        if(formData.message && formData.message != "")
            fd.append("message", formData.message);
        if($scope.file)
            fd.append("file", $scope.file, $scope.file.name);
        
        if(!formData) {
            $scope.addNote("warning", "Please specify a service and message!");
            return;
        }
        if(!formData.adapter) {
            $scope.addNote("warning", "Please specify a service!");
            return;
        }
        if(!formData.message && !formData.file) {
            $scope.addNote("warning", "Please specify a file or message!");
            return;
        }
        Api.Post("test-serviceListner", fd, function(returnData) {
            var warnLevel = "success";
            if(returnData.state == "ERROR") warnLevel = "danger";
            $scope.addNote(warnLevel, returnData.state);
            $scope.result = (returnData.result);
        }, function(returnData) {
            $scope.addNote("danger", returnData.state);
            $scope.result = (returnData.result);
        });
    };
};

function translateCtrl($translate, $scope) {
    $scope.changeLanguage = function (langKey) {
        $translate.use(langKey);
        $scope.language = langKey;
    };
};

angular
    .module('iaf.beheerconsole')
    .controller('LoginCtrl', LoginCtrl)
    .controller('LogoutCtrl', LogoutCtrl)
    .controller('LoadingCtrl', LoadingCtrl)
    .controller('MainCtrl', MainCtrl)
//    .controller('InformationCtrl', InformationCtrl)
    .controller('StatusCtrl', StatusCtrl)
    .controller('NotificationsCtrl', NotificationsCtrl)
    .controller('ExecuteJdbcQuery', ExecuteJdbcQuery)
    .controller('ShowConfiguration', ShowConfiguration)
    .controller('environment_variables', environment_variables)
    .controller('testPipeline', test_pipeline)
    .controller('testServiceListner', testServiceListner)
    .controller('translateCtrl', translateCtrl);