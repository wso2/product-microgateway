package controlplane

//Client credentials of app client
type Client struct {
	ClientID     string `json:"clientId"`
	ClientSecret string `json:"clientSecret"`
}

//Token access token
type Token struct {
	AccessToken     string `json:"access_token"`
}

//Conf configurations for app client
type Conf struct {
	Username            string
	Password            string
	ClientRegEP         string
	TokenEP             string
	SkipSSLVerification bool
	Owner               string
	GrantType           string
	ClientName          string
}
