# run utilities

These utilities are a shortcut and debugging enhancements for this: [README.adoc](../samples/boot/oauth2-integration/README.adoc)

## running all applications
`./runall.sh`
logs are written into ../build

*IMPORTANT:* Make sure to modify your `/etc/hosts` described in: [README.adoc](../samples/boot/oauth2-integration/README.adoc)

## logging TCP through tcptunnel (used by runall.sh)
tcptunnel-1.2.0.jar - see https://github.com/mike-seger/java-tcp-tunnel

## TCP Dump

### mac OS
`sudo tcpdump -i lo0 -A -s 0 "portrange 39001-39003" | tee ../build/tcpdump.log`

### Linux
`sudo tcpdump -i lo -A -s 0 "portrange 39001-39003" | tee ../build/tcpdump.log`
