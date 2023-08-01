import { appModule } from "../../../app.module";

const StorageListController = function ($scope, Api, $compile, Cookies, Session, SweetAlert) {
	const ctrl = this;

	ctrl.selectedMessages = [];
	ctrl.targetStates = [];

	ctrl.truncated = false;
	ctrl.truncateButtonText = "Truncate displayed data";
	ctrl.filterBoxExpanded = false;

	ctrl.resendMessage = ctrl.onDoResendMessage;
	ctrl.deleteMessage = ctrl.onDoDeleteMessage;

	ctrl.messagesResending = false;
	ctrl.messagesDeleting = false;

	ctrl.changingProcessState = false;

	ctrl.$onInit = function () {
		ctrl.onCloseNotes();
		let searchSession = Session.get('search');

		ctrl.search = {
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

		var a = '';

		a += '<input icheck type="checkbox" ng-model="$ctrl.selectedMessages[message.id]"/>';
		a += '<div ng-show="!$ctrl.selectedMessages[message.id]">';
		a += '<a ui-sref="pages.storage.view({adapter:adapterName,receiver:receiverName,processState:processState,messageId: message.id })" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
		a += '<button ng-if="::processState==\'Error\'" ladda="message.resending" data-style="slide-down" title="Resend Message" ng-click="$ctrl.resendMessage({message: message})" class="btn btn-warning btn-xs" type="button"><i class="fa fa-repeat"></i> Resend</button>';
		a += '<button ng-if="::processState==\'Error\'" ladda="message.deleting" data-style="slide-down" title="Delete Message" ng-click="$ctrl.deleteMessage({message: message})" class="btn btn-danger btn-xs" type="button"><i class="fa fa-times"></i> Delete</button>';
		a += '<button title="Download Message" ng-click="$ctrl.onDownloadMessage({messageId: message.id})" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';
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

		ctrl.dtOptions = {
			stateSave: true,
			stateSaveCallback: function (settings, data) {
				data.columns = columns;
				Session.set('DataTable' + ctrl.processState, data);
			},
			stateLoadCallback: function (settings) {
				return Session.get('DataTable' + ctrl.processState);
			},
			drawCallback: function (settings) {
				// reset visited rows with all draw actions e.g. pagination, filter, search
				ctrl.selectedMessages = [];
				var table = $('#datatable').DataTable();
				var data = table.rows({ page: 'current' }).data();
				// visit rows in the current page once (draw event is fired after rowcallbacks)
				for (var i = 0; i < data.length; i++) {
					ctrl.selectedMessages[data[i].id] = false;
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
				ctrl.selectedMessages[data.id] = false;
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
							if (typeof columnData == 'string' && columnData.length > 30 && ctrl.truncated) {
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

				var url = ctrl.baseUrl + "?max=" + length + "&skip=" + start + "&sort=" + direction;
				let search = ctrl.search;
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
					ctrl.targetStates = response.targetStates;
					callback(response);
					ctrl.searching = false;
					ctrl.clearSearchLadda = false;
				}, function (error) {
					ctrl.searching = false;
					ctrl.clearSearchLadda = false;
				});
			}
		};

		var filterCookie = Cookies.get(ctrl.processState + "Filter");
		if (filterCookie) {
			for (let column in columns) {
				if (column.name && filterCookie[column.name] === false) {
					column.visible = false;
				}
			}
			ctrl.displayColumn = filterCookie;
		} else {
			ctrl.displayColumn = {
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

		var search = ctrl.search;
		if (search) {
			for (let column in search) {
				let value = search[column];
				if (value && value != "") {
					ctrl.filterBoxExpanded = true;
				}
			}
		}
	};

	ctrl.getFormData = function () {
		var messageIds = [];
		for (const i in ctrl.selectedMessages) {
			if (ctrl.selectedMessages[i]) {
				messageIds.push(i);
				ctrl.selectedMessages[i] = false; //unset the messageId
			}
		}

		var fd = new FormData();
		fd.append("messageIds", messageIds);
		return fd;
	};

	ctrl.searchUpdated = function () {
		ctrl.searching = true;
		ctrl.onUpdateTable();
	};

	ctrl.truncate = function () {
		ctrl.truncated = !ctrl.truncated;
		if (ctrl.truncated) {
			ctrl.truncateButtonText = "Show original";
		} else {
			ctrl.truncateButtonText = "Truncate displayed data";
		}
		ctrl.onUpdateTable();
	};

	ctrl.clearSearch = function () {
		ctrl.clearSearchLadda = true;
		Session.remove('search');
		ctrl.search = {};
		ctrl.onUpdateTable();
	};

	ctrl.updateFilter = function (column) {
		Cookies.set(ctrl.processState + "Filter", ctrl.displayColumn);

		let table = $('#datatable').DataTable();
		if (table) {
			let tableColumn = table.column(column + ":name");
			if (tableColumn && tableColumn.length == 1)
				tableColumn.visible(ctrl.displayColumn[column]);
			table.draw();
		}
	};

	ctrl.selectAll = function () {
		for (const i in ctrl.selectedMessages) {
			ctrl.selectedMessages[i] = true;
		}
	};

	ctrl.unselectAll = function () {
		for (const i in ctrl.selectedMessages) {
			ctrl.selectedMessages[i] = false;
		}
	};

	ctrl.resendMessages = function () {
		let fd = getFormData();
		if (ctrl.isSelectedMessages(fd)) {
			ctrl.messagesResending = true;
			Api.Post(ctrl.baseUrl, fd, function () {
				ctrl.messagesResending = false;
				ctrl.onAddNote("success", "Selected messages will be reprocessed");
				ctrl.onUpdateTable();
			}, function (data) {
				ctrl.messagesResending = false;
				ctrl.onAddNote("danger", "Something went wrong, unable to resend all messages!");
				ctrl.onUpdateTable();
			});
		}
	};

	ctrl.deleteMessages = function () {
		let fd = getFormData();
		if (ctrl.isSelectedMessages(fd)) {
			ctrl.messagesDeleting = true;
			Api.Delete(ctrl.baseUrl, fd, function () {
				ctrl.messagesDeleting = false;
				ctrl.onAddNote("success", "Successfully deleted messages");
				ctrl.onUpdateTable();
			}, function (data) {
				ctrl.messagesDeleting = false;
				ctrl.onAddNote("danger", "Something went wrong, unable to delete all messages!");
				ctrl.onUpdateTable();
			});
		}
	};


	ctrl.downloadMessages = function () {
		let fd = getFormData();
		if (ctrl.isSelectedMessages(fd)) {
			ctrl.messagesDownloading = true;
			Api.Post(ctrl.baseUrl + "/messages/download", fd, function (response) {
				let blob = new Blob([response], { type: 'application/octet-stream' });
				let downloadLink = document.createElement('a');
				downloadLink.href = window.URL.createObjectURL(blob);
				downloadLink.setAttribute('download', 'messages.zip');
				document.body.appendChild(downloadLink);
				downloadLink.click();
				downloadLink.parentNode.removeChild(downloadLink);
				ctrl.onAddNote("success", "Successfully downloaded messages");
				ctrl.messagesDownloading = false;
			}, function (data) {
				ctrl.messagesDownloading = false;
				ctrl.onAddNote("danger", "Something went wrong, unable to download selected messages!");
			}, null, 'blob');
		}
	};

	ctrl.changeProcessState = function (processState, targetState) {
		let fd = getFormData();
		if (ctrl.isSelectedMessages(fd)) {
			ctrl.changingProcessState = true;
			Api.Post(ctrl.baseUrl + "/move/" + targetState, fd, function () {
				ctrl.changingProcessState = false;
				ctrl.onAddNote("success", "Successfully changed the state of messages to " + targetState);
				ctrl.onUpdateTable();
			}, function (data) {
				ctrl.changingProcessState = false;
				ctrl.onAddNote("danger", "Something went wrong, unable to move selected messages!");
				ctrl.onUpdateTable();
			});
		}
	};

	ctrl.isSelectedMessages = function (data) {
		let selectedMessages = data.get("messageIds");
		if (!selectedMessages || selectedMessages.length == 0) {
			SweetAlert.Warning("No message selected!");
			return false;
		} else {
			return true;
		}
	};
};

appModule.component('storageList', {
	bindings: {
		adapterName: '<',
		baseUrl: '<',
		storageSourceName: '<',
		storageSource: '<',
		processState: '<',
		onAddNote: '&',
		onCloseNote: '&',
		onCloseNotes: '&',
		onUpdateTable: '&',
		onDoDeleteMessage: '&',
		onDownloadMessage: '&',
		onDoResendMessage: '&',
	},
	controller: ['$scope', 'Api', '$compile', 'Cookies', 'Session', 'SweetAlert', StorageListController],
	templateUrl: 'js/app/views/storage/storage-list/storage-list.component.html',
});
