/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package envoyconf

import (
	"strconv"

	access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	envoy_config_route_v3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	envoy_type_matcher_v3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/err"
	"github.com/wso2/product-microgateway/adapter/pkg/soaputils"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

type errorResponseDetails struct {
	statusCode  int
	errorCode   int
	message     string
	description string
}

var errorResponseMap map[string]errorResponseDetails

func init() {
	errorResponseMap = map[string]errorResponseDetails{
		"NR":    {404, err.NotFoundCode, err.NotFoundMessage, err.NotFoundDescription},
		"UF":    {503, err.UfCode, err.UfMessage, "%LOCAL_REPLY_BODY%"},
		"UT":    {504, err.UtCode, err.UtMessage, "%LOCAL_REPLY_BODY%"},
		"UO":    {503, err.UoCode, err.UoMessage, "%LOCAL_REPLY_BODY%"},
		"URX":   {500, err.UrxCode, err.UrxMessage, "%LOCAL_REPLY_BODY%"},
		"NC":    {500, err.NcCode, err.NcMessage, "%LOCAL_REPLY_BODY%"},
		"UH":    {503, err.UhCode, err.UhMessage, "%LOCAL_REPLY_BODY%"},
		"UR":    {503, err.UrCode, err.UrMessage, "%LOCAL_REPLY_BODY%"},
		"UC":    {503, err.UcCode, err.UcMessage, "%LOCAL_REPLY_BODY%"},
		"LR":    {503, err.LrCode, err.LrMessage, "%LOCAL_REPLY_BODY%"},
		"IH":    {400, err.IhCode, err.IhMessage, "%LOCAL_REPLY_BODY%"},
		"SI":    {500, err.SiCode, err.SiMessage, "%LOCAL_REPLY_BODY%"},
		"DPE":   {500, err.DpeCode, err.DpeMessage, "%LOCAL_REPLY_BODY%"},
		"UPE":   {500, err.UpeCode, err.UpeMessage, "%LOCAL_REPLY_BODY%"},
		"UMSDR": {500, err.UmsdrCode, err.UmsdrMessage, "%LOCAL_REPLY_BODY%"},
	}
}

func getErrorResponseMappers() []*hcmv3.ResponseMapper {
	responseMappers := []*hcmv3.ResponseMapper{}
	conf, _ := config.ReadConfigs()
	if conf.Adapter.SoapErrorInXMLEnabled {
		for flag, details := range errorResponseMap {
			responseMappers = append(responseMappers,
				genSoap12ErrorResponseMapper(flag, uint32(details.statusCode), int32(details.errorCode), details.message, details.description),
				genSoap11ErrorResponseMapper(flag, uint32(details.statusCode), int32(details.errorCode), details.message, details.description),
			)
		}

		responseMappers = append(responseMappers,
			genSoap12ErrorResponseMapperForExtAuthz(500, err.UaexCode, err.UaexMessage, err.UaexDecription),
		)

		responseMappers = append(responseMappers,
			genSoap11ErrorResponseMapperForExtAuthz(500, err.UaexCode, err.UaexMessage, err.UaexDecription),
		)
	}

	for flag, details := range errorResponseMap {
		responseMappers = append(responseMappers,
			genErrorResponseMapperJSON(flag, uint32(details.statusCode), int32(details.errorCode), details.message, details.description),
		)
	}

	responseMappers = append(responseMappers,
		genExtAuthResponseMapper(genExtAuthFilters(), uint32(500), int32(err.UaexCode), err.UaexMessage, err.UaexDecription),
	)

	return responseMappers
}

func genErrorResponseMapperJSON(flag string, statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {
	errorMsgMap := make(map[string]*structpb.Value)
	errorMsgMap["code"] = structpb.NewStringValue(strconv.FormatInt(int64(errorCode), 10))
	errorMsgMap["message"] = structpb.NewStringValue(message)
	errorMsgMap["description"] = structpb.NewStringValue(description)

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: genResponseFlagFilter(flag),
		},
		StatusCode: wrapperspb.UInt32(statusCode),
		BodyFormatOverride: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_JsonFormat{
				JsonFormat: &structpb.Struct{
					Fields: errorMsgMap,
				},
			},
		},
	}
	return mapper
}

func genSoap12ErrorResponseMapper(flag string, statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {
	msg, _ := soaputils.GenerateSoapFaultMessage(soap12ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))
	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genResponseFlagFilter(flag),
		},
	}

	filters = append(filters, genSoap12Filters()...)
	return genSoapErrorResponseMapper(filters, statusCode, msg, contentTypeHeaderSoap)
}

func genSoap12ErrorResponseMapperForExtAuthz(statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {

	msg, _ := soaputils.GenerateSoapFaultMessage(soap12ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))

	filters := []*access_logv3.AccessLogFilter{}
	filters = append(filters, genSoap12Filters()...)
	filters = append(filters, genExtAuthFilters()...)
	return genSoapErrorResponseMapper(filters, statusCode, msg, contentTypeHeaderSoap)
}

func genSoap11ErrorResponseMapper(flag string, statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {
	msg, _ := soaputils.GenerateSoapFaultMessage(soap11ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))
	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genResponseFlagFilter(flag),
		},
	}

	filters = append(filters, genSoap11Filters()...)
	return genSoapErrorResponseMapper(filters, statusCode, msg, contentTypeHeaderXML)
}

func genSoap11ErrorResponseMapperForExtAuthz(statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {

	msg, _ := soaputils.GenerateSoapFaultMessage(soap11ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))

	filters := []*access_logv3.AccessLogFilter{}
	filters = append(filters, genSoap11Filters()...)
	filters = append(filters, genExtAuthFilters()...)
	return genSoapErrorResponseMapper(filters, statusCode, msg, contentTypeHeaderXML)
}

func genSoapErrorResponseMapper(filters []*access_logv3.AccessLogFilter,
	statusCode uint32, msg, contentTypeHeader string) *hcmv3.ResponseMapper {

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_AndFilter{
				AndFilter: &access_logv3.AndFilter{
					Filters: filters,
				},
			},
		},
		StatusCode: wrapperspb.UInt32(statusCode),
		BodyFormatOverride: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_TextFormatSource{
				TextFormatSource: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: msg,
					},
				},
			},
			ContentType: contentTypeHeader,
		},
	}
	return mapper
}

// genResponseFlagFilter returns a filter, which can be used to filter responses using response flag.
func genResponseFlagFilter(flag string) *access_logv3.AccessLogFilter_ResponseFlagFilter {
	return &access_logv3.AccessLogFilter_ResponseFlagFilter{
		ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
			Flags: []string{flag},
		},
	}
}

// genPresentMatchHeaderFilter returns a header filter specifier, which can be used to check whether the header is present or not.
func genPresentMatchHeaderFilter(headerName string) *access_logv3.AccessLogFilter_HeaderFilter {
	return &access_logv3.AccessLogFilter_HeaderFilter{
		HeaderFilter: &access_logv3.HeaderFilter{
			Header: &envoy_config_route_v3.HeaderMatcher{
				Name: soapActionHeaderName,
				HeaderMatchSpecifier: &envoy_config_route_v3.HeaderMatcher_PresentMatch{
					PresentMatch: true,
				},
			},
		},
	}
}

// genExactMatchHeaderFilter returns a header filter specifer which can be used to check the headers with exact value.
func genExactMatchHeaderFilter(headerName, headerValue string) *access_logv3.AccessLogFilter_HeaderFilter {
	return &access_logv3.AccessLogFilter_HeaderFilter{
		HeaderFilter: &access_logv3.HeaderFilter{
			Header: &envoy_config_route_v3.HeaderMatcher{
				Name: headerName,
				HeaderMatchSpecifier: &envoy_config_route_v3.HeaderMatcher_ExactMatch{
					ExactMatch: headerValue,
				},
			},
		},
	}
}

// genMetadataFilter returns a metadata filter specifer which can be used to check the metadata availability in the ext_authz filter
func genMetadataFilterForExtAuthz() *access_logv3.AccessLogFilter_MetadataFilter {

	return &access_logv3.AccessLogFilter_MetadataFilter{
		MetadataFilter: &access_logv3.MetadataFilter{
			Matcher: &envoy_type_matcher_v3.MetadataMatcher{
				Filter: extAuthzFilterName,
				Value: &envoy_type_matcher_v3.ValueMatcher{
					MatchPattern: &envoy_type_matcher_v3.ValueMatcher_StringMatch{
						StringMatch: &envoy_type_matcher_v3.StringMatcher{
							MatchPattern: &envoy_type_matcher_v3.StringMatcher_Exact{Exact: "Not Matching"},
						},
					},
				},
				Path: []*envoy_type_matcher_v3.MetadataMatcher_PathSegment{{Segment: &envoy_type_matcher_v3.MetadataMatcher_PathSegment_Key{Key: choreoConnectEnforcerReply}}},
			},
			MatchIfKeyNotFound: &wrapperspb.BoolValue{
				Value: true,
			},
		},
	}
}

func genExtAuthResponseMapper(filters []*access_logv3.AccessLogFilter,
	statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {

	errorMsgMap := make(map[string]*structpb.Value)
	errorMsgMap["code"] = structpb.NewStringValue(strconv.FormatInt(int64(errorCode), 10))
	errorMsgMap["message"] = structpb.NewStringValue(message)
	errorMsgMap["description"] = structpb.NewStringValue(description)

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_AndFilter{
				AndFilter: &access_logv3.AndFilter{
					Filters: filters,
				},
			},
		},
		StatusCode: wrapperspb.UInt32(statusCode),
		BodyFormatOverride: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_JsonFormat{
				JsonFormat: &structpb.Struct{
					Fields: errorMsgMap,
				},
			},
		},
	}
	return mapper
}

func genExtAuthFilters() []*access_logv3.AccessLogFilter {

	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genMetadataFilterForExtAuthz(),
		},
		{
			FilterSpecifier: genResponseFlagFilter(uaexCode),
		},
	}

	return filters
}

func genSoap12Filters() []*access_logv3.AccessLogFilter {

	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genExactMatchHeaderFilter(contentTypeHeaderName, contentTypeHeaderSoap),
		},
	}

	return filters
}

func genSoap11Filters() []*access_logv3.AccessLogFilter {

	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genExactMatchHeaderFilter(contentTypeHeaderName, contentTypeHeaderXML),
		},
		{
			FilterSpecifier: genPresentMatchHeaderFilter(soapActionHeaderName),
		},
	}

	return filters
}
