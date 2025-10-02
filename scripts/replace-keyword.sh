#!/usr/bin/env sh

set -e

target=data
grep -Pnrwi "COMMENT.*(?<!(?:@code ))$1\b.*" $target

read -p "Do you want to wrap that keyword into a code tag? (y/n) " answer
if [ "$answer" != "y" ]; then
  exit
fi

find $target -type f -exec perl -pi -e "s/(COMMENT.*)(?<!\@code )$1\b(.*)/\$1\{\@code $1}\$2/gi" {} +
git add "$target/*.mapping"

echo "Done!"
