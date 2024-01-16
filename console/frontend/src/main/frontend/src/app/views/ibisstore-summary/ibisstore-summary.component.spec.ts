import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IbisstoreSummaryComponent } from './ibisstore-summary.component';

describe('IbisstoreSummaryComponent', () => {
  let component: IbisstoreSummaryComponent;
  let fixture: ComponentFixture<IbisstoreSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IbisstoreSummaryComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IbisstoreSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
