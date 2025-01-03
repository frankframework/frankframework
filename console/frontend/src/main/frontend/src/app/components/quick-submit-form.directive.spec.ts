import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { QuickSubmitFormDirective } from './quick-submit-form.directive';

@Component({
  template: `<form (submit)="changeTrigger()" appQuickSubmitForm>
    <input type="text" />
  </form>`,
  imports: [QuickSubmitFormDirective],
})
class TestComponent {
  triggered = false;
  changeTrigger(): void {
    this.triggered = true;
  }
}

describe('QuickSubmitFormDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElement: HTMLFormElement;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [TestComponent, QuickSubmitFormDirective],
      declarations: [],
    }).createComponent(TestComponent);

    fixture.detectChanges();

    directiveElement = fixture.debugElement.query(By.directive(QuickSubmitFormDirective)).nativeElement;
  });

  it('should prevent default behavior and stop propagation on Enter key', () => {
    const event = new KeyboardEvent('keydown', { key: 'Enter' });
    spyOn(event, 'preventDefault');
    spyOn(event, 'stopPropagation');

    directiveElement.dispatchEvent(event);

    expect(event.preventDefault).toHaveBeenCalled();
    expect(event.stopPropagation).toHaveBeenCalled();
  });

  it('should submit the form on Ctrl+Enter', () => {
    const event = new KeyboardEvent('keydown', { key: 'Enter', ctrlKey: true });
    directiveElement.dispatchEvent(event);

    expect(fixture.componentInstance.triggered).toBe(true);
  });
});
