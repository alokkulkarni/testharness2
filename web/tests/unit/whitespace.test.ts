import { describe, expect, it } from 'vitest';
import { load } from '../helpers/lazy';

const ws = () => load<typeof import('../../src/validation/whitespace')>('/src/validation/whitespace');

// sdd §5: exactly the 25 code points of the Unicode White_Space property; the same table runs in the API's unit tests.
const WHITESPACE: [string, number][] = [
  0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020, 0x0085, 0x00a0, 0x1680,
  0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a,
  0x2028, 0x2029, 0x202f, 0x205f, 0x3000,
].map((cp) => [`U+${cp.toString(16).toUpperCase().padStart(4, '0')}`, cp]);
const NOT_WHITESPACE: [string, number][] = [
  0x001c, 0x001d, 0x001e, 0x001f, 0x200b, 0x200c, 0x200d, 0x2060, 0xfeff,
].map((cp) => [`U+${cp.toString(16).toUpperCase().padStart(4, '0')}`, cp]);

describe('whitespace set (sdd §5)', () => {
  it('REQ-TODO-002 AC-2 the set has exactly 25 whitespace code points', async () => {
    const { isWhitespace } = await ws();
    expect(WHITESPACE).toHaveLength(25);
    for (let cp = 0; cp <= 0x3100; cp++) {
      expect(isWhitespace(cp)).toBe(WHITESPACE.some(([, w]) => w === cp));
    }
  });

  it.each(WHITESPACE)('REQ-TODO-002 AC-1 %s is whitespace and is trimmed from both ends', async (_name, cp) => {
    const { isWhitespace, trimWhitespace } = await ws();
    const ch = String.fromCodePoint(cp);
    expect(isWhitespace(cp)).toBe(true);
    expect(trimWhitespace(`${ch}${ch}title${ch}`)).toBe('title');
  });

  it.each(NOT_WHITESPACE)('REQ-TODO-002 AC-1 %s is not whitespace and is kept', async (_name, cp) => {
    const { isWhitespace, trimWhitespace } = await ws();
    const ch = String.fromCodePoint(cp);
    expect(isWhitespace(cp)).toBe(false);
    expect(trimWhitespace(`${ch}title${ch}`)).toBe(`${ch}title${ch}`);
  });

  it('REQ-TODO-002 AC-2 a title made only of non-breaking and other Unicode spaces is blank', async () => {
    const { isBlank } = await ws();
    expect(isBlank('  ')).toBe(true);
    expect(isBlank('  ')).toBe(true);
    expect(isBlank('　')).toBe(true);
    expect(isBlank('')).toBe(true);
    expect(isBlank('​')).toBe(false);
  });

  it('REQ-AUTH-001 AC-8 an identifier padded with U+00A0 and U+3000 is trimmed', async () => {
    const { trimWhitespace } = await ws();
    expect(trimWhitespace(' 　alice　 ')).toBe('alice');
  });

  it('REQ-LIST-002 AC-7 search text of only leading or trailing whitespace trims to empty', async () => {
    const { trimWhitespace } = await ws();
    expect(trimWhitespace(' 　 ')).toBe('');
  });

  it('REQ-TODO-002 AC-1 interior whitespace is kept', async () => {
    const { trimWhitespace } = await ws();
    expect(trimWhitespace('  buy milk  ')).toBe('buy milk');
  });
});
