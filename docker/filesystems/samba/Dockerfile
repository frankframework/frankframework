FROM stanback/alpine-samba

COPY ./smb.conf /etc/samba/smb.conf
WORKDIR /srv/samba/shared
RUN chmod 777 -R /srv/samba/shared
RUN adduser -D -g '' wearefrank
RUN echo -e "pass_123\npass_123" | smbpasswd -a wearefrank

