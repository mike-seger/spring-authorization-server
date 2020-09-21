#!/bin/bash

dir=.

if [[ "$#" == 1 && -d "$1" ]] ; then
  dir="$1"
elif [ "$#" -ge 1 ]; then
  echo "Usage: $0 <existing-directory>"
  exit 1
fi

date_pat="^....-..-..T..:..:.........Z: up:"
end_pat="# END --------------------------"
grep -a "$date_pat" "$dir"/*tcp*log | \
  sed -e "s/: .*//;s/log:/log\t/" | sort -u|\
while read -r line ; do
  file=$(echo "$line"|cut -f1)
  start_pat=$(echo "$line"|cut -f2)
  type=$(echo "$file"|sed -e "s#.*/##;s/.tcp.log$//;s/[0-9]*-//")
  grep -A150 -a "^$start_pat" "$file" | \
    sed -n "/^$start_pat/,/$end_pat/p;/$end_pat/q" | \
    grep -i -v -E -e "^(X-|Cache|Pragma|Expire|Connection:|\
      |Keep-Alive|Upgrade|DNT|Accept|Referer|Transfer|Sec-|[[:space:]]*$|Content-Length: 0|Vary|Last-Modified|0)" | \
    sed -e "s/$start_pat/\n$type\n$start_pat\n/;s/TCP Forwarding //;/^: up:$/d;/ stopped.$/d;s/.*: down:/---->/" |\
    grep -E -v "(^: up:$|$date_pat)"
done
