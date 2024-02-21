import { Component, Input, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { NgMermaidComponent } from 'src/app/components/ng-mermaid/ng-mermaid.component';

@Component({
  selector: 'app-flow-modal',
  templateUrl: './flow-modal.component.html',
  styleUrls: ['./flow-modal.component.scss']
})
export class FlowModalComponent {

  @Input() flowName = "";
  @Input() flow = "";

  @ViewChild(NgMermaidComponent) ngMermaid!: NgMermaidComponent;

  constructor(
    private activeModal: NgbActiveModal,
  ) { }

  close() {
    this.activeModal.close();
  }


  // https://github.com/mermaid-js/mermaid-live-editor/blob/master/src/lib/components/Actions.svelte#L136
  // https://github.com/tsayen/dom-to-image/blob/master/src/dom-to-image.js
  svgToImage() {
    const canvas = document.createElement('canvas');
    const svg = this.ngMermaid.getMermaidSvgElement();

    if (svg) {
      const box: DOMRect = svg.getBoundingClientRect();
      canvas.width = box.width;
      canvas.height = box.height;

      const context = canvas.getContext('2d');
      if (!context) {
        throw new Error('context not found');
      }
      context.fillStyle = 'white';
      context.fillRect(0, 0, canvas.width, canvas.height);

      const image = new Image();
      // image.addEventListener('load', exporter(context, image));
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      this.download('test.png', canvas.toDataURL('image/png'));
    }
  }

  download(download: string, href: string) {
    const a = document.createElement('a');
    a.download = download;
    a.href = href;
    a.click();
    a.remove();
  }

  copyToClipboard() {

  }
}
