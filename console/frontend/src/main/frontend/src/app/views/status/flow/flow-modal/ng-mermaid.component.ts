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
  template: `
    <pre
      class="{{is_mermaid}}"
      #mermaidPre
    ></pre>
  `
})
export class NgMermaidComponent implements OnInit, OnChanges {
  @Input() nmModel?: any;
  @Input() nmRefreshInterval?: number;
  @Output() nmInitCallback = new EventEmitter();

  model = this.nmModel;
  interval = this.nmRefreshInterval || 2000;
  is_mermaid = 'mermaid';
  timeout?: number;

  test = `graph
	classDef default fill:#fff,stroke:#1a9496,stroke-width:2px;
	d45e2{{Receiver<br/>JavaListener}}
	d45e5{{Receiver<br/>JavaListener}}
	d45e7{{Receiver<br/>JavaListener}}
	d45e9{{Receiver<br/>WebServiceListener}}
	d45e11{{Receiver<br/>JavaListener}}
	d45e13{{Receiver<br/>WebServiceListener}}
	d45e15{{Receiver<br/>JavaListener}}
	d45e18([InputValidator<br/>XmlValidator])
	style d45e33 stroke-dasharray: 4 4
	d45e33(InputValidateFailure<br/>XsltPipe)
	style d45e31 stroke-dasharray: 4 4
	d45e31(InputValidateError<br/>FixedResultPipe)
	d45e25(Query<br/>XmlQuerySender)
	d45e28(ManageDatabaseRLY<br/>XsltPipe)
	d45e24{{success}}
	d45e2 --> |success| d45e18
	d45e5 --> |success| d45e18
	d45e7 --> |success| d45e18
	d45e9 --> |success| d45e18
	d45e11 --> |success| d45e18
	d45e13 --> |success| d45e18
	d45e15 --> |success| d45e18
	d45e18 -. failure .-> d45e33
	d45e18 -. parserError .-> d45e31
	d45e18 --> |success| d45e25
	d45e33 -. success .->
	d45e31 -. success .->
	d45e25 --> |success| d45e28`

  @ViewChild('mermaidPre') mermaidEl!: ElementRef<HTMLPreElement>;

  private element = this.elRef.nativeElement;

  constructor(private elRef: ElementRef<HTMLElement>){ }

  ngOnInit() {
    debugger;
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
          try {
            // this.mermaidEl.nativeElement.innerHTML = this.nmModel;
            // debugger;
            this.mermaidEl.nativeElement.innerHTML = this.test;
            mermaid.init(this.mermaidEl.nativeElement);
            this.nmInitCallback.emit();
          } catch (e) {
            if(e instanceof Error){
              e.message.split('\n').forEach((v) => {
                this.mermaidEl.nativeElement.innerHTML = '<span>' + v + '</span><br/>';
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
