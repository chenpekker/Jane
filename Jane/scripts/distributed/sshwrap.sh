#!/usr/bin/expect

set timeout -1

if { $argc != 4 } {
    puts "Usage: $argv0 host username password command"
    exit 1
}

set host [lindex $argv 0]
set username [lindex $argv 1]
set password [lindex $argv 2]
set command [lindex $argv 3]

spawn ssh -o StrictHostKeyChecking=no $username@$host "$command"
expect "*?assword:*"

send -- "$password\r"

send -- "\r"
expect eof