import { screen, waitFor } from '@testing-library/react';

// The app shell holds one always-present polite live region with role="status" (ux-design §6), so a test may
// never assert that no status element exists, or look for "the" status. It looks at the text of the live regions.
export const liveRegionText = (): string =>
  screen
    .queryAllByRole('status')
    .map((el) => el.textContent ?? '')
    .join(' ')
    .trim();

export const expectLiveRegionToSay = (text: RegExp | string): Promise<void> =>
  waitFor(() => {
    const said = liveRegionText();
    if (typeof text === 'string') {
      if (!said.includes(text)) throw new Error(`live regions say "${said}", not "${text}"`);
    } else if (!text.test(said)) {
      throw new Error(`live regions say "${said}", which does not match ${String(text)}`);
    }
  });
