import { appModule } from "../../../app.module";

appModule.directive('formatCode', ['$location', '$timeout', function ($location, $timeout) {
	return {
		restrict: 'A',
		link: function ($scope, element, attributes) {
			var code = document.createElement('code');
			element.addClass("line-numbers");
			element.addClass("language-markup");
			element.append(code);
			let initHash = $location.hash();

			var watch = $scope.$watch(attributes.formatCode, function (text) {
				if (text && text != '') {
					angular.element(code).text(text);
					Prism.highlightElement(code);

					addOnClickEvent(code);

					// If hash anchor has been set upon init
					$location.hash(initHash);
					let el = angular.element("#" + initHash);
					if (el) {
						el.addClass("line-selected");
						let lineNumber = Math.max(0, parseInt(initHash.substr(1)) - 15);
						$timeout(function () {
							let lineElement = angular.element("#L" + lineNumber)[0];
							if (lineElement) {
								lineElement.scrollIntoView();
							}
						}, 500);
					}
				} else if (text === '') {
					angular.element(code).text(text);
				}
			});

			function addOnClickEvent(root) {
				let spanElements = $(root).children("span.line-numbers-rows").children("span");
				spanElements.on("click", function () { //Update the anchor
					let target = $(event.target);
					target.parent().children(".line-selected").removeClass("line-selected");
					let anchor = target.attr('id');
					target.addClass("line-selected");
					$location.hash(anchor);
				});
			}

			element.on('$destroy', function () {
				watch();
			});
		},
	};
}]);
