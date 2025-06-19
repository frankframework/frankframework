import {
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  isDevMode,
  OnChanges,
  OnInit,
  Output,
} from '@angular/core';
import { Dimensions, getFactoryDimensions, initMermaid2Svg, mermaid2svg } from '@frankframework/frank-config-layout';

@Component({
  selector: 'ng-mermaid',
  template: '',
  styles: [
    `
      :host {
        display: block;
        width: 100%;
        height: 100%;
      }
    `,
  ],
  imports: [],
})
export class NgMermaidComponent implements OnInit, OnChanges {
  @Input() dimensions: Dimensions = getFactoryDimensions();
  @Input() flowName: string = '';
  @Input() nmModel: string = '';
  @Input() nmRefreshInterval?: number;
  @Output() nmInitCallback: EventEmitter<SVGSVGElement> = new EventEmitter();

  protected interval = 2000;
  protected initialized = false;
  protected firstRender = true;
  protected timeout?: number;

  private readonly rootElementReference: ElementRef<HTMLElement> = inject(ElementRef);
  private readonly rootElement = this.rootElementReference.nativeElement;

  ngOnInit(): void {
    initMermaid2Svg(this.dimensions);
    this.rootElement.textContent = 'Waiting for mermaid model...';
    this.render();
    this.initialized = true;
  }

  ngOnChanges(): void {
    if (this.initialized) this.render();
  }

  render(): void {
    if (!this.nmModel || this.nmModel == '') {
      this.rootElement.textContent = 'No mermaid model available';
      return;
    }

    this.rootElement.textContent = 'Loading...';

    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval ?? this.interval;
    }

    if (this.timeout) {
      window.clearTimeout(this.timeout);
    }

    this.timeout = window.setTimeout(
      async () => {
        try {
          this.rootElement.innerHTML = await mermaid2svg(this.nmModel!);

          const mermaidSvg = this.rootElement.firstChild as SVGSVGElement;
          const viewBoxWidth = mermaidSvg.getAttribute('width');
          const viewBoxHeight = mermaidSvg.getAttribute('height');
          mermaidSvg.setAttribute('width', '100%');
          mermaidSvg.setAttribute('height', '100%');
          mermaidSvg.setAttribute('viewBox', `0 0 ${viewBoxWidth} ${viewBoxHeight}`);

          this.firstRender = false;
          this.nmInitCallback.emit(mermaidSvg);
        } catch (error) {
          this.handleError(error as Error);
        }
      },
      this.firstRender ? 0 : this.interval,
    );
  }

  private handleError(error: Error): void {
    console.error(
      `An error occurred while trying to render mermaid flow for '${this.flowName}'\nMermaid flow code:\n${this.nmModel}`,
      error,
    );
    if (!isDevMode()) {
      this.rootElement.innerHTML = '<span style="font-size: 16px">&otimes;</span>';
      return;
    }
    let errorContainer = '<div style="font-size: 16px">&otimes;</div>';
    errorContainer += `<div style="display: inline-block; text-align: left; color: red; margin: 8px auto; font-family: Monaco,Consolas,Liberation Mono,Courier New,monospace">`;
    for (const v of error.message.split('\n')) {
      errorContainer += `<span>${v}</span><br/>`;
    }
    errorContainer += `</div>`;
    this.rootElement.innerHTML = errorContainer;
  }
}
