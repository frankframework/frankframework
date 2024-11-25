// /// <reference path="../../../../node_modules/mermaid/dist/mermaid.d.ts" /> doesnt work
import { CommonModule } from '@angular/common';
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
import { v4 as uuidv4 } from 'uuid';
// @ts-expect-error mermaid does not have types
import mermaidImport, { RenderResult } from 'mermaid/dist/mermaid.esm.mjs';
const mermaid = mermaidImport;

// Uncomment developing for type support, comment again when compiling as it causes errors
// import type { Mermaid } from 'mermaid';
// const mermaid: Mermaid = mermaidImport;

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
  standalone: true,
  imports: [CommonModule],
})
export class NgMermaidComponent implements OnInit, OnChanges, OnDestroy {
  @Input() nmModel?: string;
  @Input() nmRefreshInterval?: number;
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
      if (this.timeout) {
        window.clearTimeout(this.timeout);
      }

      this.timeout = window.setTimeout(
        () => {
          try {
            mermaid.initialize({
              startOnLoad: false,
              maxTextSize: 70 * 1000,
              maxEdges: 600,
            });

            const mermaidContainer = this.mermaidEl.nativeElement;
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
              })
              .catch((error: Error) => {
                this.handleError(error);
              })
              .finally(() => {
                this.firstRender = false;
                this.nmInitCallback.emit();
              });
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
