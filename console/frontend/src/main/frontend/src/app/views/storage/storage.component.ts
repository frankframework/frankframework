import { Component, OnInit } from '@angular/core';
import { StateService } from "@uirouter/angularjs";
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';
import { SweetAlertService } from 'src/angularjs/app/services/sweetalert.service';

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

@Component({
  selector: 'app-storage',
  templateUrl: './storage.component.html',
  styleUrls: ['./storage.component.scss'],
})
export class StorageComponent implements OnInit {
  adapterName = this.$state.params["adapter"];
  configuration = this.$state.params["configuration"];
  processState = this.$state.params["processState"];
  storageSource = this.$state.params["storageSource"];
  storageSourceName = this.$state.params["storageSourceName"];

  private baseUrl = "configurations/" + this.Misc.escapeURL(this.configuration) +
    "/adapters/" + this.Misc.escapeURL(this.adapterName) + "/" + this.storageSource +
    "/" + this.Misc.escapeURL(this.storageSourceName) + "/stores/" + this.processState;

  constructor(
    private Api: ApiService,
    private $state: StateService,
    private SweetAlert: SweetAlertService,
    private Misc: MiscService
  ) { }

  ngOnInit() {
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
