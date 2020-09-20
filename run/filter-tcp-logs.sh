#!/bin/bash

dir=.


if [[ "$#" == 1 && -d "$1" ]] ; then
  dir="$1"
elif [ "$#" -ge 1 ]; then
  echo "Usage: $0 <existing-directory>"
  exit 1
fi

datepat="^....-..-..T..:..:.........Z: up:"
grep -a "$datepat" $dir/*tcp*log | \
  sed -e "s/: .*//;s/log:/log\t/" | sort -u|\
while read line ; do
  file=$(echo "$line"|cut -f1)
  pat=$(echo "$line"|cut -f2)
  type=$(echo "$file"|sed -e "s#.*/##;s/.tcp.log$//;s/[0-9]*-//")
  delims=$(grep -a "$datepat" "$file" | grep -v stopped | grep -A1 "$pat")
  n=$(printf "%s\n" "$delims" | grep "$datepat"|wc -l)
  endpat=ENDOFBLOCKS
  if [ "$n" -gt 1 ] ; then
    endpat=$(printf "%s\n" "$delims" | tail -1|sed -e "s/: .*//")
  fi

#  echo $pat
#  echo $endpat
#  echo
#  continue


  grep -A150 -a "^$pat" "$file" | \
    sed -n "/^$pat/,/$endpat/p;/$endpat/q" | \
    egrep -i -v -E -e "^(X-|Cache|Pragma|Expire|Connection:|\
      |Keep-Alive|Upgrade|DNT|Accept|Referer|Transfer|Sec-|[[:space:]]*$|Content-Length: 0|Vary|Last-Modified|0)" | \
    sed -e "s/$pat/\n$type\n$pat\n/;s/TCP Forwarding //;/^: up:$/d;/ stopped.$/d;s/.*: down:/---->/" |\
    egrep -v "(^: up:$|$datepat)"
done
