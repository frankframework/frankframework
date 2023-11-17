import { Directive, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import * as Prism from 'prismjs';

@Directive({
  selector: '[appFormatCode]'
})
export class FormatCodeDirective implements OnChanges {
  @Input() text: string = "";

  constructor(
    private element: ElementRef,
  ) { };

  ngOnChanges(changes: SimpleChanges) {
    var code = document.createElement('code');
    // TODO:
    // this.element.addClass("line-numbers");
    // this.element.addClass("language-markup");
    // this.element.append(code);
    // let initHash = $location.hash();

    if (this.text && this.text != '') {
      angular.element(code).text(this.text);
      Prism.highlightElement(code);

      addOnClickEvent(code);

      // TODO: $location.hash(initHash);
      let el = angular.element("#"); // Todo: + initHash
      if (el) {
        el.addClass("line-selected");
        // TODO: let lineNumber = Math.max(0, parseInt(initHash.substr(1)) - 15);
        setTimeout(() => {
          let lineElement = angular.element("#L")[0]; // Todo:  + lineNumber
          if (lineElement) {
            lineElement.scrollIntoView();
          }
        }, 500)
      }
    } else if (this.text === '') {
      angular.element(code).text(this.text);
    }

    function addOnClickEvent(root: HTMLElement) {
      let spanElements = $(root).children("span.line-numbers-rows").children("span");
      spanElements.on("click", (event) => {
        // TODO:
        // let target = $(event.target);
        // target.parent().children(".line-selected").removeClass("line-selected");
        // let anchor = target.attr('id');
        // target.addClass("line-selected");
        // $location.hash(anchor);
      });
    }

    // TODO:
    // this.element.on('$destroy', function () {
    //   watch();
    // });
  }
}
