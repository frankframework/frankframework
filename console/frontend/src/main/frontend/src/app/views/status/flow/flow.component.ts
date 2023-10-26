import { HttpResponse } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { StatusService } from '../status.service';
import { MiscService } from 'src/app/services/misc.service';
import { Adapter } from 'src/app/app.service';

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
    private Misc: MiscService,
    private statusService: StatusService
  ) {}

  ngOnInit() {
    const uri = this.Misc.getServerPath() + 'iaf/api/configurations/' + this.adapter.configuration + '/adapters/' + this.Misc.escapeURL(this.adapter.name) + "/flow?" + this.adapter.upSince;
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
        this.flow.url = 'images/no_image_available.svg'
      }
    });
  }

	openFlowModal(xhr?: HttpResponse<string>) {
    this.flowModalLadda = true;
    // TODO modal
    /* $uibModal.open({
      templateUrl: 'js/app/views/status/flow/flow-modal/flow-modal.html',
      windowClass: 'mermaidFlow',
      resolve: {
        xhr: function () {
          return xhr;
        }
      },
      controller: 'FlowDiagramModalCtrl'
    }); */
    setTimeout( () => { this.flowModalLadda = false; }, 1000);
  }

}
