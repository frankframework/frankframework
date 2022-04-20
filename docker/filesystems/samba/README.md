# Introduction

Samba is the standard Windows interoperability suite of programs for Linux and Unix.

## Run Samba Docker Compose File.
docker-compose up -d --build

## Change password for user wearefrank 
By default, the password set to **pass_123** run below command to change it

- docker exec -it samba smbpasswd -a wearefrank 

## Reference

* https://www.samba.org/samba/what_is_samba.html
* https://hub.docker.com/r/dperson/samba/4
