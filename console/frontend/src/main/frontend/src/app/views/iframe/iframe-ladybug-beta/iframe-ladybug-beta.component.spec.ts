import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IframeLadybugBetaComponent } from './iframe-ladybug-beta.component';

describe('IframeLadybugBetaComponent', () => {
  let component: IframeLadybugBetaComponent;
  let fixture: ComponentFixture<IframeLadybugBetaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IframeLadybugBetaComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IframeLadybugBetaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
