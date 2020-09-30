angular.module('iaf.beheerconsole')

.directive('pageTitle', ['$rootScope', '$timeout', '$state', '$transitions', 'Debug', function($rootScope, $timeout, $state, $transitions, Debug) {
	return {
		link: function(scope, element) {
			var listener = function() {
				var toState = $state.current;
				Debug.info("state change", toState);

				var title = 'Loading...'; // Default title
				if (toState.data && toState.data.pageTitle && $rootScope.instanceName) title = $rootScope.dtapStage +'-'+$rootScope.instanceName+' | '+toState.data.pageTitle;
				else if($rootScope.startupError) title = "ERROR";
				$timeout(function() {
					element.text(title);
				});
			};
			$transitions.onSuccess({}, listener); //Fired on every state change
			$rootScope.$watch('::instanceName', listener); //Fired once, once the instance name is known.
		}
	};
}])

.directive('toDate', ['dateFilter', 'appConstants', function(dateFilter, appConstants) {
	return {
		restrict: 'A',
		scope: {
			time: '@'
		},
		link: function(scope, element, attributes) {
			var watch = scope.$watch('time', updateTime);
			function updateTime(time) {
				if(!time) return;

				if(isNaN(time))
					time = new Date(time).getTime();
				var toDate = new Date(time - appConstants.timeOffset);
				element.text(dateFilter(toDate, appConstants["console.dateFormat"]));
				watch();
			}
		}
	};
}])

.directive('clipboard', function() {
	return {
		restrict: 'A',
		controller: function ($scope, $element, $compile) {
			var selector = angular.element('<i ng-click="copyToClipboard()" title="copy to clipboard" class="fa fa-clipboard" aria-hidden="true"></i>');
			$element.append(selector);
			$element.addClass("clipboard");
			$compile(selector)($scope);

			$scope.copyToClipboard = function () {
				var textToCopy = $element.text().trim();
				if(textToCopy) {
					var el = document.createElement('textarea');
					el.value = textToCopy;
					el.setAttribute('readonly', '');
					el.style.position = 'absolute';
					el.style.left = '-9999px';
					document.body.appendChild(el);
					el.select();
					document.execCommand('copy');
					document.body.removeChild(el);
				}
			};
		}
	};
})

.directive('timeSince', ['appConstants', '$interval', function(appConstants, $interval) {
	return {
		restrict: 'A',
		scope: {
			time: '@'
		},
		link: function(scope, element, attributes) {
			function updateTime() {
				if(!attributes.time) return;
				var seconds = Math.round((new Date().getTime() - attributes.time + appConstants.timeOffset) / 1000);

				var minutes = seconds / 60;
				seconds = Math.floor(seconds % 60);
				if (minutes < 1) {
					return element.text( seconds + 's');
				}
				var hours = minutes / 60;
				minutes = Math.floor(minutes % 60);
				if (hours < 1) {
					return element.text( minutes + 'm');
				}
				var days = hours / 24;
				hours = Math.floor(hours % 24);
				if (days < 1) {
					return element.text( hours + 'h');
				}
				days = Math.floor(days);
				return element.text( days + 'd');
			}

			var timeout = $interval(updateTime, 300000);
			element.on('$destroy', function() {
				$interval.cancel(timeout);
			});
			scope.$watch('time', updateTime);
		}
	};
}])

.directive('quickSubmitForm', function() {
	return {
		restrict: 'A',
		link: function(scope, element, attributes) {
			var map = Array();
			element.bind("keydown keyup", function (event) {
				if(event.which == 13 || event.which == 17)
					map[event.keyCode] = event.type == 'keydown';
				if(map[13] && map[17]) {
					scope.$apply(function (){
						scope.$eval(attributes.quickSubmitForm);
					});
				}
			});
		}
	};
})

.directive('sideNavigation', ['$timeout', function($timeout) {
	return {
		restrict: 'A',
		link: function(scope, element) {
			// Call the metisMenu plugin and plug it to sidebar navigation
			$timeout(function(){
				element.metisMenu();

			});
		}
	};
}])

.directive('customViews', ['appConstants', function(appConstants) {
	return {
		restrict: 'E',
		replace: true,
		link: function(scope, element, attributes) {
			scope.customViews = [];
			scope.$on('appConstants', function() {
				var customViews = appConstants["customViews.names"];
				if(customViews == undefined)
					return;

				if(customViews.length > 0) {
					var views = customViews.split(",");
					for(i in views) {
						var viewId = views[i];
						var name = appConstants["customViews."+viewId+".name"];
						var url =  appConstants["customViews."+viewId+".url"];
						if(name && url)
							scope.customViews.push({
								view: viewId,
								name: name,
								url: url
							});
					}
				}
			});
		},
		template: '<li ng-repeat="view in customViews" ui-sref-active="active">'+
		'<a ui-sref="pages.customView(view)"><i class="fa fa-desktop"></i> <span class="nav-label">{{view.name}}</span></a>' +
		'</li>'
	};
}])

.directive('iboxToolsClose', ['$timeout', function($timeout) {
	return {
		restrict: 'A',
		scope: true,
		templateUrl: 'views/common/ibox_tools_close.html',
		controller: function ($scope, $element) {
			$scope.closebox = function () {
				var ibox = $element.closest('div.ibox');
				ibox.remove();
			};
		}
	};
}])

.service('Sidebar', function() {
	this.toggle = function() {
		$("body").toggleClass("mini-navbar");
		if (!$('body').hasClass('mini-navbar') || $('body').hasClass('body-small')) {
			// Hide menu in order to smoothly turn on when maximize menu
			$('#side-menu').hide();
			// For smoothly turn on menu
			setTimeout(
				function () {
					$('#side-menu').fadeIn(400);
				}, 200);
		} else if ($('body').hasClass('fixed-sidebar')){
			$('#side-menu').hide();
			setTimeout(
				function () {
					$('#side-menu').fadeIn(400);
				}, 100);
		} else {
			// Remove all inline style from jquery fadeIn function to reset menu state
			$('#side-menu').removeAttr('style');
		}
	}
})

.directive('minimalizaSidebar', ['Sidebar', function(Sidebar) {
	return {
		restrict: 'A',
		template: '<a class="navbar-minimalize minimalize" href="" ng-click="toggleSidebar()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>',
		controller: function ($scope, $element) {
			$scope.toggleSidebar = function () { Sidebar.toggle() };
		}
	};
}])

.directive('hamburger', ['Sidebar', function(Sidebar) {
	return {
		restrict: 'A',
		template: '<a class="hamburger btn btn-primary " href="" ng-click="toggleSidebar()"><i class="fa fa-bars"></i></a>',
		controller: function ($scope, $element) {
			$scope.toggleSidebar = function () { Sidebar.toggle() };
		}
	};
}])

.directive('fitHeight', function() {
	return {
		restrict: 'A',
		link: function($scope, element) {
			$scope.height = {
				topnavbar: 0,
				topinfobar: 0,
				window: 0,
				min: 800
			};

			function fitHeight() {
				var offset = $scope.height.topnavbar + $scope.height.topinfobar;
				var height = ($scope.height.window > $scope.height.min ? $scope.height.window : $scope.height.min) - offset;
				element.css("height", height + "px");
				element.css("min-height", height + "px");
			}

			$scope.$watch(function(){ return $(window).height();}, function(newValue){
				if(!newValue) return;
				$scope.height.window = newValue;
				fitHeight();
			});
			$scope.$watch(function(){ return $('nav.navbar-default').height();}, function(newValue){
				if(!newValue) return;
				$scope.height.min = newValue;
				fitHeight();
			});
			$scope.$watch(function(){ return $('.topnavbar').height();}, function(newValue){
				if(!newValue) return;
				$scope.height.topnavbar = newValue;
				fitHeight();
			});
			$scope.$watch(function(){ return $('.topinfobar').height();}, function(newValue){
				if(!newValue) return;
				$scope.height.topinfobar = newValue;
				fitHeight();
			});

			fitHeight();
		}
	};
})

.directive('scrollToTop', function() {
	return {
		restrict: 'A',
		replace: true,
		template: '<div class="scroll-to-top"><a title="Scroll to top" ng-click="scrollTop()"><i class="fa fa-arrow-up"></i> <span class="nav-label">Scroll To Top</span></a></div>',
		controller: function ($scope) {
			$scope.scrollTop = function() {
				$(window).scrollTop(0);
			};
		}
	};
})

.directive('generalDataProtectionRegulation', ['$uibModal', 'GDPR', 'appConstants', '$rootScope', function($uibModal, GDPR, appConstants, $rootScope) {
	return {
		restrict: 'A',
		require: 'icheck',
		templateUrl: 'views/common/cookie.html',
		controller: function ($scope) {
			$scope.bottomNotification = false;
			$scope.cookies = GDPR.defaults;

			$rootScope.$on('appConstants', function() {
				$scope.cookies = {
						necessary: true,
						personalization: appConstants.getBoolean("console.cookies.personalization", true),
						analytical: appConstants.getBoolean("console.cookies.analytical", true),
						functional: appConstants.getBoolean("console.cookies.functional", true)
				};

				$scope.bottomNotification = GDPR.showCookie();
			});

			$scope.savePreferences = function(cookies) {
				GDPR.setSettings(cookies);
				$scope.bottomNotification = false;
			};
			$scope.consentAllCookies = function() {
				$scope.savePreferences({
					necessary: true,
					personalization: true,
					analytical: true,
					functional: true
				});
			};

			$scope.openModal = function() {
				$scope.bottomNotification = false;

				$uibModal.open({
					templateUrl: 'views/common/cookieModal.html',
					size: 'lg',
					backdrop: 'static',
					controller: function($uibModalInstance) {
						$scope.savePreferences = function(cookies) {
							GDPR.setSettings(cookies);
							$uibModalInstance.close();
						};
					}
				});
			};
		}
	};
}])

.directive('icheck', ['$timeout', '$parse', function($timeout, $parse) {
	return {
		restrict: 'A',
		require: 'ngModel',
		link: function($scope, element, $attrs, ngModel) {
			return $timeout(function() {
				var value = $attrs['value'];

				$scope.$watch($attrs['ngModel'], function(newValue) {
					$(element).iCheck('update');
				});

				return $(element).iCheck({
					checkboxClass: 'icheckbox_square-green',
					radioClass: 'iradio_square-green'

				}).on('ifChanged', function(event) {
						if ($(element).attr('type') === 'checkbox' && $attrs['ngModel']) {
							$scope.$apply(function() {
								return ngModel.$setViewValue(event.target.checked);
							});
						}
						if ($(element).attr('type') === 'radio' && $attrs['ngModel']) {
							return $scope.$apply(function() {
								return ngModel.$setViewValue(value);
							});
						}
					});
			});
		}
	};
}])

.directive('icheckRadius', ['$timeout', '$parse', function($timeout, $parse) {
	return {
		restrict: 'A',
		require: 'ngModel',
		link: function($scope, element, $attrs, ngModel) {
			return $timeout(function() {

				$scope.$watch($attrs['ngModel'], function(newValue) {
					$(element).iCheck('update');
				});

				return $(element).iCheck({
					checkboxClass: 'iradio_square-green',
				}).on('ifChanged', function(event) {
						if ($(element).attr('type') === 'checkbox' && $attrs['ngModel']) {
							$scope.$apply(function() {
								return ngModel.$setViewValue(event.target.checked);
							});
						}
					});
			});
		}
	};
}]);