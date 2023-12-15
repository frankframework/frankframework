import { HttpResponse } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { StatusService } from '../status.service';
import { MiscService } from 'src/app/services/misc.service';
import { Adapter, AppService } from 'src/app/app.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FlowModalComponent } from './flow-modal/flow-modal.component';

@Component({
  selector: 'app-flow',
  templateUrl: './flow.component.html',
  styleUrls: ['./flow.component.scss']
})
export class FlowComponent implements OnInit {

  flow: {
    image: boolean,
    url: string,
    data?: HttpResponse<string>
  } = { image: false, url: "" }
  flowModalLadda = false;

  @Input() adapter!: Adapter;

  constructor(
    private appService: AppService,
    private Misc: MiscService,
    private statusService: StatusService,
    private modalService: NgbModal,
  ) {}

  ngOnInit() {
    const uri = this.appService.getServerPath() + 'iaf/api/configurations/' + this.adapter.configuration + '/adapters/' + this.Misc.escapeURL(this.adapter.name) + "/flow?" + this.adapter.upSince;
    this.flow = { "image": false, "url": uri };
    this.statusService.getAdapterFlowDiagram(uri).subscribe((data) => {
      const status = (data && data.status) ? data.status : 204;
      if (status == 200) {
        const contentType = data.headers.get("Content-Type")!;
        this.flow.image = (contentType.indexOf("image") > 0 || contentType.indexOf("svg") > 0); //display an image or a button to open a modal
        if (!this.flow.image) { //only store metadata when required
          // data.adapter = this.adapter;
          this.flow.data = data;
        }
      } else { //If non successfull response, force no-image-available
        this.flow.image = true;
        this.flow.url = 'assets/images/no_image_available.svg'
      }
    });
  }

	openFlowModal(xhr?: HttpResponse<string>) {
    this.flowModalLadda = true;
    const modalRef = this.modalService.open(FlowModalComponent, { windowClass: 'mermaidFlow' });
    modalRef.componentInstance.flow = xhr?.body;
    modalRef.componentInstance.adapterName = this.adapter.name;
    setTimeout( () => { this.flowModalLadda = false; }, 1000);
  }

}
