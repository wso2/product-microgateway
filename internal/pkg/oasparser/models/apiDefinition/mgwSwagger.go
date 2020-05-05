package apiDefinition

type MgwSwagger struct {
	Id               string `json:"id,omitempty"`
	SwaggerVersion   string `json:"swagger,omitempty"`
	Description      string `json:"description,omitempty"`
	Title            string `json:"title,omitempty"`
	Version          string `json:"version,omitempty"`
	BasePath         string `json:"basePath,omitempty"`
	VendorExtensible map[string]interface{}
	Resources        []Resource
	//Consumes            []string                    `json:"consumes,omitempty"`
	//Produces            []string                    `json:"produces,omitempty"`
	//Schemes             []string                    `json:"schemes,omitempty"`
	//info                *spec.Info                       `json:"info,omitempty"`
	//Host                string                      `json:"host,omitempty"`
	//Paths               *spec.Paths                      `json:"paths"`
	//Definitions         spec.Definitions            `json:"definitions,omitempty"`
	//Parameters          map[string]spec.Parameter   `json:"parameters,omitempty"`
	//Responses           map[string]spec.Response    `json:"responses,omitempty"`
	//SecurityDefinitions spec.SecurityDefinitions    `json:"securityDefinitions,omitempty"`
	//Security            []map[string][]string       `json:"security,omitempty"`
	//tags                []spec.Tag                  `json:"tags,omitempty"`
	//ExternalDocs        *spec.ExternalDocumentation `json:"externalDocs,omitempty"`

}

type Resource struct {
	Context          string
	Rtype            string
	Description      string
	Consumes         []string
	Schemes          []string
	Tags             []string
	Summary          string
	ID               string
	Security         []map[string][]string
	VendorExtensible map[string]interface{}
	//produces     []string
	//externalDocs *ExternalDocumentation
	//deprecated   bool
	//parameters   []Parameter
	//responses    *Responses
}

type Endpoint struct {
	Url     []string
	UrlType string
}
