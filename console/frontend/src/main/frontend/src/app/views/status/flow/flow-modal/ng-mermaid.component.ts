import { CommonModule } from "@angular/common";
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from "@angular/core";
import mermaid from 'mermaid';

type CSSRuleExtended = CSSRule & { selectorText: string };

@Component({
  standalone: true,
  selector: 'ng-mermaid',
  imports: [
    CommonModule
  ],
  template: `<div>
      <!-- <div *ngIf="!mermaiderror" class="{{is_mermaid}}" [innerHtml]="model"></div> -->
      <div *ngIf="!mermaiderror">
        <pre class="{{is_mermaid}}" #test>{{test}}</pre>
      </div>
      <span *ngIf="mermaiderror" [innerHtml]="mermaiderror"></span>
    </div>`
})
export class NgMermaidComponent implements OnInit, OnChanges {
  @Input() nmModel?: any;
  @Input() nmRefreshInterval?: number;
  @Output() nmInitCallback = new EventEmitter();

  model = this.nmModel;
  interval = this.nmRefreshInterval || 2000;
  is_mermaid = 'mermaid';
  mermaiderror?: string;
  timeout?: number;

  test = `flowchart LR

A[Hard] -->|Text| B(Round)
B --> C{Decision}
C -->|One| D[Result 1]
C -->|Two| E[Result 2]`

  @ViewChild('test') testEl!: ElementRef<HTMLPreElement>;

  private element = this.elRef.nativeElement;

  constructor(private elRef: ElementRef<HTMLElement>){ }

  ngOnInit() {
    // angularjs ng:xxx style escape
    for (const styleidx in document.styleSheets) {
      for (var cssridx in document.styleSheets[styleidx].cssRules) {
        const cssroule = document.styleSheets[styleidx].cssRules[cssridx] as CSSRuleExtended;
        if (cssroule.selectorText) {
          cssroule.selectorText = this.cssReplace(cssroule.selectorText);
          cssroule.cssText = this.cssReplace(cssroule.cssText);
        }
      }
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.nmModel) {
        this.model = this.nmModel;
        this.element.querySelectorAll("[data-processed]").forEach((v, k) => {
          v.removeAttribute("data-processed");
        });
        if (this.timeout)
          window.clearTimeout(this.timeout);
        this.timeout = window.setTimeout(() => {
          this.mermaiderror = '';
          try {
            this.testEl.nativeElement.innerHTML=this.test;
            debugger;
            mermaid.init(this.testEl.nativeElement);
            this.nmInitCallback.emit();
          } catch (e) {
            if(e instanceof Error){
              e.message.split('\n').forEach((v) => {
                this.mermaiderror += '<span>' + v + '</span><br/>';
              });
            }
          }
        }, this.interval);
    }
    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval || this.interval;
    }
  }

  cssReplace(cssRule: string) {
    return cssRule
      .replace('ng\:cloak', 'ng--cloak')
      .replace('ng\:form', 'ng--form');
  };
}
