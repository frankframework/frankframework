import { computeServerPath, getProcessStateIcon, getProcessStateIconColor } from './utilities';

function testProcessState(state: string, expectedIcon: string, expectedIconColor: string): () => void {
  return (): void => {
    expect(getProcessStateIcon(state)).toBe(expectedIcon);
    expect(getProcessStateIconColor(state)).toBe(expectedIconColor);
  };
}

describe('Utils', () => {
  it('computeServerPath', () => {
    expect(computeServerPath()).toContain('/');
  });

  describe('getProcessState Icon & Color', () => {
    it('Available', testProcessState('Available', 'fa-server', 'success'));

    it('InProcess', testProcessState('InProcess', 'fa-gears', 'success'));

    it('Done', testProcessState('Done', 'fa-sign-in', 'success'));

    it('Hold', testProcessState('Hold', 'fa-pause-circle', 'warning'));

    it('Error', testProcessState('Error', 'fa-times-circle', 'danger'));
  });
});
