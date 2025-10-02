#!/usr/bin/env sh

set -e

target=data
grep -Enrw "ARG [0-9]+ $1\b" $target

read -p "Do you want to replace that parameter? (y/n) " answer
if [ "$answer" != "y" ]; then
  exit
fi

find $target -type f -exec sed -Ei "s/(ARG [0-9]+) $1\b/\1 $2/" {} +
git add "$target/*.mapping"

echo "Done!"
