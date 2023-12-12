import { KeyValueDiffer, KeyValueDiffers, Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatStatKeys'
})
export class FormatStatKeysPipe implements PipeTransform {

  private keyValues: string[] = [];
  private differ?: KeyValueDiffer<string, any>;

  constructor(private differs: KeyValueDiffers) { }

  transform(input: Record<string, any>): string[] {
    if (!input || (!(input instanceof Map) && typeof input !== 'object')) {
      return [];
    }
    if (!this.differ) {
      // make a differ for whatever type we've been passed in
      this.differ = this.differs.find(input).create();
    }
    const differChanges = this.differ.diff(input);
    if (differChanges) {
      this.keyValues = [];
      differChanges.forEachItem((r) => {
        this.keyValues.push(r.currentValue);
      });
    }
    return this.keyValues;
  }

}
