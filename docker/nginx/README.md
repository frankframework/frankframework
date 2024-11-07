# Nginx (Reverse Proxy)

This folder contains the configuration files for the Nginx reverse proxy.

The reverse proxy can be used to test websockets and cors/csrf protection.

## Usage

To start the Nginx container, run the following command:

```bash
docker-compose up -d
```

The container will proxy to the `host.docker.internal:8080` address, this allows you to proxy the application on the host machine. 
You can run the application on the host machine with Tomcat, Spring or with any other Docker container. (Using the multiple compose files)

The proxy can be accessed on port 80 and 443.

## Configuration

The configuration files are located in the `conf` folder. The `default.conf` file is the main configuration file.

The `default.conf` file contains the following configuration:

- Listens on port 80 and 443
- Uses the `ssl/nginx.crt` and `ssl/nginx.key` files as the SSL certificate and key

## Generate SSL Certificate

To test with certificates it is needed to generate a self-signed certificate.

You can use the following command to generate a self-signed certificate:

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout certs/nginx.key -out certs/nginx.crt -addext "subjectAltName=DNS:dummy.dev,DNS:*.dummy.dev,IP:127.0.0.1"
```
