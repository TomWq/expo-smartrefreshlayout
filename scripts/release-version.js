#!/usr/bin/env node

const { execFileSync } = require('node:child_process');

const releaseTypes = new Set(['patch', 'minor', 'major']);
const semverPattern = /^(0|[1-9][0-9]*)[.](0|[1-9][0-9]*)[.](0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:[.][0-9A-Za-z-]+)*))?(?:[+][0-9A-Za-z-]+(?:[.][0-9A-Za-z-]+)*)?$/;

function nextStableVersion(version, releaseType) {
  if (!releaseTypes.has(releaseType)) {
    throw new Error('release type must be patch, minor, or major');
  }

  const match = version.match(semverPattern);
  if (!match) {
    throw new Error(`package version is not a supported semantic version: ${version}`);
  }

  let [major, minor, patch] = match.slice(1, 4).map(Number);
  const hasPrerelease = Boolean(match[4]);
  if (![major, minor, patch].every(Number.isSafeInteger)) {
    throw new Error(`package version contains an unsafe numeric component: ${version}`);
  }

  if (releaseType === 'major') {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (releaseType === 'minor') {
    minor += 1;
    patch = 0;
  } else if (!hasPrerelease) {
    patch += 1;
  }

  return `${major}.${minor}.${patch}`;
}

function main() {
  const releaseType = process.argv[2];
  let targetVersion;

  try {
    targetVersion = nextStableVersion(require('../package.json').version, releaseType);
  } catch (error) {
    console.error(`Release blocked: ${error.message}`);
    process.exitCode = 1;
    return;
  }

  try {
    execFileSync('npm', ['version', targetVersion], { stdio: 'inherit' });
  } catch (error) {
    process.exitCode = error.status || 1;
  }
}

if (require.main === module) {
  main();
}

module.exports = { nextStableVersion };
