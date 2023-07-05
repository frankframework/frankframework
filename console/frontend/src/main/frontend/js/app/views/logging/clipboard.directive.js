import { appModule } from "../../app.module";

appModule.directive('clipboard', function () {
	return {
		restrict: 'A',
		controller: function ($scope, $element, $compile) {
			var selector = angular.element('<i ng-click="copyToClipboard()" title="copy to clipboard" class="fa fa-clipboard" aria-hidden="true"></i>');
			$element.append(selector);
			$element.addClass("clipboard");
			$compile(selector)($scope);

			$scope.copyToClipboard = function () {
				var textToCopy = $element.text().trim();
				if (textToCopy) {
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
});
