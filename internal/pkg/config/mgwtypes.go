package config

// The mgw configuration struct.
type Config struct {
	// The server parameters.
	Server struct {
		IP   string
		Port string
	}

	// TLS configuration
	TLS struct {
		Enabled  bool
		KeyStore string // Key store location
		Alias    string // Alias of the private key
		Password string // Key store alias
	}

	// Server admin user credentials.
	Admin struct {
		User struct {
			UserName string
			Password string
		}
	}
}
