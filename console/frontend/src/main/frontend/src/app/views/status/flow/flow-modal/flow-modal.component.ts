import { Component, Input, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppService } from 'src/app/app.service';
import { NgMermaidComponent } from 'src/app/components/ng-mermaid/ng-mermaid.component';

@Component({
  selector: 'app-flow-modal',
  templateUrl: './flow-modal.component.html',
  styleUrls: ['./flow-modal.component.scss']
})
export class FlowModalComponent {

  @Input() flowName = "";
  @Input() flow = "";

  protected showActionButtons = false;

  @ViewChild(NgMermaidComponent) ngMermaid!: NgMermaidComponent;

  constructor(
    private activeModal: NgbActiveModal,
    private appService: AppService
  ) { }

  close() {
    this.activeModal.close();
  }

  mermaidLoaded() {
    this.showActionButtons = true;
  }

  downloadAsPng() {
    this.svgToImage()
      .then(canvas => canvas.toDataURL('image/png'))
      .then(url => {
        const a = document.createElement('a');
        a.download = `${this.flowName}-flow.png`;
        a.href = url;
        a.click();
        a.remove();
      });
  }

  copyToClipboard() {
    this.svgToImage().then(canvas => canvas.toBlob(blob => {
      if (!blob)
        throw new Error("Couldn't create blob from canvas");

      navigator.clipboard.write([
        new ClipboardItem({
          [blob.type]: blob
        })
      ]);
    }));
  }

  openNewTab() {
    const newTab = window.open('about:blank');
    const svg = this.ngMermaid.getMermaidSvgElement()?.cloneNode(true) as SVGSVGElement;

    if(newTab && svg){
      setTimeout(() => {
        newTab.document.body.innerHTML = svg.outerHTML;
        newTab.document.title = `${this.flowName} Flow`;
      });
    }
  }

  private svgToBase64(svg: SVGSVGElement, width?: number, height?: number) {
    const svgClone = svg.cloneNode(true) as SVGSVGElement;
    if (width) svgClone.setAttribute('height', `${height}px`);
    if (height) svgClone.setAttribute('width', `${width}px`);
    const svgString = svgClone.outerHTML.replaceAll('<br>', '<br/>');
    return btoa(svgString);
  }

  private svgToImage() {
    return new Promise<HTMLCanvasElement>((resolve, reject) => {
      const canvas = document.createElement('canvas');
      const svg = this.ngMermaid.getMermaidSvgElement();

      if (svg) {
        const svgBox = svg.viewBox.baseVal;
        canvas.width = svgBox.width;
        canvas.height = svgBox.height;

        const context = canvas.getContext('2d');
        if (!context) {
          throw new Error('context not found');
        }
        context.fillStyle = 'white';
        context.fillRect(0, 0, canvas.width, canvas.height);

        const image = new Image();
        image.onload = () => {
          context.drawImage(image, 0, 0, canvas.width, canvas.height);
          resolve(canvas);
        }
        image.onerror = reject;
        image.src = `data:image/svg+xml;base64,${this.svgToBase64(svg, canvas.width, canvas.height)}`;
      }
    });

  }
}
