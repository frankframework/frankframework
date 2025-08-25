import { Component, inject, Input, OnChanges } from '@angular/core';
import { StatusService } from '../status.service';
import { MiscService } from 'src/app/services/misc.service';
import { Adapter, AppService } from 'src/app/app.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FlowModalComponent } from './flow-modal/flow-modal.component';

import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { NgMermaidComponent } from '../../../components/ng-mermaid/ng-mermaid.component';
import { HttpResponse } from '@angular/common/http';
import { faShareAltSquare } from '@fortawesome/free-solid-svg-icons';

type FlowModel = {
  isImage: boolean;
  url: string;
  data: string | null;
};

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

  protected flow: FlowModel = { isImage: false, url: '', data: null };
  protected flowModalLadda = false;
  protected loadInline = true;
  protected flowName = '';
  protected readonly faShareAltSquare = faShareAltSquare;

  private readonly appService: AppService = inject(AppService);
  private readonly Misc: MiscService = inject(MiscService);
  private readonly statusService: StatusService = inject(StatusService);
  private readonly modalService: NgbModal = inject(NgbModal);

  ngOnChanges(): void {
    if (!!this.adapter || this.configurationFlowDiagram) {
      const flowUrl = this.getflowUrl();
      this.flow = { isImage: false, url: flowUrl, data: null };
      this.flowName = this.adapter ? `${this.adapter.configuration}/${this.adapter.name}` : 'Configuration';

      this.checkLoadInline();
    }
  }

  prepareFlowModal(): void {
    this.flowModalLadda = true;

    if (!this.loadInline) {
      this.statusService.getAdapterFlowDiagram(this.flow.url).subscribe((data) => {
        this.loadFlowData(data);
        this.openFlowModal();
      });
      return;
    }
    this.openFlowModal();
  }

  private getflowUrl(): string {
    if (this.adapter) {
      return `${this.appService.getServerPath()}iaf/api/configurations/${this.adapter.configuration}/adapters/${this.Misc.escapeURL(this.adapter.name)}/flow?${this.adapter.upSince}`;
    }
    return this.configurationFlowDiagram ?? '';
  }

  private checkLoadInline(): void {
    if (!this.canLoadInline) {
      this.loadInline = false;
      return;
    }

    this.statusService.getAdapterFlowDiagramContentLength(this.flow.url).subscribe((length) => {
      this.loadInline = length < 20_000;
      if (this.loadInline) {
        this.statusService.getAdapterFlowDiagram(this.flow.url).subscribe((data) => this.loadFlowData(data));
      }
    });
  }

  private loadFlowData(data: HttpResponse<string>): void {
    const status = data && data.status ? data.status : 204;
    if (status == 200) {
      const contentType = data.headers.get('Content-Type')!;
      this.flow.isImage = contentType.includes('image') || contentType.includes('svg'); //display an image or a button to open a modal
      if (!this.flow.isImage) {
        //only store metadata when required
        this.flow.data = data.body;
      }
      return;
    }
    //If non successful response, force no-image-available
    this.flow.isImage = true;
    this.flow.url = 'assets/images/no_image_available.svg';
  }

  private openFlowModal(): void {
    setTimeout(() => {
      this.flowModalLadda = false;
    }, 1000);

    const modalReference = this.modalService.open(FlowModalComponent, {
      windowClass: 'mermaidFlow',
    });
    modalReference.componentInstance.flow = this.flow.data ?? '';
    modalReference.componentInstance.flowName = this.adapter?.name ?? 'Configuration';
  }
}
