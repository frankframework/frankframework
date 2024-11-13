import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MonitorsNewComponent } from './monitors-new.component';

describe('MonitorsNewComponent', () => {
  let component: MonitorsNewComponent;
  let fixture: ComponentFixture<MonitorsNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MonitorsNewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MonitorsNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
