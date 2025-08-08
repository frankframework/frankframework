import { inject, KeyValueDiffer, KeyValueDiffers, Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatStatKeys',
})
export class FormatStatKeysPipe implements PipeTransform {
  private keyValues: string[] = [];
  private differ?: KeyValueDiffer<string, string>;
  private differs = inject(KeyValueDiffers);

  transform(input: Record<string, string>): string[] {
    if (!input || (!(input instanceof Map) && typeof input !== 'object')) {
      return [];
    }
    if (!this.differ) {
      // make a differ for whatever type we've been passed in
      // eslint-disable-next-line unicorn/no-array-callback-reference
      this.differ = this.differs.find(input).create();
    }
    const differChanges = this.differ.diff(input);
    if (differChanges) {
      this.keyValues = [];
      differChanges.forEachItem((r) => {
        if (r.currentValue) this.keyValues.push(r.currentValue);
      });
    }
    return this.keyValues;
  }
}
