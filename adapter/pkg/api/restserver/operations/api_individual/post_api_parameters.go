// Code generated by go-swagger; DO NOT EDIT.

// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package api_individual

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the swagger generate command

import (
	"io"
	"mime/multipart"
	"net/http"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/middleware"
	"github.com/go-openapi/strfmt"
	"github.com/go-openapi/swag"
)

// PostAPIMaxParseMemory sets the maximum size in bytes for
// the multipart form parser for this operation.
//
// The default value is 32 MB.
// The multipart parser stores up to this + 10MB.
var PostAPIMaxParseMemory int64 = 32 << 20

// NewPostAPIParams creates a new PostAPIParams object
//
// There are no default values defined in the spec.
func NewPostAPIParams() PostAPIParams {

	return PostAPIParams{}
}

// PostAPIParams contains all the bound params for the post API operation
// typically these are obtained from a http.Request
//
// swagger:parameters PostAPI
type PostAPIParams struct {

	// HTTP Request Object
	HTTPRequest *http.Request `json:"-"`

	/*Zip archive consisting on exported api configuration

	  Required: true
	  In: formData
	*/
	File io.ReadCloser
	/*Whether to update the API or not. This is used when updating already existing APIs.

	  In: query
	*/
	Overwrite *bool
	/*Preserve Original Provider of the API. This is the user choice to keep or replace the API provider.

	  In: query
	*/
	PreserveProvider *bool
}

// BindRequest both binds and validates a request, it assumes that complex things implement a Validatable(strfmt.Registry) error interface
// for simple values it will use straight method calls.
//
// To ensure default values, the struct must have been initialized with NewPostAPIParams() beforehand.
func (o *PostAPIParams) BindRequest(r *http.Request, route *middleware.MatchedRoute) error {
	var res []error

	o.HTTPRequest = r

	qs := runtime.Values(r.URL.Query())

	if err := r.ParseMultipartForm(PostAPIMaxParseMemory); err != nil {
		if err != http.ErrNotMultipart {
			return errors.New(400, "%v", err)
		} else if err := r.ParseForm(); err != nil {
			return errors.New(400, "%v", err)
		}
	}

	file, fileHeader, err := r.FormFile("file")
	if err != nil {
		res = append(res, errors.New(400, "reading file %q failed: %v", "file", err))
	} else if err := o.bindFile(file, fileHeader); err != nil {
		// Required: true
		res = append(res, err)
	} else {
		o.File = &runtime.File{Data: file, Header: fileHeader}
	}

	qOverwrite, qhkOverwrite, _ := qs.GetOK("overwrite")
	if err := o.bindOverwrite(qOverwrite, qhkOverwrite, route.Formats); err != nil {
		res = append(res, err)
	}

	qPreserveProvider, qhkPreserveProvider, _ := qs.GetOK("preserveProvider")
	if err := o.bindPreserveProvider(qPreserveProvider, qhkPreserveProvider, route.Formats); err != nil {
		res = append(res, err)
	}
	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

// bindFile binds file parameter File.
//
// The only supported validations on files are MinLength and MaxLength
func (o *PostAPIParams) bindFile(file multipart.File, header *multipart.FileHeader) error {
	return nil
}

// bindOverwrite binds and validates parameter Overwrite from query.
func (o *PostAPIParams) bindOverwrite(rawData []string, hasKey bool, formats strfmt.Registry) error {
	var raw string
	if len(rawData) > 0 {
		raw = rawData[len(rawData)-1]
	}

	// Required: false
	// AllowEmptyValue: false

	if raw == "" { // empty values pass all other validations
		return nil
	}

	value, err := swag.ConvertBool(raw)
	if err != nil {
		return errors.InvalidType("overwrite", "query", "bool", raw)
	}
	o.Overwrite = &value

	return nil
}

// bindPreserveProvider binds and validates parameter PreserveProvider from query.
func (o *PostAPIParams) bindPreserveProvider(rawData []string, hasKey bool, formats strfmt.Registry) error {
	var raw string
	if len(rawData) > 0 {
		raw = rawData[len(rawData)-1]
	}

	// Required: false
	// AllowEmptyValue: false

	if raw == "" { // empty values pass all other validations
		return nil
	}

	value, err := swag.ConvertBool(raw)
	if err != nil {
		return errors.InvalidType("preserveProvider", "query", "bool", raw)
	}
	o.PreserveProvider = &value

	return nil
}
