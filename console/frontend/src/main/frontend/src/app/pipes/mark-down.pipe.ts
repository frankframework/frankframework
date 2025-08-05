import { inject, Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import MarkdownIt from 'markdown-it';

@Pipe({
  name: 'markDown',
})
export class MarkDownPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(input: string): SafeHtml {
    if (!input) return '';
    // input = input.replace(/(?:\r\n|\r|\n)/g, '<br />');
    // input = input.replace(/\[(.*?)\]\((.+?)\)/g, '<a target="_blank" href="$2" alt="$1">$1</a>');
    const md = MarkdownIt({
      html: true,
      linkify: true,
      typographer: false,
    });
    return this.sanitizer.bypassSecurityTrustHtml(md.render(input));
  }
}
