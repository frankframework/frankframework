import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'literal',
})
export class LiteralPipe implements PipeTransform {
  transform(value: string): string {
    // since we cant get a singular "\", we do manual replacements using String.raw
    return value
      .replaceAll('\\', String.raw`\\`)
      .replaceAll('\t', String.raw`\t`)
      .replaceAll('\n', String.raw`\n`)
      .replaceAll('\r', String.raw`\r`)
      .replaceAll('\f', String.raw`\f`)
      .replaceAll('\v', String.raw`\v`);
  }
}
