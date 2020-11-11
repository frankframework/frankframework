# Introduction

Samba is the standard Windows interoperability suite of programs for Linux and Unix.

# Start
To run our docker-compose file, firts, we need to create an environment variable called HOST_IP

1.- For Linux: 
  Open a terminal and run this command: export HOST_IP=<YOUR_IP> \
2.- For Windows 10
  You can find it here: * https://superuser.com/questions/949560/how-do-i-set-system-environment-variables-in-windows-10


# Run Samba Docker Compose File.
docker-compose up -d --build

# Set password for user wearefrank
By default, we have created a user called wearefrank for samba container, now, we need to set the password.

The password must be: **pass_123**

- docker exec -it samba smbpasswd -a wearefrank 

# Reference

* https://www.samba.org/samba/what_is_samba.html
* https://hub.docker.com/r/dperson/samba/4
