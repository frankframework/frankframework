import { appModule } from "../app.module";

appModule.directive('toDate', ['dateFilter', 'appConstants', function (dateFilter, appConstants) {
	return {
		restrict: 'A',
		scope: {
			time: '@'
		},
		link: function (scope, element, attributes) {
			var watch = scope.$watch('time', updateTime);
			function updateTime(time) {
				if (!time) return;

				if (isNaN(time))
					time = new Date(time).getTime();
				var toDate = new Date(time - appConstants.timeOffset);
				element.text(dateFilter(toDate, appConstants["console.dateFormat"]));
				watch();
			}
		}
	};
}]);
