FROM osixia/openldap:1.3.0

ENV LDAP_ORGANISATION="Musobar Media" \
     LDAP_DOMAIN="musobarmedia.com"

COPY bootstrap.ldif /container/service/slapd/assets/config/bootstrap/ldif/50-bootstrap.ldif
ADD certs /container/service/slapd/assets/certs