import { HttpResponse } from '@angular/common/http';
import { Component, Input, OnChanges } from '@angular/core';
import { StatusService } from '../status.service';
import { MiscService } from 'src/app/services/misc.service';
import { Adapter, AppService } from 'src/app/app.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FlowModalComponent } from './flow-modal/flow-modal.component';

import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { NgMermaidComponent } from '../../../components/ng-mermaid/ng-mermaid.component';

@Component({
  selector: 'app-flow',
  templateUrl: './flow.component.html',
  styleUrls: ['./flow.component.scss'],
  imports: [HasAccessToLinkDirective, NgMermaidComponent],
})
export class FlowComponent implements OnChanges {
  @Input() adapter: Adapter | null = null;
  @Input() configurationFlowDiagram: string | null = null;
  @Input() height = 350;
  @Input() canLoadInline = true;

  protected flow: {
    isImage: boolean;
    url: string;
    data?: HttpResponse<string>;
  } = { isImage: false, url: '' };
  protected flowModalLadda = false;
  protected loadInline = true;

  constructor(
    private appService: AppService,
    private Misc: MiscService,
    private statusService: StatusService,
    private modalService: NgbModal,
  ) {}

  ngOnChanges(): void {
    if (this.adapter || this.configurationFlowDiagram) {
      const flowUrl = this.getflowUrl();
      this.flow = { isImage: false, url: flowUrl };
      this.statusService.getAdapterFlowDiagram(flowUrl).subscribe((data) => {
        const status = data && data.status ? data.status : 204;
        if (status == 200) {
          const contentType = data.headers.get('Content-Type')!;
          this.flow.isImage = contentType.indexOf('image') > 0 || contentType.indexOf('svg') > 0; //display an image or a button to open a modal
          if (!this.flow.isImage) {
            //only store metadata when required
            this.flow.data = data;
            this.loadInline = this.canLoadInline;
            if (this.canLoadInline) {
              const dataLength = data.body?.length;
              this.loadInline = (dataLength ?? 0) < 20_000;
            }
          }
        } else {
          //If non successful response, force no-image-available
          this.flow.isImage = true;
          this.flow.url = 'assets/images/no_image_available.svg';
        }
      });
    }
  }

  openFlowModal(xhr?: HttpResponse<string>): void {
    this.flowModalLadda = true;
    const modalReference = this.modalService.open(FlowModalComponent, {
      windowClass: 'mermaidFlow',
    });
    modalReference.componentInstance.flow = xhr?.body;
    modalReference.componentInstance.flowName = this.adapter?.name ?? 'Configuration';
    setTimeout(() => {
      this.flowModalLadda = false;
    }, 1000);
  }

  private getflowUrl(): string {
    if (this.adapter) {
      return `${this.appService.getServerPath()}iaf/api/configurations/${this.adapter.configuration}/adapters/${this.Misc.escapeURL(this.adapter.name)}/flow?${this.adapter.upSince}`;
    }
    return this.configurationFlowDiagram ?? '';
  }
}
