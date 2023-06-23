import { appModule } from "../../../app.module";

appModule.directive('quickSubmitForm', function () {
	return {
		restrict: 'A',
		link: function (scope, element, attributes) {
			var map = Array();
			element.bind("keydown keyup", function (event) {
				if (event.which == 13 || event.which == 17)
					map[event.keyCode] = event.type == 'keydown';
				if (map[13] && map[17]) {
					scope.$apply(function () {
						scope.$eval(attributes.quickSubmitForm);
					});
				}
			});
		}
	};
});
