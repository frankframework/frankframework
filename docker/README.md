# Introduction

Samba : the standard Windows interoperability suite of programs for Linux and Unix.
This container aims to run a Samba server registered as a member of a given Active Directory domain.

# Start
To run our docker-compose file, firts, we need to create an environment variable called HOST_IP with your local IP
Example
1.- For Linux: 
Open a terminal and run this command: export HOST_IP=<YOUR_IP>
2.- For Windows 10
You can find it here: * https://superuser.com/questions/949560/how-do-i-set-system-environment-variables-in-windows-10


# Run Samba Docker Compose File.
docker-compose up -d --build

# Set wearefrank Samba user password
By default, we created wearefrank user for "samba server" container, now, we need to set the password.
The password must be: pass_123

- docker exec -it samba smbpasswd -a wearefrank 

# Reference

* https://wiki.debian.org/AuthenticatingLinuxWithActiveDirectory
* https://github.com/docker/docker/issues/12084
* http://www.enterprisenetworkingplanet.com/windows/article.php/3849061/Use-Samba-With-Windows-7-Clients.htm
* https://wiki.samba.org/index.php/Setting_up_a_Share_Using_Windows_ACLs