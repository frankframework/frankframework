import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IafUpdateComponent } from './iaf-update.component';

describe('IafUpdateComponent', () => {
  let component: IafUpdateComponent;
  let fixture: ComponentFixture<IafUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IafUpdateComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IafUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
