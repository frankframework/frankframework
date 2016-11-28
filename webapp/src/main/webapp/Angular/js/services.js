angular.module('iaf.beheerconsole')
    .service('Api', ['$http', 'appConstants', function($http, appConstants) {
        var absolutePath = appConstants.server;
        if(!absolutePath) {
        	absolutePath = window.location.origin;
        	var path = window.location.pathname;
        	absolutePath += path.substr(0, path.indexOf("Angular"));
        }
        if(absolutePath && absolutePath.slice(-1) != "/") absolutePath += "/";
        absolutePath += "api/";
        var etags = [];
        
        //$http.defaults.headers.common['Authorization'] = 'Basic dGVzdDp0ZXN0';
        //$http.defaults.headers.post["Content-Type"] = "text/plain";

        this.Get = function (uri, callback, error) {
            if(etags.hasOwnProperty(uri)) {
                $http.defaults.headers.common['If-None-Match'] = etags[uri]; //Set previously cached ETag
            }

            return $http.get(absolutePath + uri).then(function(response) {
                if(callback && typeof callback === 'function') {
                    if(response.headers("etag")) {
                        etags[uri] = response.headers("etag");
                    }
                    callback(response.data);
                }
            }, function(response){ errorException(response, error); });
        };

        this.Post = function () { // uri, object, callback, error || uri, object, headers, callback, error
            var args = Array.prototype.slice.call(arguments);
            var uri = args.shift();
            var object = args.shift();
            var headers = {};
            if(args.length == 3) {
                headers = args.shift();
            }
            var callback = args.shift();
            var error = args.shift();

            return $http.post(absolutePath + uri, object, {
                headers: headers,
                transformRequest: angular.identity,
            }).then(function(response){
                if(callback && typeof callback === 'function') {
                    etags[uri] = response.headers("etag");
                    callback(response.data);
                }
            }, function(response){ errorException(response, error); });
        };

        this.Put = function (uri, object, callback, error) {
            return $http.put(absolutePath + uri, object).then(function(response){
                if(callback && typeof callback === 'function') {
                    etags[uri] = response.headers("etag");
                    callback(response.data);
                }
            }, function(response){ errorException(response, error); });
        };

        var errorException = function (response, callback) {
            if(response.status != 304) {
                var status = (response.status > 0) ? " " + response.status + " error" : "n unknown error";
                if(response.status == 404 || response.status == 500) {
                    var config = response.config;
                    var debug = "DEBUG: url["+config.url+"] method["+config.method+"]";
                    if(config.data && config.data != "") debug += " data["+config.data+"]";
                    console.error("A" + status + " occurred, please notify a system administrator!" + '\n' + debug);
                }
                else{
                    console.warn("A" + status + " occured.", response);
                }

                if((response.status != 304) && (callback && typeof callback === 'function')) {
                    callback(response.data, response.status, response.statusText);
                }
            }
        };

        //Getters
        this.errorException = errorException;
        this.absolutePath = absolutePath;
        this.etags = etags;

    }]).service('Poller', ['Api', 'appConstants', function(Api, appConstants) {
        var data = {};
        this.createPollerObject = function(uri, callback) {
            this.uri = uri;
            this.waiting = false;
            this.pollerInterval = appConstants["console.pollerInterval"]; //Default to 2 seconds.
            this.ai = {
                list: [],
                avg: 0,
                push: function(obj) {
                    this.list.push(obj);
                    if(this.list.length == 10) {
                        var tmp = 0;
                        for (var i = this.list.length - 1; i >= 0; i--) {
                            tmp += this.list[i];
                        }
                        this.avg = Math.round((tmp / 10) / 100 ) * 100;
                        this.list = [];
                        return this.avg;
                    }
                }
            };
            this.started = function() {return (this.poller) ? true : false;};
            this.stop = function() {
                if(!this.started()) return;

                if(this.waiting)
                    clearTimeout(this.poller);
                else
                    clearInterval(this.poller);
                delete this.poller;
            };
            this.fn = function() {
                var self = this;
                Api.Get(uri, callback, function() {
                    data[uri].stop();
                }).then(function() {
                    var p = data[uri];
                    if(p && p.waiting)
                        p.start();
                });
            };
            this.start = function() {
                if(this.started() && !this.waiting) return;

                if(this.waiting) {
                    var now = new Date().getTime();
                    if(this.lastPolled) {
                        var timeBetweenLastPolledAndNow = now - this.lastPolled;
                        var interval = this.ai.push(timeBetweenLastPolledAndNow);
                        if(interval > 0) {
                            this.changeInterval(interval, false);
                            this.waitForResponse(false);
                            return;
                        }
                    }
                    this.poller = setTimeout(this.fn, this.pollerInterval);
                    this.lastPolled = now;
                }
                else
                    this.poller = setInterval(this.fn, this.pollerInterval);
            };
            this.changeInterval = function(interval, restart) {
                var restart = (restart === false) ? false : true;
                console.warn("Interval for " + this.uri + " changed to: " + interval);
                this.pollerInterval = interval;
                if(restart)
                    this.restart();
            };
            this.waitForResponse = function(bool) {
                this.stop();
                delete this.lastPolled;
                this.waiting = !!bool;
                this.start();
            };
            this.restart = function() {
                this.stop();
                this.start();
            };
            this.start();
        },
        this.changeInterval = function(uri, interval) {
            data[uri].changeInterval(interval);
        },
        this.add = function (uri, callback) {
            data[uri] = new this.createPollerObject(uri, callback);
        },
        this.remove = function (uri) {
            data[uri].stop();
            delete data[uri];
        },
        this.getAll = function () {
            var list = [];
            for(uri in data) {
                list.push(uri);
            }
            return list;
        };
    }]).service('Notification', ['$rootScope', '$timeout', function($rootScope, $timeout) {
        var list = [];
        var count = 0;
        this.add = function(icon, title, msg, fn) {
            var obj = {
                icon: icon,
                title: title,
                message: (msg) ? msg : false,
                fn: (fn) ? fn: false,
                time: new Date().getTime()
            };
            list.unshift(obj);
            obj.id = list.length;
            count++;
        };
        this.get = function(id) {
            for (var i = 0; i < list.length; i++) {
                var notification = list[i];
                if(notification.id == id) {
                    if(notification.fn) {
                        $timeout(function(){
                            notification.fn.apply(this, notification);
                        }, 50);
                    }
                    return notification;
                }
            }

            return false;
        };
        this.resetCount = function() {
            count = 0;
        };
        this.getCount = function() {
            return count;
        };
        this.getLatest = function(amount) {
            if(amount < 1) amount = 1;
            return list.slice(0, amount);
        };
    }]).service('Session', function() {
        this.get = function(key) {
            return JSON.parse(sessionStorage.getItem(key));
        };
        this.set = function(key, value) {
            sessionStorage.setItem(key, JSON.stringify(value));
        };
        this.remove = function(key) {
            sessionStorage.removeItem(key);
        };
    }).service('Hooks', ['$rootScope', '$timeout', function($rootScope, $timeout) {
        this.call = function() {
            $rootScope.callHook.apply(this, arguments);
            //$rootScope.$broadcast.apply(this, arguments);
        };
        this.register = function() {
            $rootScope.registerHook.apply(this, arguments);
            //$rootScope.$on.apply(this, arguments);
        };
    }]).run(function($rootScope, $timeout) {
        $rootScope.hooks = [];

        $rootScope.callHook = function() {
            var args = Array.prototype.slice.call(arguments);
            var name = args.shift();
            //when this is called execute:
            $timeout( function () {
                if($rootScope.hooks.hasOwnProperty(name)) {
                    var hooks = $rootScope.hooks[name];
                    for(id in hooks) {
                        hooks[id].apply(this, args);
                        if(id == "once") {
                            $rootScope.removeHook(name, id);
                        }
                    }
                }
                /*
                else {
                    console.warn("Hook: '" + name + "' does not exist!");
                }*/
            }, 50);
        };
        $rootScope.registerHook = function() {
            var args = Array.prototype.slice.call(arguments);
            var name = args.shift();
            var id = 0;
            if(name.indexOf(":") > -1) {
                id = name.substring(name.indexOf(":")+1);
                name = name.substring(0, name.indexOf(":"));
            }
            var callback = args.shift();
            if(!$rootScope.hooks.hasOwnProperty(name))
                $rootScope.hooks[name] = [];

            if($rootScope.hooks[name].hasOwnProperty(id)) {
                console.warn("Tried to redefine the same hook twice...");
            }
            else {
                $rootScope.hooks[name][id] = callback;
            }
        };
        $rootScope.removeHook = function(name, id) {
            if(name != null && id != null)
                delete $rootScope.hooks[name][id];
        };
    }).filter('ucfirst', function() {
        return function(input) {
            return (angular.isString(s) && s.length > 0) ? s[0].toUpperCase() + s.substr(1).toLowerCase() : s;
        };
    }).factory('authService', ['$rootScope', '$http', 'Base64', '$location', 'appConstants', 
        function($rootScope, $http, Base64, $location, appConstants) {
        var authToken;
        return {
            login: function(username, password) {
                if(username != "anonymous") {
                    authToken = Base64.encode(username + ':' + password);
                    sessionStorage.setItem('authToken', authToken);
                    $http.defaults.headers.common['Authorization'] = 'Basic ' + authToken;
                }
                var location = sessionStorage.getItem('location') || "status";
                var absUrl = window.location.href.split("login")[0];
                window.location.href = (absUrl + location);
                //window.location.reload();
                //$location.path(location);
            },
            loggedin: function() {
                var token = sessionStorage.getItem('authToken');
                if(token != null && token != "null") {
                    $http.defaults.headers.common['Authorization'] = 'Basic ' + token;
                    if($location.path().indexOf("login") >= 0)
                        $location.path(sessionStorage.getItem('location') || "status");
                }
                else {
                    if(appConstants.init > 0) {
                        if($location.path().indexOf("login") < 0)
                            sessionStorage.setItem('location', $location.path() || "status");
                        $location.path("login");
                    }
                }
            },
            logout: function() {
                sessionStorage.clear();
                $location.path("login");
            }
        };
    }]).factory('Base64', function () {
        var keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
      
        return {
            encode: function (input) {
                var output = "";
                var chr1, chr2, chr3 = "";
                var enc1, enc2, enc3, enc4 = "";
                var i = 0;
      
                do {
                    chr1 = input.charCodeAt(i++);
                    chr2 = input.charCodeAt(i++);
                    chr3 = input.charCodeAt(i++);
      
                    enc1 = chr1 >> 2;
                    enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
                    enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
                    enc4 = chr3 & 63;
      
                    if (isNaN(chr2)) {
                        enc3 = enc4 = 64;
                    } else if (isNaN(chr3)) {
                        enc4 = 64;
                    }
      
                    output = output +
                        keyStr.charAt(enc1) +
                        keyStr.charAt(enc2) +
                        keyStr.charAt(enc3) +
                        keyStr.charAt(enc4);
                    chr1 = chr2 = chr3 = "";
                    enc1 = enc2 = enc3 = enc4 = "";
                } while (i < input.length);
      
                return output;
            },
      
            decode: function (input) {
                var output = "";
                var chr1, chr2, chr3 = "";
                var enc1, enc2, enc3, enc4 = "";
                var i = 0;
      
                // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
                var base64test = /[^A-Za-z0-9\+\/\=]/g;
                if (base64test.exec(input)) {
                    console.error("There were invalid base64 characters in the input text.\n" +
                        "Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='\n" +
                        "Expect errors in decoding.");
                }
                input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
      
                do {
                    enc1 = keyStr.indexOf(input.charAt(i++));
                    enc2 = keyStr.indexOf(input.charAt(i++));
                    enc3 = keyStr.indexOf(input.charAt(i++));
                    enc4 = keyStr.indexOf(input.charAt(i++));
      
                    chr1 = (enc1 << 2) | (enc2 >> 4);
                    chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
                    chr3 = ((enc3 & 3) << 6) | enc4;
      
                    output = output + String.fromCharCode(chr1);
      
                    if (enc3 != 64) {
                        output = output + String.fromCharCode(chr2);
                    }
                    if (enc4 != 64) {
                        output = output + String.fromCharCode(chr3);
                    }
      
                    chr1 = chr2 = chr3 = "";
                    enc1 = enc2 = enc3 = enc4 = "";
      
                } while (i < input.length);
      
                return output;
            }
        };
    }).service('Alert', ['$timeout', 'Session', function($timeout, Session) {
        this.add = function(level, message, non_repeditive) {
            if(non_repeditive === true)
                if(this.checkIfExists(message))
                    return;

            var type;
            switch(level) {
                case "info":
                case 1:
                    type = "fa fa-info";
                break;
                case "warning":
                case 2:
                    type = "fa fa-warning";
                break;
                case "severe":
                case 3:
                    type = "fa fa-times";
                break;
                default:
                    type = "fa fa-info";
                break;
            }
            var list = this.get(true);
            var obj = {
                type: type,
                message: message,
                time: new Date().getTime()
            };
            list.unshift(obj);
            obj.id = list.length;
            Session.set("Alert", list);
            //sessionStorage.setItem("Alert", JSON.stringify(list));
        };
        this.get = function(preserveList) {
            //var list = JSON.parse(sessionStorage.getItem("Alert"));
            var list = Session.get("Alert");
            if(preserveList == undefined) Session.set("Alert", []); //sessionStorage.setItem("Alert", JSON.stringify([])); //Clear after retreival
            return (list != null) ? list : [];
        };
        this.getCount = function() {
            return this.get(true).length || 0;
        };
        this.checkIfExists = function(message) {
            var list = this.get(true);
            if(list.length > 0) {
                for (var i = 0; i < list.length; i++) {
                    if(list[i].message == message) {
                        return true;
                    }
                }
            }
            return false;
        };
    }]).config(['$httpProvider', function($httpProvider) {
        $httpProvider.interceptors.push(['$rootScope', 'appConstants', '$q', '$location', 'Alert', function($rootScope, appConstants, $q, $location, Alert) {
            return {
                responseError: function(rejection) {
                    switch (rejection.status) {
                        case -1:
                            console.log(appConstants.init);
                            sessionStorage.setItem("authToken", null);
                            if(appConstants.init == 1) {
                                if(rejection.config.headers["Authorization"] != undefined) {
                                    Alert.add(1, "Wrong password...", true);
                                }
                            }
                            if(appConstants.init == 2) {
                                if($location.path().indexOf("login") < 0)
                                    sessionStorage.setItem('location', $location.path() || "status");
                                Alert.add(1, "Connection to the server was lost, please reauthenticate!", true);
                            }
                            $location.path("login");
                            break;
                        case 401:
                            var location = $location.path();
                            if(location.indexOf("login") < 0)
                                sessionStorage.setItem('location', location);
                            sessionStorage.setItem('authToken', null);
                            //var deferred = $q.defer();
                            //$rootScope.$broadcast('event:auth-loginRequired', rejection);
                            $location.path("login");
                            //return deferred.promise;
                            break;
                        case 403:
                            //Not modefied
                            //$rootScope.$broadcast('event:auth-forbidden', rejection);
                            break;
                    }
                    // otherwise, default behaviour
                    return $q.reject(rejection);
                }
            };
        }]);
    }]);