package apiDefinition

type Resource struct {
	path          string
	pathtype            string
	description      string
	consumes         []string
	schemes          []string
	tags             []string
	summary          string
	iD               string
	productionUrls   []Endpoint
	sandboxUrls      []Endpoint

	security         []map[string][]string
	vendorExtensible map[string]interface{}
	//produces     []string
	//externalDocs *ExternalDocumentation
	//deprecated   bool
	//parameters   []Parameter
	//responses    *Responses
}

func (resource *Resource) GetProdEndpoints() []Endpoint {
	return resource.productionUrls
}

func (resource *Resource) GetSandEndpoints() []Endpoint {
	return resource.sandboxUrls
}

func (resource *Resource) GetPath() string {
	return resource.path
}

func (resource *Resource) GetId() string {
	return resource.iD
}
