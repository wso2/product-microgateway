package apiDefinition

import "github.com/go-openapi/spec"

func (swagger *MgwSwagger) SetInfoSwagger(swagger2 spec.Swagger) {
	swagger.Id = swagger2.ID
	swagger.SwaggerVersion = swagger2.Swagger
	swagger.Description = swagger2.Info.Description
	swagger.Title = swagger2.Info.Title
	swagger.Version = swagger2.Info.Version
	swagger.BasePath = swagger2.BasePath
	swagger.VendorExtensible = swagger2.VendorExtensible.Extensions
	swagger.Resources = SetResourcesSwagger(swagger2)
}

func SetResourcesSwagger(swagger2 spec.Swagger) []Resource {
	var resources []Resource
	for contxt, _ := range swagger2.Paths.Paths {
		var pathItem spec.PathItemProps = swagger2.Paths.Paths[contxt].PathItemProps
		var resource Resource
		if pathItem.Get != nil {
			resource = setOperationSwagger(contxt, "get", pathItem.Get)
		} else if pathItem.Post != nil {
			resource = setOperationSwagger(contxt, "post", pathItem.Post)
		} else if pathItem.Put != nil {
			resource = setOperationSwagger(contxt, "put", pathItem.Put)
		} else if pathItem.Delete != nil {
			resource = setOperationSwagger(contxt, "delete", pathItem.Delete)
		} else if pathItem.Head != nil {
			resource = setOperationSwagger(contxt, "head", pathItem.Head)
		} else if pathItem.Patch != nil {
			resource = setOperationSwagger(contxt, "patch", pathItem.Patch)
		} else {
			//resource = setOperation(contxt,"get",pathItem.Get)
		}

		resources = append(resources, resource)
	}
	return resources
}

func setOperationSwagger(context string, rtype string, operation *spec.Operation) Resource {
	var resource Resource
	resource = Resource{
		Context:          context,
		Rtype:            rtype,
		ID:               operation.ID,
		Summary:          operation.Summary,
		Schemes:          operation.Schemes,
		Tags:             operation.Tags,
		Security:         operation.Security,
		VendorExtensible: operation.VendorExtensible.Extensions}
	return resource
}
