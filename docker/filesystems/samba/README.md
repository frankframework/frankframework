# Introduction

Samba is the standard Windows interoperability suite of programs for Linux and Unix.

## Run Samba Docker Compose File.
docker-compose up -d --build

## Set password for user wearefrank
By default, we have created a user called wearefrank for samba container, now, we need to set the password.

The password must be: **pass_123**

- docker exec -it samba smbpasswd -a wearefrank 

## Reference

* https://www.samba.org/samba/what_is_samba.html
* https://hub.docker.com/r/dperson/samba/4
