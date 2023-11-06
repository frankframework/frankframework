import { Component, OnInit } from '@angular/core';
import { Message, StorageService, PartialMessage } from '../storage.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';

@Component({
  selector: 'app-storage-view',
  templateUrl: './storage-view.component.html',
  styleUrls: ['./storage-view.component.scss']
})
export class StorageViewComponent implements OnInit {
  message: PartialMessage = {
    id: '0',//this.$state.params["messageId"],
    resending: false,
    deleting: false
  };
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

  // service bindings
  storageParams = this.storageService.storageParams;
  closeNote = (index: number) => { this.storageService.closeNote(index); };
  downloadMessage = (messageId: string) => { this.storageService.downloadMessage(messageId); };

  constructor(
    private SweetAlert: SweetalertService,
    private storageService: StorageService
  ) { }

  ngOnInit() {
    // this.$state.current.data.breadcrumbs = "Adapter > " + (this.$state.params["storageSource"] == 'pipes' ? "Pipes > " + this.$state.params["storageSourceName"] + " > " : "") + this.$state.params["processState"] + " List > View Message " + this.$state.params["messageId"];
    this.storageService.closeNotes();

    if (!this.message.id) {
      this.SweetAlert.Warning("Invalid URL", "No message id provided!");
      return;
    }

    this.Api.Get(this.storageService.baseUrl + "/messages/" + encodeURIComponent(encodeURIComponent(this.message.id)), (data) => {
      this.metadata = data;
    }, (errorData, statusCode, errorMsg) => {
      let error = (errorData) ? errorData.error : errorMsg;
      if (statusCode == 500) {
        this.SweetAlert.Warning("An error occured while opening the message", "message id [" + this.message.id + "] error [" + error + "]");
      } else {
        this.SweetAlert.Warning("Message not found", "message id [" + this.message.id + "] error [" + error + "]");
      }
      // this.$state.go("pages.storage.list", { adapter: this.storageParams.adapterName, storageSource: this.storageParams.storageSource, storageSourceName: this.storageParams.storageSourceName, processState: this.storageParams.processState });
    });
  };

  getNotes() {
    return this.storageService.notes;
  }

  resendMessage(message: PartialMessage) {
    this.storageService.resendMessage(message, (messageId: string) => {
      //Go back to the storage list if successful
      // this.$state.go("pages.storage.list", { adapter: this.storageParams.adapterName, storageSource: this.storageParams.storageSource, storageSourceName: this.storageParams.storageSourceName, processState: this.storageParams.processState });
    });
  };

  deleteMessage(message: PartialMessage) {
    this.storageService.deleteMessage(message, (messageId: string) => {
      //Go back to the storage list if successful
      // this.$state.go("pages.storage.list", { adapter: this.storageParams.adapterName, storageSource: this.storageParams.storageSource, storageSourceName: this.storageParams.storageSourceName, processState: this.storageParams.processState });
    });
  };

  goBack = function () {
    history.back();
  }
}
