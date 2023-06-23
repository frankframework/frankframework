import { appModule } from "../../../app.module";

appModule.controller('FeedbackCtrl', ['$scope', '$uibModalInstance', '$http', 'rating', '$timeout', 'appConstants', 'SweetAlert', function ($scope, $uibModalInstance, $http, rating, $timeout, appConstants, SweetAlert) {
	var URL = appConstants["console.feedbackURL"];
	$scope.form = { rating: rating, name: "", feedback: "" };

	$timeout(function () {
		while (rating >= 0) {
			setRate(rating);
			rating--;
		}
	}, 150);

	$scope.setRating = function (ev, i) {
		resetRating();
		$scope.form.rating = i;
		var j = i;
		while (j >= 0) {
			setRate(j);
			j--;
		}
	};
	function setRate(i) {
		$(".rating i.rating" + i).removeClass("fa-star-o");
		$(".rating i.rating" + i).addClass("fa-star");
	};
	function resetRating() {
		$(".rating i").each(function (i, e) {
			$(e).addClass("fa-star-o").removeClass("fa-star");
		});
	};

	$scope.submit = function (form) {
		form.rating++;
		$http.post(URL, form, { headers: { "Authorization": undefined } }).then(function (response) {
			if (response && response.data && response.data.result && response.data.result == "ok")
				SweetAlert.Success("Thank you for sending us feedback!");
			else
				SweetAlert.Error("Oops, something went wrong...", "Please try again later!");
		}).catch(function (error) {
			SweetAlert.Error("Oops, something went wrong...", "Please try again later!");
		});
		$uibModalInstance.close();
	};

	$scope.close = function () {
		$uibModalInstance.close();
	};
}]);
