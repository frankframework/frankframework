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
  adapterName = this.$state.params["adapter"];
  configuration = this.$state.params["configuration"];
  processState = this.$state.params["processState"];
  storageSource = this.$state.params["storageSource"];
  storageSourceName = this.$state.params["storageSourceName"];

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
}

appModule.component('storage', {
  controller: ['Api', '$state', 'SweetAlert', 'Misc', StorageController],
  template: `
    <ui-view
      base-url="$ctrl.base_url"
    ></ui-view>
    `
});
