import { appModule } from "../../app.module";

appModule.controller('LoggingCtrl', ['$scope', 'Api', 'Misc', '$timeout', '$state', '$stateParams', function ($scope, Api, Misc, $timeout, $state, $stateParams) {
	$scope.viewFile = false;

	var getFileType = function (fileName) {
		if (fileName.indexOf('-stats_') >= 0)
			return 'stats';
		else if (fileName.indexOf('_xml.log') >= 0)
			return 'log4j';
		else if (fileName.indexOf('-stats_') >= 0 || fileName.indexOf('_xml.log') >= 0)
			return 'xml';
		else if (fileName.indexOf('-stats_') < 0 && fileName.indexOf('_xml.log') < 0)
			return 'html';
	};

	var openFile = function (file) {
		var resultType = "";
		var params = "";
		var as = getFileType(file.name);
		switch (as) {
			case "stats":
				resultType = "html";
				params += "&stats=true";
				break;

			case "log4j":
				resultType = "html";
				params += "&log4j=true";

			default:
				resultType = as;
				break;
		}

		var URL = Misc.getServerPath() + "FileViewerServlet?resultType=" + resultType + "&fileName=" + Misc.escapeURL(file.path) + params;
		if (resultType == "xml") {
			window.open(URL, "_blank");
			return;
		}

		$scope.viewFile = URL;
		$timeout(function () {
			var iframe = angular.element("iframe");

			iframe[0].onload = function () {
				var iframeBody = $(iframe[0].contentWindow.document.body);
				iframe.css({ "height": iframeBody.height() + 50 });
			};
		});
	};

	$scope.closeFile = function () {
		$scope.viewFile = false;
		$state.transitionTo('pages.logging_show', { directory: $scope.directory });
	};

	$scope.download = function (file) {
		var url = Misc.getServerPath() + "FileViewerServlet?resultType=bin&fileName=" + Misc.escapeURL(file.path);
		window.open(url, "_blank");
	};

	$scope.alert = false;
	var openDirectory = function (directory) {
		var url = "logging";
		if (directory) {
			url = "logging?directory=" + directory;
		}

		Api.Get(url, function (data) {
			$scope.alert = false;
			$.extend($scope, data);
			$scope.path = data.directory;
			if (data.count > data.list.length) {
				$scope.alert = "Total number of items [" + data.count + "] exceeded maximum number, only showing first [" + (data.list.length - 1) + "] items!";
			}
		}, function (data) {
			$scope.alert = (data) ? data.error : "An unknown error occured!";
		}, false);
	};

	$scope.open = function (file) {
		if (file.type == "directory") {
			$state.transitionTo('pages.logging_show', { directory: file.path });
		} else {
			$state.transitionTo('pages.logging_show', { directory: $scope.directory, file: file.name }, { notify: false, reload: false });
		}
	};

	//This is only false when the user opens the logging page
	var directory = ($stateParams.directory && $stateParams.directory.length > 0) ? $stateParams.directory : false;
	//The file param is only set when the user copy pastes an url in their browser
	if ($stateParams.file && $stateParams.file.length > 0) {
		var file = $stateParams.file;

		$scope.directory = directory;
		$scope.path = directory + "/" + file;
		openFile({ path: directory + "/" + file, name: file });
	}
	else {
		openDirectory(directory);
	}
}]);
