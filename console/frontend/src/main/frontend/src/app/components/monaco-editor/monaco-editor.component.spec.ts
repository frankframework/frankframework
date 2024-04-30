import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MonacoEditorComponent } from './monaco-editor.component';

describe('MonacoEditorComponent', () => {
  let component: MonacoEditorComponent;
  let fixture: ComponentFixture<MonacoEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MonacoEditorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MonacoEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
