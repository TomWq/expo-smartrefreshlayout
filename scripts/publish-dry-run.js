#!/usr/bin/env node

const { execFileSync } = require('node:child_process');
const { version } = require('../package.json');

function publishTag(version) {
  const prerelease = version.split('-', 2)[1]?.split('+', 1)[0];
  return prerelease ? prerelease.split('.', 1)[0] : 'latest';
}

function main() {
  const tag = publishTag(version);

  try {
    execFileSync(
      'npm',
      [
        'publish',
        '--dry-run',
        '--access',
        'public',
        '--registry=https://registry.npmjs.org/',
        '--tag',
        tag,
      ],
      { stdio: 'inherit' }
    );
  } catch (error) {
    process.exitCode = error.status || 1;
  }
}

if (require.main === module) {
  main();
}

module.exports = { publishTag };
