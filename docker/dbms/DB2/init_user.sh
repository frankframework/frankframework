#!/bin/bash
# Create user, not setting password yet as useradd requires encrypted password
useradd -s /bin/bash -d /home/testiaf_user -m testiaf_user
# Use chpasswd to store the password, chpasswd encrypts it
echo testiaf_user:testiaf_user00 | chpasswd

# Set DB permissions
/opt/ibm/db2/V11.5/bin/db2 CONNECT TO testiaf user db2inst1 using syspw
/opt/ibm/db2/V11.5/bin/db2 GRANT DBADM,CREATETAB,BINDADD,CONNECT,CREATE_NOT_FENCED_ROUTINE,IMPLICIT_SCHEMA,LOAD,CREATE_EXTERNAL_ROUTINE,QUIESCE_CONNECT,SECADM ON DATABASE TO USER testiaf_user