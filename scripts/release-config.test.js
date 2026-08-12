const { publishTag } = require('./publish-dry-run');
const { nextStableVersion } = require('./release-version');

describe('release version selection', () => {
  test.each([
    ['2.0.0-alpha.0', 'patch', '2.0.0'],
    ['2.0.0-alpha.0', 'minor', '2.1.0'],
    ['2.0.0-alpha.0', 'major', '3.0.0'],
    ['2.0.0', 'patch', '2.0.1'],
    ['2.0.0', 'minor', '2.1.0'],
    ['2.0.0', 'major', '3.0.0'],
  ])('%s %s becomes %s', (version, releaseType, expectedVersion) => {
    expect(nextStableVersion(version, releaseType)).toBe(expectedVersion);
  });
});

describe('publish tag selection', () => {
  test.each([
    ['2.0.0-alpha.0', 'alpha'],
    ['2.0.0-beta.3', 'beta'],
    ['2.0.0', 'latest'],
  ])('%s uses the %s tag', (version, expectedTag) => {
    expect(publishTag(version)).toBe(expectedTag);
  });
});
