useradd -s /bin/bash -d /home/testiaf_user -m -p testiaf_user00 testiaf_user

db2 grant dataaccess on testiaf to user testiaf_user
