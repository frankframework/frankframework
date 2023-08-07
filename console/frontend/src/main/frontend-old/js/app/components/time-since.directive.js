import { appModule } from "../app.module";

appModule.directive('timeSince', ['appConstants', '$interval', function (appConstants, $interval) {
	return {
		restrict: 'A',
		scope: {
			time: '@'
		},
		link: function (scope, element, attributes) {
			function updateTime() {
				if (!attributes.time) return;
				var seconds = Math.round((new Date().getTime() - attributes.time + appConstants.timeOffset) / 1000);

				var minutes = seconds / 60;
				seconds = Math.floor(seconds % 60);
				if (minutes < 1) {
					return element.text(seconds + 's');
				}
				var hours = minutes / 60;
				minutes = Math.floor(minutes % 60);
				if (hours < 1) {
					return element.text(minutes + 'm');
				}
				var days = hours / 24;
				hours = Math.floor(hours % 24);
				if (days < 1) {
					return element.text(hours + 'h');
				}
				days = Math.floor(days);
				return element.text(days + 'd');
			}

			var timeout = $interval(updateTime, 300000);
			element.on('$destroy', function () {
				$interval.cancel(timeout);
			});
			scope.$watch('time', updateTime);
		}
	};
}]);
