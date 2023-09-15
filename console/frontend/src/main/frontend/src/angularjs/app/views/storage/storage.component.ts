import { appModule } from "../../app.module";
import { ApiService } from "../../services/api.service";
import { MiscService } from "../../services/misc.service";
import { SweetAlertService } from "../../services/sweetalert.service";
import { StateService } from "@uirouter/angularjs";

export type Message = {
  id: string; //MessageId
	originalId: string; //Made up Id?
	correlationId: string;
	type: string;
	host: string;
	insertDate: number;
	comment: string;
	message: string;
  expiryDate?: number;
  label?: string;
	position?: number;
}

export type MessageRuntime = Message & {
  deleting?: boolean;
  resending?: boolean;
}

class StorageController {
  notes: { type: string, message: string }[] = [];
	adapterName = this.$state.params["adapter"];
  configuration = this.$state.params["configuration"];
  processState = this.$state.params["processState"];
  storageSource = this.$state.params["storageSource"];
  storageSourceName = this.$state.params["storageSourceName"];

  private base_url = "configurations/" + this.Misc.escapeURL(this.configuration) +
    "/adapters/" + this.Misc.escapeURL(this.adapterName) + "/" + this.storageSource +
    "/" + this.Misc.escapeURL(this.storageSourceName) + "/stores/" + this.processState;

  constructor(
    private Api: ApiService,
    private $state: StateService,
    private SweetAlert: SweetAlertService,
    private Misc: MiscService
  ) { }

	$onInit() {
		this.$state.current.data.pageTitle = this.$state.params["processState"] + " List";
    this.$state.current.data.breadcrumbs = "Adapter > " + (this.$state.params["storageSource"] == 'pipes' ? "Pipes > " + this.$state.params["storageSourceName"] + " > " : "") + this.$state.params["processState"] + " List";

    if (!this.adapterName) {
      this.SweetAlert.Warning("Invalid URL", "No adapter name provided!");
      return;
    }
    if (!this.storageSourceName) {
      this.SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
      return;
    }
    if (!this.storageSource) {
      this.SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
      return;
    }
    if (!this.processState) {
      this.SweetAlert.Warning("Invalid URL", "No storage type provided!");
      return;
    }
	}

	addNote(type: string, message: string) {
		this.notes.push({ type: type, message: message });
	}

	closeNote(index: number) {
    this.notes.splice(index, 1);
	}

	closeNotes() {
    this.notes = [];
	}

	updateTable() {
		var table = $('#datatable').DataTable();
		if (table)
			table.draw();
	}

  doDeleteMessage(message: MessageRuntime, callback: (messageId: string) => void) {
		message.deleting = true;
		let messageId = message.id;
    this.Api.Delete(this.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), () => {
			if (callback != undefined && typeof callback == 'function')
				callback(messageId);
      this.addNote("success", "Successfully deleted message with ID: " + messageId);
      this.updateTable();
		}, () => {
			message.deleting = false;
      this.addNote("danger", "Unable to delete messages with ID: " + messageId);
      this.updateTable();
		});
	};

	downloadMessage(messageId: string) {
    window.open(this.Misc.getServerPath() + "iaf/api/" + this.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)) + "/download");
	};

  doResendMessage(message: MessageRuntime, callback: (messageId: string) => void) {
		message.resending = true;
		let messageId = message.id;
    this.Api.Put(this.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), false, () => {
			if (callback != undefined && typeof callback == 'function')
				callback(message.id);
      this.addNote("success", "Message with ID: " + messageId + " will be reprocessed");
      this.updateTable();
		}, (data) => {
			message.resending = false;
			data = (data.error) ? data.error : data;
      this.addNote("danger", "Unable to resend message [" + messageId + "]. " + data);
      this.updateTable();
		}, false);
	};
};

appModule.component('storage', {
	controller: ['Api', '$state', 'SweetAlert', 'Misc', StorageController],
	template: `
    <div ui-view
      adapter-name="$ctrl.adapterName"
      base-url="$ctrl.base_url"
      storage-source-name="$ctrl.storageSourceName"
      storage-source="$ctrl.storageSource"
      process-state="$ctrl.processState",
      on-add-note="$ctrl.addNote(type, message, removeQueue)"
      on-close-note="$ctrl.closeNote(index)"
      on-close-notes="$ctrl.closeNotes()"
      on-update-table="$ctrl.updateTable()"
      on-do-delete-message="$ctrl.doDeleteMessage(message, callback)"
      on-download-message="$ctrl.downloadMessage(messageId)"
      on-do-resend-message="$ctrl.doResendMessage(message, callback)"
    ></div>
    `
});
