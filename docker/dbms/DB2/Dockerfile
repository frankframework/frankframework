FROM ibmcom/db2

RUN useradd -s /bin/bash -d /home/testiaf_user -m testiaf_user \
	# Use chpasswd to store the encrypted password
	&& echo testiaf_user:testiaf_user00 | chpasswd 

COPY init_user.* /var/custom/