import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DatatableComponent } from './datatable.component';

describe('DatatableComponent', () => {
  let component: DatatableComponent<object>;
  let fixture: ComponentFixture<DatatableComponent<object>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DatatableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DatatableComponent<object>);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
