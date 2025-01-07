import { Component, Input, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { NgMermaidComponent } from 'src/app/components/ng-mermaid/ng-mermaid.component';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-flow-modal',
  templateUrl: './flow-modal.component.html',
  styleUrls: ['./flow-modal.component.scss'],
  imports: [NgMermaidComponent, NgIf],
})
export class FlowModalComponent {
  @Input() flowName = '';
  @Input() flow = '';
  @ViewChild(NgMermaidComponent) ngMermaid!: NgMermaidComponent;

  protected showActionButtons = false;
  protected errorActionMessage: null | string = null;

  constructor(private activeModal: NgbActiveModal) {}

  close(): void {
    this.activeModal.close();
  }

  mermaidLoaded(): void {
    this.showActionButtons = true;
  }

  downloadAsPng(event: MouseEvent): void {
    const buttonElement = event.target as HTMLButtonElement;
    this.svgToImage()
      .then((canvas) => canvas.toDataURL('image/png'))
      .then((url) => {
        const a = document.createElement('a');
        a.download = `${this.flowName}-flow.png`;
        a.href = url;
        a.click();
        a.remove();
      })
      .catch((error) => {
        buttonElement.textContent = 'Failed';
        buttonElement.disabled = true;
        this.errorActionMessage = error;
        console.error(error);
      });
  }

  copyToClipboard(event: MouseEvent): void {
    const buttonElement = event.target as HTMLButtonElement;
    buttonElement.textContent = 'Copying...';
    this.svgToImage()
      .then((canvas) => this.canvasToBlob(canvas))
      .then((blob) =>
        navigator.clipboard.write([
          new ClipboardItem({
            [blob.type]: blob,
          }),
        ]),
      )
      .then(() => (buttonElement.textContent = 'Copied'))
      .catch((error) => {
        buttonElement.textContent = 'Failed';
        buttonElement.disabled = true;
        this.errorActionMessage = error;
        console.error(error);
      });
  }

  openNewTab(): void {
    const newTab = window.open('about:blank');
    const svg = this.ngMermaid.getMermaidSvgElement()?.cloneNode(true) as SVGSVGElement;

    if (newTab && svg) {
      setTimeout(() => {
        newTab.document.body.innerHTML = svg.outerHTML;
        newTab.document.title = `${this.flowName} Flow`;
      });
    }
  }

  private canvasToBlob(canvas: HTMLCanvasElement, type?: string, quality?: number): Promise<Blob> {
    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob != null) {
            resolve(blob);
          }
          reject("Couldn't create blob from canvas");
        },
        type,
        quality,
      );
    });
  }

  private svgToBase64(svg: SVGSVGElement, width?: number, height?: number): string {
    const svgClone = svg.cloneNode(true) as SVGSVGElement;
    if (width) svgClone.setAttribute('height', `${height}px`);
    if (height) svgClone.setAttribute('width', `${width}px`);
    const svgString = svgClone.outerHTML.replaceAll('<br>', '<br/>');
    return btoa(svgString);
  }

  private svgToImage(): Promise<HTMLCanvasElement> {
    return new Promise<HTMLCanvasElement>((resolve, reject) => {
      const canvas = document.createElement('canvas');
      const svg = this.ngMermaid.getMermaidSvgElement();

      if (!svg) {
        throw new Error('Mermaid SVG not found');
      }

      const svgBox = svg.viewBox.baseVal;
      canvas.width = svgBox.width;
      canvas.height = svgBox.height;

      const context = canvas.getContext('2d');
      if (!context) {
        throw new Error('context not found');
      }
      context.fillStyle = 'white';
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.imageSmoothingQuality = 'high';

      const image = new Image();
      image.addEventListener('load', () => {
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas);
      });
      // eslint-disable-next-line unicorn/prefer-add-event-listener
      image.onerror = reject;
      image.src = `data:image/svg+xml;base64,${this.svgToBase64(svg, canvas.width, canvas.height)}`;
    });
  }
}
