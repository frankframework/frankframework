/**
 * pageTitle - Directive for set Page title - mata title
 */
function pageTitle($rootScope, $timeout) {
    return {
        link: function(scope, element) {
            var listener = function(event, toState, toParams, fromState, fromParams) {
                var title = 'IAF, Ibis AdapterFramework'; // Default title
                if (toState.data && toState.data.pageTitle) title = 'IAF | ' + toState.data.pageTitle;
                $timeout(function() {
                    element.text(title);
                });
            };
            $rootScope.$on('$stateChangeStart', listener);
        }
    };
};

function toDate(dateFilter, appConstants, Hooks) {
    return {
        restrict: 'A',
        scope: {
            time: '@'
        },
        link: function(scope, element, attributes) {
            scope.$watch('time', updateTime);
            function updateTime(time) {
                var toDate = new Date(time - appConstants.timeOffset);
                element.text(dateFilter(toDate, appConstants["console.dateFormat"]));
            }
        }
    };
};

function timeSince(appConstants, $interval) {

    return {
        restrict: 'A',
        scope: {
            time: '@'
        },
        link: function(scope, element, attributes) {
            var timeout = $interval(updateTime, 5000);
            function updateTime() {
                var text = "";
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
                days = Math.floor(days % 7);
                return element.text( days + 'd');
            }
            element.on('$destroy', function() {
                $interval.cancel(timeout);
            });
            setTimeout(function(){
                updateTime();
            }, 50);
        }
    };
};


function quickSubmitForm() {
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
};

/**
 * sideNavigation - Directive for run metsiMenu on sidebar navigation
 */
function sideNavigation($timeout) {
    return {
        restrict: 'A',
        link: function(scope, element) {
            // Call the metisMenu plugin and plug it to sidebar navigation
            $timeout(function(){
                element.metisMenu();

            });
        }
    };
};

/**
 * iboxTools - Directive for iBox tools elements in right corner of ibox
 */
function iboxToolsToggle($timeout) {
    return {
        restrict: 'A',
        scope: true,
        templateUrl: 'views/common/ibox_tools_toggle.html',
        controller: function ($scope, $element) {
            // Function for collapse ibox
            $scope.showhide = function () {
                var ibox = $element.closest('div.ibox');
                var icon = $element.find('i:first');
                var content = ibox.find('div.ibox-content');
                content.slideToggle(200);
                // Toggle icon from up to down
                icon.toggleClass('fa-chevron-up').toggleClass('fa-chevron-down');
                ibox.toggleClass('').toggleClass('border-bottom');
                $timeout(function () {
                    ibox.resize();
                    ibox.find('[id^=map-]').resize();
                }, 50);
            };
        }
    };
}

function iboxExpand($timeout) {
    return {
        restrict: 'A',
        controller: function ($scope, $element) {
            var iboxTitle = $($element.context).find(".ibox-title");
            iboxTitle.bind('dblclick', function() {
                var iboxToolsToggle = iboxTitle.find(".ibox-tools > a");
                if(iboxToolsToggle)
                    iboxToolsToggle.trigger("click");
            });
        }
    };
}

function iboxToolsClose($timeout) {
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
}

/**
 * minimalizaSidebar - Directive for minimalize sidebar
*/
function minimalizaSidebar($timeout) {
    return {
        restrict: 'A',
        template: '<a class="navbar-minimalize minimalize-styl-2 btn btn-primary " href="" ng-click="minimalize()"><i class="fa fa-bars"></i></a>',
        controller: function ($scope, $element) {
            $scope.minimalize = function () {
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
            };
        }
    };
};

/**
 * fitHeight - Directive for set height fit to window height
 */
function fitHeight(){
    return {
        restrict: 'A',
        link: function(scope, element) {
            element.css("height", $(window).height() + "px");
            element.css("min-height", $(window).height() + "px");
        }
    };
}

/**
 * truncate - Directive for truncate string
 */
function truncate($timeout){
    return {
        restrict: 'A',
        scope: {
            truncateOptions: '='
        },
        link: function(scope, element) {
            $timeout(function(){
                element.dotdotdot(scope.truncateOptions);

            });
        }
    };
}

/**
 *
 * Pass all functions into module
 */
angular
    .module('iaf.beheerconsole')
    .directive('toDate', toDate)
    .directive('timeSince', timeSince)
    .directive('pageTitle', pageTitle)
    .directive('quickSubmitForm', quickSubmitForm)
    .directive('sideNavigation', sideNavigation)
    .directive('iboxToolsToggle', iboxToolsToggle)
    .directive('iboxExpand', iboxExpand)
    .directive('iboxToolsClose', iboxToolsClose)
    .directive('minimalizaSidebar', minimalizaSidebar)
    .directive('fitHeight', fitHeight)
    .directive('truncate', truncate);
