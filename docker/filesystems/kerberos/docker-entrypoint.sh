#!/bin/sh


if [ -z ${KRB5_REALM} ]; then
    echo "No KRB5_REALM Provided. Exiting ..."
    exit 1
fi

if [ -z ${KRB5_KDC} ]; then
    echo "No KRB5_KDC Provided. Exting ..."
    exit 1
fi

if [ -z ${KRB5_ADMINSERVER} ]; then
    echo "KRB5_ADMINSERVER provided. Using ${KRB5_KDC} in place."
    KRB5_ADMINSERVER=${KRB5_KDC}
fi

echo "Creating Krb5 Client Configuration"

cat <<EOT > /etc/krb5.conf
[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = 24h
 renew_lifetime = 7d
 forwardable = true
 rdns = false
 default_realm = ${KRB5_REALM}
 udp_preference_limit = 1

 [realms]
 ${KRB5_REALM} = {
    kdc = ${KRB5_KDC}
    admin_server = ${KRB5_ADMINSERVER}
 }
EOT

if [ ! -f "/var/lib/krb5kdc/principal" ]; then

    echo "No Krb5 Database Found. Creating One with provided information"

    if [ -z ${KRB5_PASS} ]; then
        echo "No Password for kdb provided ... Creating One"
        KRB5_PASS=`< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-32};echo;`
        echo "Using Password ${KRB5_PASS}"
    fi

    echo "Creating KDC Configuration"
cat <<EOT > /var/lib/krb5kdc/kdc.conf
[kdcdefaults]
    kdc_listen = 88
    kdc_tcp_listen = 88
    
[realms]
    ${KRB5_REALM} = {
        kadmin_port = 749
        max_life = 12h 0m 0s
        max_renewable_life = 7d 0h 0m 0s
        master_key_type = aes256-cts
        supported_enctypes = aes256-cts:normal aes128-cts:normal
        default_principal_flags = +preauth
    }
    
[logging]
    kdc = FILE:/var/log/krb5kdc.log
    admin_server = FILE:/var/log/kadmin.log
    default = FILE:/var/log/krb5lib.log
EOT

echo "Creating Default Policy - Admin Access to */admin"
echo "*/admin@${KRB5_REALM} *" > /var/lib/krb5kdc/kadm5.acl
echo "*/service@${KRB5_REALM} aci" >> /var/lib/krb5kdc/kadm5.acl

    echo "Creating Temp pass file"
cat <<EOT > /etc/krb5_pass
${KRB5_PASS}
${KRB5_PASS}
EOT

    echo "Creating krb5util database"
    kdb5_util create -r ${KRB5_REALM} < /etc/krb5_pass
    rm /etc/krb5_pass

    echo "Creating Admin Account"
    kadmin.local -q "addprinc -pw ${KRB5_PASS} admin/admin@${KRB5_REALM}"
    kadmin.local -q "addprinc -pw ${KRB5_PASS} wearefrank@${KRB5_REALM}"

fi


/usr/bin/supervisord -c /etc/supervisord.conf
