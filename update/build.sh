#/bin/bash

FILE=$1

echo ${FILE%.*}
exit 0

echo "$FILE" 1>&2
cp "$FILE" "$FILE.min.js"



exit 0
