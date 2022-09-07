#!/bin/bash

if [ $# -ne 2 ];
then
  echo "
Usage:  $0 mail parityAddress
Sample: $0 mail@mail.com http://localhost:8547
";
  exit -1;
fi

MAIL=$1
ADDRESS=$2
RESPONSE=`curl --silent -X GET $ADDRESS/api/health`
STATUS=${RESPONSE:2:2}
DATE=`date '+%Y-%m-%d %H:%M:%S'`

if [ "$RESPONSE" == "" ];
then
 echo "Parity is down or unreachable"
 RESPONSE="{\"Down\":{\"status\":\"Down\",\"message\":\"Parity is down or unreachable\"}}"
fi

echo "$DATE: $RESPONSE"

if [ "$STATUS" != "Ok" ];
then
  echo "Parity missbehaving, reporting"
  echo "$RESPONSE" | mail -s "Parity health check failed" $MAIL
  echo "Email sent"
fi




