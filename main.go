package main

import (
	"crypto/tls"
	"fmt"
	"log"
	"os"

	"github.com/go-ldap/ldap/v3"
)

// UserResult represent LDAP users
type UserResult struct {
	DN            string
	CN            string
	SN            string
	DisplayName   string
	GivenName     string
	Email         string
	Password      string
	MobileNumber  string
	Country       string
	City          string
	Street        string
	StreetAddress string
	Department    string
	JobTitle      string
}

// LDAPToUser convert lda.Entry into UserResult
func LDAPToUser(entries []*ldap.Entry) UserResult {
	var user UserResult
	for _, v := range entries[0].Attributes {
		fmt.Println(v.Name)
		switch v.Name {
		case "dn":
			user.DN = v.Values[0]
		case "cn":
			user.CN = v.Values[0]
		case "sn":
			user.SN = v.Values[0]
		case "displayName":
			user.DisplayName = v.Values[0]
		case "givenName":
			user.GivenName = v.Values[0]
		case "mail":
			user.Email = v.Values[0]
		case "mobile":
			user.MobileNumber = v.Values[0]
		case "userPassword":
			user.Password = v.Values[0]
		case "co":
			user.Country = v.Values[0]
		case "l":
			user.City = v.Values[0]
		case "street":
			user.Street = v.Values[0]
		case "streetaddress":
			user.StreetAddress = v.Values[0]
		case "department":
			user.Department = v.Values[0]
		case "title":
			user.JobTitle = v.Values[0]
		}
	}

	return user
}

var (
	adminUser = "cn=admin,dc=musobarmedia,dc=com"
	adminPass = "test1234"

	username = "rob"
	password = "rob_pass"
)

func main() {
	ld, err := ldap.DialURL("ldap://localhost:389")

	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	defer func() { ld.Close() }()

	// Reconnect with TLS

	cert, err := tls.LoadX509KeyPair("openldap/certs/server.crt", "openldap/certs/server.key")
	if err != nil {
		log.Fatalf("server: loadkeys: %s", err)
		os.Exit(1)
	}

	//create certificate pool from CA
	// certPool := x509.NewCertPool()
	// ca, err := ioutil.ReadFile("openldap/certs/server.crt")
	// if err != nil {
	// 	return fmt.Errorf("cannot load certificate authority : %s", err)
	// }

	// //append the client certificate from the CA
	// if ok := certPool.AppendCertsFromPEM(ca); !ok {
	// 	return errors.New("failed append client cert")
	// }

	// InsecureSkipVerify: true
	// if CA you are using is a self signed SSL
	config := tls.Config{Certificates: []tls.Certificate{cert}, InsecureSkipVerify: true}

	err = ld.StartTLS(&config)
	if err != nil {
		fmt.Println("here..")
	}

	// First bind with a read only user
	err = ld.Bind(adminUser, adminPass)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	// Search for the given username
	searchRequest := ldap.NewSearchRequest(
		"dc=musobarmedia,dc=com",
		ldap.ScopeWholeSubtree, ldap.NeverDerefAliases, 0, 0, false,
		fmt.Sprintf("(&(objectClass=inetOrgPerson)(cn=%s))", username),
		[]string{"dn", "cn", "sn", "mail", "givenname", "displayname", "userpassword"},
		nil,
	)

	sr, err := ld.Search(searchRequest)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	if len(sr.Entries) != 1 {
		fmt.Println("User does not exist or too many entries returned")
		os.Exit(1)
	}

	userdn := sr.Entries[0].DN

	// Bind as the user to verify their password
	err = ld.Bind(userdn, password)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	fmt.Println("login success")

	fmt.Println(LDAPToUser(sr.Entries))
}
