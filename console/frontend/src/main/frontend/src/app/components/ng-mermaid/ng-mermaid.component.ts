import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { Dimensions, getFactoryDimensions, initMermaid2Svg, mermaid2svg } from '@frankframework/frank-config-layout';

@Component({
  selector: 'ng-mermaid',
  template: ` <div class="{{ is_mermaid }}" #mermaidPre>Loading...</div> `,
  styles: [
    `
      div {
        width: 100%;
        height: 100%;
      }
    `,
  ],
  imports: [],
})
export class NgMermaidComponent implements OnInit, OnChanges, OnDestroy {
  @Input() nmModel?: string;
  @Input() nmRefreshInterval?: number;
  @Input() dimensions: Dimensions = getFactoryDimensions();
  @Output() nmInitCallback = new EventEmitter();

  @ViewChild('mermaidPre') mermaidEl!: ElementRef<HTMLElement>;

  protected interval = 2000;
  protected is_mermaid = 'mermaid';
  protected initialized = false;
  protected firstRender = true;
  protected mermaidSvgElement: SVGSVGElement | null = null;
  protected timeout?: number;

  constructor() {}

  ngOnInit(): void {
    initMermaid2Svg(this.dimensions);
    this.render();
    this.initialized = true;
  }

  ngOnChanges(): void {
    if (this.initialized) this.render();
  }

  ngOnDestroy(): void {
    this.mermaidEl.nativeElement.innerHTML = 'Loading...';
  }

  render(): void {
    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval || this.interval;
    }

    if (this.nmModel) {
      const mermaidContainer = this.mermaidEl.nativeElement;
      mermaidContainer.innerHTML = 'Loading...';
      if (this.timeout) {
        window.clearTimeout(this.timeout);
      }

      this.timeout = window.setTimeout(
        async () => {
          try {
            const mermaidSvg = await mermaid2svg(this.nmModel!);
            mermaidContainer.innerHTML = mermaidSvg;

            this.firstRender = false;
            this.nmInitCallback.emit();

            /*
            mermaidContainer.innerHTML = 'Loading...';
            const uid = `m${uuidv4()}`;

            mermaid
              .render(uid, this.nmModel!, mermaidContainer)
              .then(({ svg, bindFunctions }: RenderResult) => {
                mermaidContainer.innerHTML = svg;
                const svgElement = mermaidContainer.firstChild as SVGSVGElement;
                this.mermaidSvgElement = svgElement;
                svgElement.setAttribute('height', '100%');
                svgElement.setAttribute('style', 'max-width: 100%;');
                if (bindFunctions) bindFunctions(svgElement);
              })*/
          } catch (error) {
            this.handleError(error as Error);
          }
        },
        this.firstRender ? 0 : this.interval,
      );
    }
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
    this.mermaidEl.nativeElement.innerHTML += errorContainer;
  }
}
