import { SessionService } from "src/angularjs/app/services/session.service";
import { appModule } from "../../../app.module";
import { CookiesService } from "src/angularjs/app/services/cookies.service";
import { SweetAlertService } from "src/angularjs/app/services/sweetalert.service";
import { ApiService } from "src/angularjs/app/services/api.service";

class StorageListController {
	selectedMessages: boolean[] = [];
	targetStates = [];

	truncated = false;
	truncateButtonText = "Truncate displayed data";
	filterBoxExpanded = false;

  //@ts-expect-error binding
	resendMessage = this.onDoResendMessage;
  //@ts-expect-error binding
	deleteMessage = this.onDoDeleteMessage;

	messagesResending = false;
	messagesDeleting = false;

	changingProcessState = false;

  search: Record<string, string> = {};
  dtOptions: DataTables.Settings = {};
  searching = false;
  clearSearchLadda = false;
  messagesDownloading = false;
  messagesData: Record<string, any> = {};
  displayColumn: {
    id: boolean,
    insertDate: boolean,
    host: boolean,
    originalId: boolean,
    correlationId: boolean,
    comment: boolean,
    expiryDate: boolean,
    label: boolean,
  } = {
    id: true,
    insertDate: true,
    host: true,
    originalId: true,
    correlationId: true,
    comment: true,
    expiryDate: true,
    label: true,
  }

  constructor(
    private $scope: angular.IScope,
    private Api: ApiService,
    private $compile: angular.ICompileService,
    private Cookies: CookiesService,
    private Session: SessionService,
    private SweetAlert: SweetAlertService
  ) { }

  $onInit() {
    //@ts-expect-error binding
		this.onCloseNotes();
    let searchSession = this.Session.get('search');

		this.search = {
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
		a += '<a ui-sref="pages.storage.view({processState:$ctrl.processState,messageId: message.id })" class="btn btn-info btn-xs" type="button"><i class="fa fa-file-text-o"></i> View</a>';
		a += '<button ng-if="::processState==\'Error\'" ladda="message.resending" data-style="slide-down" title="Resend Message" ng-click="$ctrl.resendMessage({message: message})" class="btn btn-warning btn-xs" type="button"><i class="fa fa-repeat"></i> Resend</button>';
		a += '<button ng-if="::processState==\'Error\'" ladda="message.deleting" data-style="slide-down" title="Delete Message" ng-click="$ctrl.deleteMessage({message: message})" class="btn btn-danger btn-xs" type="button"><i class="fa fa-times"></i> Delete</button>';
		a += '<button title="Download Message" ng-click="$ctrl.onDownloadMessage({messageId: message.id})" class="btn btn-info btn-xs" type="button"><i class="fa fa-arrow-circle-o-down"></i> Download</button>';
		a += '</div';

    var columns: DataTables.ColumnSettings[] = [
      { "data": null, defaultContent: a, className: "m-b-xxs storageActions", orderable: false },
      { "name": "pos", "data": "position", orderable: false, defaultContent: "" },
      { "name": "id", "data": "messageId", orderable: false, defaultContent: "" },
			{ "name": "insertDate", "data": "insertDate", className: "date", defaultContent: "" },
      { "name": "host", "data": "host", orderable: false, defaultContent: "" },
      { "name": "originalId", "data": "originalId", orderable: false, defaultContent: "" },
      { "name": "correlationId", "data": "correlationId", orderable: false, defaultContent: "" },
      { "name": "comment", "data": "comment", orderable: false, defaultContent: "" },
      { "name": "expiryDate", "data": "expiryDate", className: "date", orderable: false, defaultContent: "" },
      { "name": "label", "data": "label", orderable: false, defaultContent: "" },
		];

		this.dtOptions = {
			stateSave: true,
			stateSaveCallback: (settings, data: Record<any, any>) => {
				data["columns"] = columns;
        // @ts-expect-error binding
				this.Session.set('DataTable' + this.processState, data);
			},
      stateLoadCallback: (settings) => {
      // @ts-expect-error binding
				return this.Session.get('DataTable' + this.processState);
			},
			drawCallback: (settings) => {
				// reset visited rows with all draw actions e.g. pagination, filter, search
				this.selectedMessages = [];
				var table = $('#datatable').DataTable();
				var data = table.rows({ page: 'current' }).data();
				// visit rows in the current page once (draw event is fired after rowcallbacks)
				for (var i = 0; i < data.length; i++) {
					this.selectedMessages[data[i].id] = false;
				}
			},
			rowCallback: (row, data: Record<any, any>) => {
				var rowNode = $(row);// .children("td:first").addClass("m-b-xxs");
        rowNode.children("td.date").each((_, element) => {
					var time = $(this).text();
					if (time)
						$(element).attr({ "to-date": "", "time": time });
				});
        this.messagesData[data["id"]] = data;
				this.selectedMessages[data["id"]] = false;
        this.$compile(rowNode as JQuery<HTMLElement>)(this.$scope);
			},
			searching: false,
			scrollX: true,
			// bAutoWidth: false,
			orderCellsTop: true,
			serverSide: true,
			processing: true,
			paging: true,
			lengthMenu: [10, 25, 50, 100, 500, 999],
			order: [[3, 'asc']],
			columns: columns,
			columnDefs: [{
				targets: 0,
				render: (data, type, row) => {
					if (type === 'display') {
						data["messageId"] = data["id"];
						for (let i in data) {
							if (i == "id") continue;
							var columnData = data[i];
							if (typeof columnData == 'string' && columnData.length > 30 && this.truncated) {
								data[i] = '<span title="' + columnData.replace(/"/g, '&quot;') + '">' + columnData.substr(0, 15) + ' &#8230; ' + columnData.substr(-15) + '</span>';
							}
						}
					}
					return data;
				}
			}],
			// sAjaxDataProp: 'messages',
			ajax: (data: Record<any, any>, callback, settings) => {
				var start = data["start"];
				var length = data["length"];
				var order = data["order"][0];
				var direction = order.dir; // asc or desc


        // @ts-expect-error binding
				var url = this.baseUrl + "?max=" + length + "&skip=" + start + "&sort=" + direction;
				let search = this.search;
				let searchSession: Record<keyof typeof search, string> = {};
				for (let column in search) {
					let text = search[column as keyof typeof search];
					if (text) {
						url += "&" + column + "=" + text;
            searchSession[column as keyof typeof search] = text;
					}
				}
				this.Session.set('search', searchSession);
				this.Api.Get(url, (response) => {
					response.draw = data["draw"];
					response.recordsTotal = response.totalMessages;
					this.targetStates = response.targetStates;
					callback(response);
					this.searching = false;
					this.clearSearchLadda = false;
				}, (error) => {
          this.searching = false;
					this.clearSearchLadda = false;
				});
			}
		};

    // @ts-expect-error binding
		var filterCookie = this.Cookies.get(this.processState + "Filter");
		if (filterCookie) {
			for (let column of columns) {
				if (column.name && filterCookie[column.name] === false) {
					column.visible = false;
				}
			}
			this.displayColumn = filterCookie;
		} else {
			this.displayColumn = {
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

		var search = this.search;
		if (search) {
			for (let column in search) {
				let value = search[column as keyof typeof search];
				if (value && value != "") {
					this.filterBoxExpanded = true;
				}
			}
		}
	};

	getFormData() {
		var messageIds: string[] = [];
    for (const i in this.selectedMessages) {
      if (this.selectedMessages[i]) {
				messageIds.push(i);
        this.selectedMessages[i] = false; //unset the messageId
			}
		}

		var fd = new FormData();
		fd.append("messageIds", messageIds as any);
		return fd;
	};

	searchUpdated() {
    this.searching = true;
    // @ts-expect-error binding
    this.onUpdateTable();
	};

	truncate() {
    this.truncated = !this.truncated;
    if (this.truncated) {
      this.truncateButtonText = "Show original";
		} else {
      this.truncateButtonText = "Truncate displayed data";
    }
    // @ts-expect-error binding
    this.onUpdateTable();
	};

	clearSearch() {
    this.clearSearchLadda = true;
		this.Session.remove('search');
    this.search = {};
    // @ts-expect-error binding
    this.onUpdateTable();
	};

	updateFilter(column: string) {
    // @ts-expect-error binding
    this.Cookies.set(this.processState + "Filter", this.displayColumn);

		let table = $('#datatable').DataTable();
		if (table) {
			let tableColumn = table.column(column + ":name");
			if (tableColumn && tableColumn.length == 1)
        tableColumn.visible(this.displayColumn[column as keyof typeof this.displayColumn]);
			table.draw();
		}
	};

	selectAll() {
    for (const i in this.selectedMessages) {
      this.selectedMessages[i] = true;
		}
	};

	unselectAll() {
    for (const i in this.selectedMessages) {
      this.selectedMessages[i] = false;
		}
	};

	resendMessages() {
    let fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesResending = true;
      // @ts-expect-error binding
      this.Api.Post(this.baseUrl, fd, () => {
        this.messagesResending = false;
        // @ts-expect-error binding
        this.onAddNote("success", "Selected messages will be reprocessed");
        // @ts-expect-error binding
        this.onUpdateTable();
			}, (data) => {
        this.messagesResending = false;
        // @ts-expect-error binding
        this.onAddNote("danger", "Something went wrong, unable to resend all messages!");
        // @ts-expect-error binding
        this.onUpdateTable();
			});
		}
	};

	deleteMessages() {
    let fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesDeleting = true;
      // @ts-expect-error binding
      this.Api.Delete(this.baseUrl, fd, () => {
        this.messagesDeleting = false;
        // @ts-expect-error binding
        this.onAddNote("success", "Successfully deleted messages");
        // @ts-expect-error binding
        this.onUpdateTable();
			}, (data) => {
        this.messagesDeleting = false;
        // @ts-expect-error binding
        this.onAddNote("danger", "Something went wrong, unable to delete all messages!");
        // @ts-expect-error binding
        this.onUpdateTable();
			});
		}
	};


	downloadMessages() {
    let fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.messagesDownloading = true;
      // @ts-expect-error binding
      this.Api.Post(this.baseUrl + "/messages/download", fd, (response) => {
				let blob = new Blob([response], { type: 'application/octet-stream' });
				let downloadLink = document.createElement('a');
				downloadLink.href = window.URL.createObjectURL(blob);
				downloadLink.setAttribute('download', 'messages.zip');
				document.body.appendChild(downloadLink);
				downloadLink.click();
        downloadLink.parentNode!.removeChild(downloadLink);
        // @ts-expect-error binding
        this.onAddNote("success", "Successfully downloaded messages");
        this.messagesDownloading = false;
			}, (data) => {
        this.messagesDownloading = false;
        // @ts-expect-error binding
        this.onAddNote("danger", "Something went wrong, unable to download selected messages!");
			}, false, 'blob');
		}
	};

	changeProcessState(processState: string, targetState: string) {
    let fd = this.getFormData();
    if (this.isSelectedMessages(fd)) {
      this.changingProcessState = true;
      // @ts-expect-error binding
      this.Api.Post(this.baseUrl + "/move/" + targetState, fd, () => {
        this.changingProcessState = false;
        // @ts-expect-error binding
        this.onAddNote("success", "Successfully changed the state of messages to " + targetState);
        // @ts-expect-error binding
        this.onUpdateTable();
			}, (data) => {
        this.changingProcessState = false;
        // @ts-expect-error binding
        this.onAddNote("danger", "Something went wrong, unable to move selected messages!");
        // @ts-expect-error binding
        this.onUpdateTable();
			});
		}
	};

	isSelectedMessages(data: FormData) {
		let selectedMessages = data.get("messageIds");
		if (!selectedMessages || selectedMessages.length == 0) {
			this.SweetAlert.Warning("No message selected!");
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
	controller: ['$scope','Api', '$compile', 'Cookies', 'Session', 'SweetAlert', StorageListController],
	templateUrl: 'angularjs/app/views/storage/storage-list/storage-list.component.html',
});
