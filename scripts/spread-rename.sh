#!/usr/bin/env sh

set -e

target=$1
: ${target:=HEAD}

git log --oneline -n 1 $target
read -p "Do you want to spread that rename on this commit? (y/n) " answer
if [ "$answer" != "y" ]; then
  exit
fi

. $(dirname "$0")/versions.sh

origin_branch="$(git symbolic-ref HEAD 2>/dev/null)" || exit
origin_branch=${origin_branch##refs/heads/}

git commit --fixup "reword:$target"
reword_commit=$(git rev-parse HEAD)
git rebase $target^^ --autosquash

spread=false
for version in "${versions[@]}"
do
  if [ "$spread" = true ]; then
    echo "Apply rename to $version..."
    git switch $version -q
    git cherry-pick $reword_commit --empty keep
    git rebase $target^^ --autosquash
  fi
  if [ "$version" = "$origin_branch" ]; then
    spread=true
  fi
done

git switch $origin_branch

echo "Done!"
