#!/usr/bin/env node

const { execFileSync } = require('node:child_process');

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
}

try {
  if (git(['rev-parse', '--is-inside-work-tree']).trim() !== 'true') {
    throw new Error('not inside a Git work tree');
  }

  const status = git(['status', '--porcelain=v1', '--untracked-files=all']).trim();
  if (status) {
    console.error('Release blocked: the Git work tree is not clean.');
    console.error('Commit, stash, or remove every change before creating a release version.');
    process.exitCode = 1;
  }
} catch (error) {
  if (!process.exitCode) {
    console.error(`Release blocked: unable to verify Git status (${error.message}).`);
    process.exitCode = 1;
  }
}
