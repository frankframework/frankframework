import { appModule } from "../../../app.module";

appModule.controller('AdapterStorageCtrl', ['$scope', 'Api', '$compile', 'Cookies', 'Session', 'SweetAlert', function ($scope, Api, $compile, Cookies, Session, SweetAlert) {
	$scope.closeNotes();
	$scope.selectedMessages = [];
	$scope.targetStates = [];
	var a = '';

	a += '<input icheck type="checkbox" ng-model="selectedMessages[message.id]"/>';
	a += '<div ng-show="!selectedMessages[message.id]">';
	a += '<a ui-sref="pages.storage.view({adapter:adapterName,receiver:receiverName,processState:processState,messageId: message.id })" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
	a += '<button ng-if="::processState==\'Error\'" ladda="message.resending" data-style="slide-down" title="Resend Message" ng-click="resendMessage(message)" class="btn btn-warning btn-xs" type="button"><i class="fa fa-repeat"></i> Resend</button>';
	a += '<button ng-if="::processState==\'Error\'" ladda="message.deleting" data-style="slide-down" title="Delete Message" ng-click="deleteMessage(message)" class="btn btn-danger btn-xs" type="button"><i class="fa fa-times"></i> Delete</button>';
	a += '<button title="Download Message" ng-click="downloadMessage(message.id)" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';
	a += '</div';

	var columns = [
		{ "data": null, defaultContent: a, className: "m-b-xxs storageActions", bSortable: false },
		{ "name": "pos", "data": "position", bSortable: false, defaultContent: "" },
		{ "name": "id", "data": "messageId", bSortable: false, defaultContent: "" },
		{ "name": "insertDate", "data": "insertDate", className: "date", defaultContent: "" },
		{ "name": "host", "data": "host", bSortable: false, defaultContent: "" },
		{ "name": "originalId", "data": "originalId", bSortable: false, defaultContent: "" },
		{ "name": "correlationId", "data": "correlationId", bSortable: false, defaultContent: "" },
		{ "name": "comment", "data": "comment", bSortable: false, defaultContent: "" },
		{ "name": "expiryDate", "data": "expiryDate", className: "date", bSortable: false, defaultContent: "" },
		{ "name": "label", "data": "label", bSortable: false, defaultContent: "" },
	];
	var filterCookie = Cookies.get($scope.processState + "Filter");
	if (filterCookie) {
		for (let column in columns) {
			if (column.name && filterCookie[column.name] === false) {
				column.visible = false;
			}
		}
		$scope.displayColumn = filterCookie;
	} else {
		$scope.displayColumn = {
			id: true,
			insertDate: true,
			host: true,
			originalId: true,
			correlationId: true,
			comment: true,
			expiryDate: true,
			label: true,
		}
	}

	$scope.searchUpdated = function () {
		$scope.searching = true;
		$scope.updateTable();
	};

	$scope.truncated = false;
	$scope.truncateButtonText = "Truncate displayed data";
	$scope.truncate = function () {
		$scope.truncated = !$scope.truncated;
		if ($scope.truncated) {
			$scope.truncateButtonText = "Show original";
		} else {
			$scope.truncateButtonText = "Truncate displayed data";
		}
		$scope.updateTable();
	};

	$scope.dtOptions = {
		stateSave: true,
		stateSaveCallback: function (settings, data) {
			data.columns = columns;
			Session.set('DataTable' + $scope.processState, data);
		},
		stateLoadCallback: function (settings) {
			return Session.get('DataTable' + $scope.processState);
		},
		drawCallback: function (settings) {
			// reset visited rows with all draw actions e.g. pagination, filter, search
			$scope.selectedMessages = [];
			var table = $('#datatable').DataTable();
			var data = table.rows({ page: 'current' }).data();
			// visit rows in the current page once (draw event is fired after rowcallbacks)
			for (var i = 0; i < data.length; i++) {
				$scope.selectedMessages[data[i].id] = false;
			}
		},
		rowCallback: function (row, data) {
			var row = $(row);// .children("td:first").addClass("m-b-xxs");
			row.children("td.date").each(function (_, element) {
				var time = $(this).text();
				if (time)
					$(element).attr({ "to-date": "", "time": time });
			});
			var scope = $scope.$new();
			scope.message = data;
			$scope.selectedMessages[data.id] = false;
			$compile(row)(scope);
		},
		searching: false,
		scrollX: true,
		bAutoWidth: false,
		orderCellsTop: true,
		serverSide: true,
		processing: true,
		paging: true,
		lengthMenu: [10, 25, 50, 100, 500, 999],
		order: [[3, 'asc']],
		columns: columns,
		columnDefs: [{
			targets: 0,
			render: function (data, type, row) {
				if (type === 'display') {
					data["messageId"] = data["id"];
					for (let i in data) {
						if (i == "id") continue;
						var columnData = data[i];
						if (typeof columnData == 'string' && columnData.length > 30 && $scope.truncated) {
							data[i] = '<span title="' + columnData.replace(/"/g, '&quot;') + '">' + columnData.substr(0, 15) + ' &#8230; ' + columnData.substr(-15) + '</span>';
						}
					}
				}
				return data;
			}
		}],
		sAjaxDataProp: 'messages',
		ajax: function (data, callback, settings) {
			var start = data.start;
			var length = data.length;
			var order = data.order[0];
			var direction = order.dir; // asc or desc

			var url = $scope.base_url + "?max=" + length + "&skip=" + start + "&sort=" + direction;
			let search = $scope.search;
			let searchSession = {};
			for (let column in search) {
				let text = search[column];
				if (text) {
					url += "&" + column + "=" + text;
					searchSession[column] = text;
				}
			}
			Session.set('search', searchSession);
			Api.Get(url, function (response) {
				response.draw = data.draw;
				response.recordsTotal = response.totalMessages;
				$scope.targetStates = response.targetStates;
				callback(response);
				$scope.searching = false;
				$scope.clearSearchLadda = false;
			}, function (error) {
				$scope.searching = false;
				$scope.clearSearchLadda = false;
			});
		}
	};

	let searchSession = Session.get('search');
	$scope.search = {
		id: searchSession ? searchSession['id'] : "",
		startDate: searchSession ? searchSession["startDate"] : "",
		endDate: searchSession ? searchSession["endDate"] : "",
		host: searchSession ? searchSession["host"] : "",
		messageId: searchSession ? searchSession["messageId"] : "",
		correlationId: searchSession ? searchSession["correlationId"] : "",
		comment: searchSession ? searchSession["comment"] : "",
		label: searchSession ? searchSession["label"] : "",
		message: searchSession ? searchSession["message"] : ""
	};

	$scope.clearSearch = function () {
		$scope.clearSearchLadda = true;
		Session.remove('search');
		$scope.search = {};
		$scope.updateTable();
	};

	$scope.filterBoxExpanded = false;
	var search = $scope.search;
	if (search) {
		for (let column in search) {
			let value = search[column];
			if (value && value != "") {
				$scope.filterBoxExpanded = true;
			}
		}
	}

	$scope.updateFilter = function (column) {
		Cookies.set($scope.processState + "Filter", $scope.displayColumn);

		let table = $('#datatable').DataTable();
		if (table) {
			let tableColumn = table.column(column + ":name");
			if (tableColumn && tableColumn.length == 1)
				tableColumn.visible($scope.displayColumn[column]);
			table.draw();
		}
	}

	$scope.resendMessage = $scope.doResendMessage;
	$scope.deleteMessage = $scope.doDeleteMessage;

	$scope.selectAll = function () {
		for (const i in $scope.selectedMessages) {
			$scope.selectedMessages[i] = true;
		}
	}
	$scope.unselectAll = function () {
		for (const i in $scope.selectedMessages) {
			$scope.selectedMessages[i] = false;
		}
	}

	$scope.messagesResending = false;
	$scope.messagesDeleting = false;
	function getFormData() {
		var messageIds = [];
		for (const i in $scope.selectedMessages) {
			if ($scope.selectedMessages[i]) {
				messageIds.push(i);
				$scope.selectedMessages[i] = false;//unset the messageId
			}
		}

		var fd = new FormData();
		fd.append("messageIds", messageIds);
		return fd;
	}
	$scope.resendMessages = function () {
		let fd = getFormData();
		if ($scope.isSelectedMessages(fd)) {
			$scope.messagesResending = true;
			Api.Post($scope.base_url, fd, function () {
				$scope.messagesResending = false;
				$scope.addNote("success", "Selected messages will be reprocessed");
				$scope.updateTable();
			}, function (data) {
				$scope.messagesResending = false;
				$scope.addNote("danger", "Something went wrong, unable to resend all messages!");
				$scope.updateTable();
			});
		}
	}
	$scope.deleteMessages = function () {
		let fd = getFormData();
		if ($scope.isSelectedMessages(fd)) {
			$scope.messagesDeleting = true;
			Api.Delete($scope.base_url, fd, function () {
				$scope.messagesDeleting = false;
				$scope.addNote("success", "Successfully deleted messages");
				$scope.updateTable();
			}, function (data) {
				$scope.messagesDeleting = false;
				$scope.addNote("danger", "Something went wrong, unable to delete all messages!");
				$scope.updateTable();
			});
		}
	}

	$scope.downloadMessages = function () {
		let fd = getFormData();
		if ($scope.isSelectedMessages(fd)) {
			$scope.messagesDownloading = true;
			Api.Post($scope.base_url + "/messages/download", fd, function (response) {
				let blob = new Blob([response], { type: 'application/octet-stream' });
				let downloadLink = document.createElement('a');
				downloadLink.href = window.URL.createObjectURL(blob);
				downloadLink.setAttribute('download', 'messages.zip');
				document.body.appendChild(downloadLink);
				downloadLink.click();
				downloadLink.parentNode.removeChild(downloadLink);
				$scope.addNote("success", "Successfully downloaded messages");
				$scope.messagesDownloading = false;
			}, function (data) {
				$scope.messagesDownloading = false;
				$scope.addNote("danger", "Something went wrong, unable to download selected messages!");
			}, null, 'blob');
		}
	}

	$scope.changingProcessState = false;
	$scope.changeProcessState = function (processState, targetState) {
		let fd = getFormData();
		if ($scope.isSelectedMessages(fd)) {
			$scope.changingProcessState = true;
			Api.Post($scope.base_url + "/move/" + targetState, fd, function () {
				$scope.changingProcessState = false;
				$scope.addNote("success", "Successfully changed the state of messages to " + targetState);
				$scope.updateTable();
			}, function (data) {
				$scope.changingProcessState = false;
				$scope.addNote("danger", "Something went wrong, unable to move selected messages!");
				$scope.updateTable();
			});
		}
	}

	$scope.isSelectedMessages = function (data) {
		let selectedMessages = data.get("messageIds");
		if (!selectedMessages || selectedMessages.length == 0) {
			SweetAlert.Warning("No message selected!");
			return false;
		} else {
			return true;
		}
	};
}]);
