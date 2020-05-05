package apiDefinition

import "github.com/getkin/kin-openapi/openapi3"

func (swagger *MgwSwagger) SetInfoOpenApi(swagger3 openapi3.Swagger) {
	//swagger.Id = swagger3.
	swagger.SwaggerVersion = swagger3.OpenAPI
	//swagger.Info = swagger3.Info
	//swagger.BasePath = swagger3.Servers
	swagger.Description = swagger3.Info.Description
	swagger.Title = swagger3.Info.Title
	swagger.Version = swagger3.Info.Version
	swagger.VendorExtensible = swagger3.Extensions
	swagger.Resources = SetResourcesOpenApi3(swagger3)
}

func setOperationOpenApi(context string, rtype string, operation *openapi3.Operation) Resource {
	var resource Resource
	resource = Resource{
		Context: context,
		Rtype:   rtype,
		ID:      operation.OperationID,
		Summary: operation.Summary,
		//Schemes: operation.,
		Tags: operation.Tags,
		//Security: operation.Security.,
		VendorExtensible: operation.Extensions}
	return resource
}

func GetResources(swagger MgwSwagger) []Resource {
	return swagger.Resources
}

func SetResourcesOpenApi3(openApi openapi3.Swagger) []Resource {
	var resources []Resource

	for contxt, pathItem := range openApi.Paths {

		var resource Resource
		if pathItem.Get != nil {
			resource = setOperationOpenApi(contxt, "get", pathItem.Get)
		} else if pathItem.Post != nil {
			resource = setOperationOpenApi(contxt, "post", pathItem.Post)
		} else if pathItem.Put != nil {
			resource = setOperationOpenApi(contxt, "put", pathItem.Put)
		} else if pathItem.Delete != nil {
			resource = setOperationOpenApi(contxt, "delete", pathItem.Delete)
		} else if pathItem.Head != nil {
			resource = setOperationOpenApi(contxt, "head", pathItem.Head)
		} else if pathItem.Patch != nil {
			resource = setOperationOpenApi(contxt, "patch", pathItem.Patch)
		} else {
			//resource = setOperation(contxt,"get",pathItem.Get)
		}

		resources = append(resources, resource)
	}
	return resources
}
