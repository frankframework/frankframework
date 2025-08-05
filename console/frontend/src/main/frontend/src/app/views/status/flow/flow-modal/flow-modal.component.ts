import { Component, inject, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { NgMermaidComponent } from 'src/app/components/ng-mermaid/ng-mermaid.component';
import { ZoomPanDirective } from '../../../../components/zoom-pan.directive';

@Component({
  selector: 'app-flow-modal',
  templateUrl: './flow-modal.component.html',
  styleUrls: ['./flow-modal.component.scss'],
  imports: [NgMermaidComponent, ZoomPanDirective],
})
export class FlowModalComponent {
  @Input() flowName = '';
  @Input() flow = '';

  protected showActionButtons = false;
  protected errorActionMessage: null | string = null;
  protected isFirefox = false;
  protected flowSvg: SVGSVGElement | null = null;
  protected zoom = '100%';

  private activeModal: NgbActiveModal = inject(NgbActiveModal);

  close(): void {
    this.activeModal.close();
  }

  mermaidLoaded(flowSvg: SVGSVGElement): void {
    this.flowSvg = flowSvg;
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

    if (newTab && this.flowSvg) {
      const svgBox = this.flowSvg.dataset;
      const width = +(svgBox['contentWidth'] ?? '0');
      const height = +(svgBox['contentHeight'] ?? '0');
      const svg = this.cloneSvg(this.flowSvg, width, height);

      setTimeout(() => {
        newTab.document.body.innerHTML = svg.outerHTML;
        newTab.document.title = `${this.flowName} Flow`;
      }, 50);
    }
  }

  updateZoom(scale: number): void {
    this.zoom = `${scale.toFixed(0)}%`;
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

  private svgToBase64(svg: SVGSVGElement, width: number, height: number): string {
    const svgClone = this.cloneSvg(svg, width, height);
    svgClone.setAttribute('height', `${window.innerWidth}px`);
    svgClone.setAttribute('width', `${window.innerHeight}px`);
    const svgString = svgClone.outerHTML.replaceAll('<br>', '<br/>');
    return btoa(svgString);
  }

  private svgToImage(): Promise<HTMLCanvasElement> {
    return new Promise<HTMLCanvasElement>((resolve, reject) => {
      const canvas = document.createElement('canvas');
      canvas.addEventListener('error', (event) => reject(event));

      if (!this.flowSvg) {
        throw new Error('Mermaid SVG not found');
      }

      const svgBox = this.flowSvg.dataset;
      canvas.width = +(svgBox['contentWidth'] ?? '0');
      canvas.height = +(svgBox['contentHeight'] ?? '0');

      const context = canvas.getContext('2d');
      if (!context) {
        throw new Error('context not found');
      }
      context.fillStyle = 'white';
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.imageSmoothingQuality = 'high';

      const base64Svg = this.svgToBase64(this.flowSvg, canvas.width, canvas.height);
      const base64SvgLength = base64Svg.length;

      if (base64SvgLength > 1e6) {
        reject('Image is too big');
      }

      const image = new Image();
      image.addEventListener('load', () => {
        try {
          context.drawImage(image, 0, 0, canvas.width, canvas.height);

          setTimeout(() => {
            resolve(canvas);
          });
        } catch (error) {
          if ((error as DOMException).name === 'InvalidStateError') {
            reject("Couldn't recreate image (browser gives no reason)");
            return;
          }
          reject(error);
        }
      });
      // eslint-disable-next-line unicorn/prefer-add-event-listener
      image.onerror = (event): void => reject(event);
      image.src = `data:image/svg+xml;base64,${base64Svg}`;
    });
  }

  private cloneSvg(svg: SVGSVGElement, width: number, height: number): SVGSVGElement {
    const svgClone = svg.cloneNode(true) as SVGSVGElement;
    svgClone.setAttribute('viewBox', `0 0 ${width} ${height}`);
    return svgClone;
  }
}
