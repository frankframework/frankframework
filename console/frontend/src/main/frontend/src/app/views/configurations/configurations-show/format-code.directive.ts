import { Directive, ElementRef, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import * as Prism from 'prismjs';

@Directive({
  selector: '[appFormatCode]',
})
export class FormatCodeDirective implements OnInit, OnChanges {
  @Input() text: string = '';

  private element = this.elementRef.nativeElement;
  private code = document.createElement('code');
  private initHash = '';
  private initAdapter = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private elementRef: ElementRef<HTMLElement>,
  ) {}

  ngOnInit(): void {
    this.element.classList.add('line-numbers');
    this.element.classList.add('language-markup');
    this.element.append(this.code);
    this.route.fragment.subscribe((hash) => (this.initHash = hash ?? ''));
    this.route.queryParamMap.subscribe(
      (parameters) => (this.initAdapter = parameters.get('adapter') ?? ''),
    );
  }

  ngOnChanges(): void {
    if (this.text && this.text != '' /*  && this.initHash !== '' */) {
      $(this.code).text(this.text);
      Prism.highlightElement(this.code);

      this.addOnClickEvent(this.code);

      if (this.initHash != '') {
        this.scrollToLine();
      } else if (this.initAdapter != '') {
        this.scrollToAdapter();
      }
    } else if (this.text === '') {
      $(this.code).text(this.text);
    }
  }

  scrollToLine(): void {
    const element = $(`#${this.initHash}`);
    if (element) {
      element.addClass('line-selected');
      const lineNumber = Math.max(
        0,
        Number.parseInt(this.initHash.slice(1)) - 15,
      );
      setTimeout(() => {
        const lineElement = $(`#L${lineNumber}`)[0];
        if (lineElement) {
          lineElement.scrollIntoView();
        }
      }, 500);
    }
  }

  scrollToAdapter(): void {
    const element = document.querySelector(`.adapter-tag.${this.initAdapter}`);
    setTimeout(() => {
      element?.scrollIntoView();
      this.element.scrollTo({ left: 0 });
    }, 500);
  }

  addOnClickEvent(root: HTMLElement): void {
    const spanElements = $(root)
      .children('span.line-numbers-rows')
      .children('span');
    spanElements.on('click', (event) => {
      const target = $(event.target);
      target.parent().children('.line-selected').removeClass('line-selected');
      const anchor = target.attr('id');
      target.addClass('line-selected');
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: this.route.snapshot.queryParams,
        fragment: anchor,
      });
    });
  }
}
