import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MonitorsAddEditComponent } from './monitors-add-edit.component';

describe('MonitorsAddEditComponent', () => {
  let component: MonitorsAddEditComponent;
  let fixture: ComponentFixture<MonitorsAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MonitorsAddEditComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MonitorsAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
