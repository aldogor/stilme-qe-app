"""Build the public cut of this repository on the local branch `public-master`.

This repository is the internal one (remote "origin", private) and holds the whole project. The
public repository (remote "public") receives only what public_exclude.txt does not list. The cut is
built from the committed HEAD in a temporary git index, so the working tree and `master` are never
touched:

  1. every file tracked at HEAD that matches a pattern of public_exclude.txt is dropped;
  2. the remaining tree is checked again against the manifest, and the script stops if an excluded
     path survived;
  3. if the tree differs from the last public commit, a new commit "Publish <sha>: <subject>" is
     created on `public-master`, whose parent is the previous public commit (the first run starts
     from `public/master`).

Pushing stays a manual decision: `git push public public-master:master`. The pre-push hook in
tools/pre-push refuses any other branch aimed at the public remote.

Usage:
  python tools/publish_public.py           build the cut and report
  python tools/publish_public.py --check   only list what the cut would drop and verify it
"""
import fnmatch
import os
import subprocess
import sys
import tempfile

ROOT = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True,
                      check=True).stdout.strip()
MANIFEST = os.path.join(ROOT, 'public_exclude.txt')
BRANCH = 'refs/heads/public-master'
UPSTREAM = 'refs/remotes/public/master'


def git(*args, env=None, check=True):
    r = subprocess.run(['git', *args], cwd=ROOT, capture_output=True, text=True, env=env, check=False)
    if check and r.returncode != 0:
        sys.exit(f'git {" ".join(args)} failed: {r.stderr.strip()}')
    return r.stdout.strip()


def patterns():
    out = []
    for line in open(MANIFEST, encoding='utf-8'):
        line = line.split('#', 1)[0].strip()
        if line:
            out.append(line)
    return out


def excluded(path, pats):
    for p in pats:
        if p.endswith('/**') and (path.startswith(p[:-2]) or path == p[:-3]):
            return True
        if fnmatch.fnmatchcase(path, p):
            return True
    return False


def ref_exists(ref):
    return subprocess.run(['git', 'rev-parse', '--verify', '--quiet', ref], cwd=ROOT,
                          capture_output=True).returncode == 0


def main():
    pats = patterns()
    head = git('rev-parse', 'HEAD')
    files = git('ls-tree', '-r', '--name-only', '-z', 'HEAD').split('\0')
    drop = [f for f in files if f and excluded(f, pats)]
    print(f'HEAD {head[:8]}: {len(files)} tracked files, {len(drop)} stay internal')
    for p in pats:
        n = sum(1 for f in drop if excluded(f, [p]))
        print(f'  {p:12} {n} files')
        if n == 0:
            print(f'  warning: pattern {p!r} matches nothing (stale rule?)')

    # build the public tree in a throwaway index
    with tempfile.TemporaryDirectory() as tmp:
        env = dict(os.environ, GIT_INDEX_FILE=os.path.join(tmp, 'index'))
        git('read-tree', 'HEAD', env=env)
        for i in range(0, len(drop), 200):
            git('rm', '--cached', '--quiet', '--', *drop[i:i + 200], env=env)
        tree = git('write-tree', env=env)

    survivors = [f for f in git('ls-tree', '-r', '--name-only', '-z', tree).split('\0') if f and excluded(f, pats)]
    if survivors:
        sys.exit(f'refusing to publish: excluded paths survived the cut: {survivors[:10]}')
    print(f'public tree {tree[:8]} verified: nothing listed in public_exclude.txt survives')
    if '--check' in sys.argv:
        return

    parent = BRANCH if ref_exists(BRANCH) else (UPSTREAM if ref_exists(UPSTREAM) else None)
    if parent is None:
        sys.exit('no public history found: fetch the public remote first (git fetch public)')
    if ref_exists(BRANCH) and ref_exists(UPSTREAM) and subprocess.run(
            ['git', 'merge-base', '--is-ancestor', UPSTREAM, BRANCH], cwd=ROOT).returncode != 0:
        sys.exit('public/master has commits that public-master lacks (someone pushed to the public '
                 'repository directly): reconcile before publishing')
    if git('rev-parse', f'{parent}^{{tree}}') == tree:
        print('the public cut is unchanged since the last publish: nothing to do')
        return
    subject = git('log', '-1', '--format=%s', 'HEAD')
    msg = f'Publish {head[:8]}: {subject}'
    commit = git('commit-tree', tree, '-p', git('rev-parse', parent), '-m', msg)
    git('update-ref', BRANCH, commit)
    print(f'public-master -> {commit[:8]} "{msg}"')
    print('to publish: git push public public-master:master')


if __name__ == '__main__':
    main()
