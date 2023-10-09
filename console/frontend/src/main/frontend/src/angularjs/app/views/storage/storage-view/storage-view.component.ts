import { StateService } from "@uirouter/angularjs";
import { ApiService } from "src/angularjs/app/services/api.service";
import { SweetAlertService } from "src/angularjs/app/services/sweetalert.service";
import { appModule } from "../../../app.module";
import { Message } from "../storage.component";

class StorageViewController {
  message = { id: this.$state.params["messageId"] };
  metadata?: Message = {
    id: "",
    originalId: "",
    correlationId: "",
    type: "",
    host: "",
    insertDate: 0,
    comment: "Loading...",
    message: "Loading..."
  };

  constructor(
    private Api: ApiService,
    private $state: StateService,
    private SweetAlert: SweetAlertService
  ) { }

  $onInit() {
    this.$state.current.data.breadcrumbs = "Adapter > " + (this.$state.params["storageSource"] == 'pipes' ? "Pipes > " + this.$state.params["storageSourceName"] + " > " : "") + this.$state.params["processState"] + " List > View Message " + this.$state.params["messageId"];
    // @ts-expect-error binding
    this.onCloseNotes();

    if (!this.message.id) {
      this.SweetAlert.Warning("Invalid URL", "No message id provided!");
      return;
    }

    // @ts-expect-error binding
    this.Api.Get(this.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(this.message.id)), (data) => {
      this.metadata = data;
    }, (errorData, statusCode, errorMsg) => {
      let error = (errorData) ? errorData.error : errorMsg;
      if (statusCode == 500) {
        this.SweetAlert.Warning("An error occured while opening the message", "message id [" + this.message.id + "] error [" + error + "]");
      } else {
        this.SweetAlert.Warning("Message not found", "message id [" + this.message.id + "] error [" + error + "]");
      }
      // @ts-expect-error binding
      this.$state.go("pages.storage.list", { adapter: this.adapterName, storageSource: this.storageSource, storageSourceName: this.storageSourceName, processState: this.processState });
    });
  };

  resendMessage(message: Message) {
    // @ts-expect-error binding
    this.onDoResendMessage({
      message: message, callback: (messageId: string) => {
        //Go back to the storage list if successful
        // @ts-expect-error binding
        this.$state.go("pages.storage.list", { adapter: this.adapterName, storageSource: this.storageSource, storageSourceName: this.storageSourceName, processState: this.processState });
      }
    });
  };

  deleteMessage(message: Message) {
    // @ts-expect-error binding
    this.onDoDeleteMessage({
      message: message, callback: (messageId: string) => {
        //Go back to the storage list if successful
        // @ts-expect-error binding
        this.$state.go("pages.storage.list", { adapter: this.adapterName, storageSource: this.storageSource, storageSourceName: this.storageSourceName, processState: this.processState });
      }
    });
  };

  goBack = function () {
    history.back();
  }
};

appModule.component('storageView', {
  bindings: {
    adapterName: '<',
    baseUrl: '<',
    storageSourceName: '<',
    storageSource: '<',
    onCloseNote: '&',
    onCloseNotes: '&',
    onDoDeleteMessage: '&',
    onDoResendMessage: '&',
    onDownloadMessage: '&',
  },
  controller: ['Api', '$state', 'SweetAlert', StorageViewController],
  templateUrl: 'angularjs/app/views/storage/storage-view/storage-view.component.html',
});
