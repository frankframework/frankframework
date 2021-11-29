#!/bin/sh
#
# entrypoint.sh
#
# database initializing entrypoint.sh


ORIGINAL_ENTRYPOINT=/home/oracle/setup/dockerInit.sh
ORIGINAL_SETUP_SCRIPT=/home/oracle/setup/configDBora.sh


QUERY_COMMAND="sqlplus / as sysdba"
QUERY_PATH=/home/oracle/create_user.sql

appendSetup() {
  echo "appending iaf init to script $ORIGINAL_SETUP_SCRIPT"

cat << !! >> $ORIGINAL_SETUP_SCRIPT

  echo "do iaf setup"
  echo "$QUERY_COMMAND $QUERY_PATH"
  $QUERY_COMMAND << EOF
  $(cat $QUERY_PATH)
EOF
  exitcode=\$?
  echo "iaf setup done, exitcode=\$exitcode"
!!
}

appendSetup

$ORIGINAL_ENTRYPOINT
