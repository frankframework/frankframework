import {
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Dimensions, getFactoryDimensions, initMermaid2Svg, mermaid2svg } from '@frankframework/frank-config-layout';

@Component({
  selector: 'ng-mermaid',
  template: `<div class="mermaid" #mermaidPre>Loading...</div>`,
  imports: [],
})
export class NgMermaidComponent implements OnInit, OnChanges, OnDestroy {
  @Input() dimensions: Dimensions = getFactoryDimensions();
  @Input() nmModel?: string;
  @Input() nmRefreshInterval?: number;
  @Output() nmInitCallback = new EventEmitter();

  protected interval = 2000;
  protected initialized = false;
  protected firstRender = true;
  protected mermaidSvgElement: SVGSVGElement | null = null;
  protected timeout?: number;

  private readonly rootElement: ElementRef<HTMLElement> = inject(ElementRef);

  ngOnInit(): void {
    initMermaid2Svg(this.dimensions);
    this.rootElement.nativeElement.textContent = 'Waiting for mermaid model...';
    this.render();
    this.initialized = true;
  }

  ngOnChanges(): void {
    if (this.initialized) this.render();
  }

  ngOnDestroy(): void {
    this.rootElement.nativeElement.innerHTML = '';
  }

  render(): void {
    if (!this.nmModel) return;
    const mermaidContainer = this.rootElement.nativeElement;
    mermaidContainer.textContent = 'Loading...';

    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval ?? this.interval;
    }

    if (this.timeout) {
      window.clearTimeout(this.timeout);
    }

    this.timeout = window.setTimeout(
      async () => {
        try {
          const mermaidSvgString = await mermaid2svg(this.nmModel!);
          mermaidContainer.innerHTML = mermaidSvgString;

          const mermaidSvg = mermaidContainer.firstChild as SVGSVGElement;
          const viewBoxWidth = mermaidSvg.getAttribute('width');
          const viewBoxHeight = mermaidSvg.getAttribute('height');
          mermaidSvg.setAttribute('width', '100%');
          mermaidSvg.setAttribute('height', '100%');
          mermaidSvg.setAttribute('viewBox', `0 0 ${viewBoxWidth} ${viewBoxHeight}`);

          this.firstRender = false;
          this.nmInitCallback.emit();
        } catch (error) {
          this.handleError(error as Error);
        }
      },
      this.firstRender ? 0 : this.interval,
    );
  }

  getMermaidSvgElement(): SVGSVGElement | null {
    return this.mermaidSvgElement;
  }

  private handleError(error: Error): void {
    console.error(error);
    let errorContainer = '';
    errorContainer += `<div style="display: inline-block; text-align: left; color: red; margin: 8px auto; font-family: Monaco,Consolas,Liberation Mono,Courier New,monospace">`;
    for (const v of error.message.split('\n')) {
      errorContainer += `<span>${v}</span><br/>`;
    }
    errorContainer += `</div>`;
    this.rootElement.nativeElement.innerHTML += errorContainer;
  }
}
