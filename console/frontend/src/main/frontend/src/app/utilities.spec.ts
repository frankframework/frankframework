import { computeServerPath, getProcessStateIcon, getProcessStateIconColor } from './utilities';
import { IconDefinition } from '@fortawesome/angular-fontawesome';
import { faGears, faPauseCircle, faServer, faSignIn, faTimesCircle } from '@fortawesome/free-solid-svg-icons';

function testProcessState(state: string, expectedIcon: IconDefinition, expectedIconColor: string): () => void {
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
    it('Available', testProcessState('Available', faServer, 'success'));

    it('InProcess', testProcessState('InProcess', faGears, 'success'));

    it('Done', testProcessState('Done', faSignIn, 'success'));

    it('Hold', testProcessState('Hold', faPauseCircle, 'warning'));

    it('Error', testProcessState('Error', faTimesCircle, 'danger'));
  });
});
