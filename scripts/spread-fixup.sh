#!/usr/bin/env sh

set -e

target=$1
: ${target:=HEAD}

git log --oneline -n 1 $target
read -p "Do you want to spread that fixup on this commit? (y/n) " answer
if [ "$answer" != "y" ]; then
  exit
fi

. $(dirname "$0")/versions.sh

origin_branch="$(git symbolic-ref HEAD 2>/dev/null)" || exit
origin_branch=${origin_branch##refs/heads/}

git commit --fixup $target
from=$(git rev-parse HEAD)
git rebase $target^^ --autosquash

spread=false
for version in "${versions[@]}"
do
  if [ "$spread" = true ]; then
    echo "Apply fix to $version..."
    git switch $version -q
    before=$(git rev-parse HEAD)
    git cherry-pick $from --empty drop || true
    conflict=$(git rev-parse -q --verify CHERRY_PICK_HEAD || echo "clean")
    if [ "$conflict" != "clean" ]; then
      git status
      read -p "Fix failed to apply cleanly, press enter once conflicts are resolved!"
      git add .
      git cherry-pick --continue --no-edit || git cherry-pick --skip
    fi
    from=$(git rev-parse HEAD)
    if [ "$before" = "$from" ]; then
      echo -e "Fix already applied, skipping next versions (\033[1;30m$version+\033[0m)!"
      break
    fi
    git rebase $target^^ --autosquash
  fi
  if [ "$version" = "$origin_branch" ]; then
    spread=true
  fi
done

git switch $origin_branch

echo "Done!"
