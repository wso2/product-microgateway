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
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/err"
	"github.com/wso2/product-microgateway/adapter/pkg/soaputils"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func getErrorResponseMappers() []*hcmv3.ResponseMapper {
	conf, _ := config.ReadConfigs()
	if conf.Adapter.SoapErrorXMLFormatEnabled {
		return append(getSoapErrorResponseMappers(), getErrorResponseMappersJSON()...)
	}
	return getErrorResponseMappersJSON()
}

func getErrorResponseMappersJSON() []*hcmv3.ResponseMapper {
	return []*hcmv3.ResponseMapper{
		genErrorResponseMapperJSON(err.NotFoundCode, err.NotFoundCode, err.NotFoundMessage, err.NotFoundDescription, "NR"),
		genErrorResponseMapperJSON(500, err.UaexCode, err.UaexMessage, err.UaexDecription, "UAEX"),
		genErrorResponseMapperJSON(503, err.UfCode, err.UfMessage, "%LOCAL_REPLY_BODY%", "UF"),
		genErrorResponseMapperJSON(504, err.UtCode, err.UtMessage, "%LOCAL_REPLY_BODY%", "UT"),
		genErrorResponseMapperJSON(503, err.UoCode, err.UoMessage, "%LOCAL_REPLY_BODY%", "UO"),
		genErrorResponseMapperJSON(500, err.UrxCode, err.UrxMessage, "%LOCAL_REPLY_BODY%", "URX"),
		genErrorResponseMapperJSON(500, err.NcCode, err.NcMessage, "%LOCAL_REPLY_BODY%", "NC"),
		genErrorResponseMapperJSON(503, err.UhCode, err.UhMessage, "%LOCAL_REPLY_BODY%", "UH"),
		genErrorResponseMapperJSON(503, err.UrCode, err.UrMessage, "%LOCAL_REPLY_BODY%", "UR"),
		genErrorResponseMapperJSON(503, err.UcCode, err.UcMessage, "%LOCAL_REPLY_BODY%", "UC"),
		genErrorResponseMapperJSON(503, err.LrCode, err.LrMessage, "%LOCAL_REPLY_BODY%", "LR"),
		genErrorResponseMapperJSON(400, err.IhCode, err.IhMessage, "%LOCAL_REPLY_BODY%", "IH"),
		genErrorResponseMapperJSON(500, err.SiCode, err.SiMessage, "%LOCAL_REPLY_BODY%", "SI"),
		genErrorResponseMapperJSON(500, err.DpeCode, err.DpeMessage, "%LOCAL_REPLY_BODY%", "DPE"),
		genErrorResponseMapperJSON(500, err.UpeCode, err.UpeMessage, "%LOCAL_REPLY_BODY%", "UPE"),
		genErrorResponseMapperJSON(500, err.UmsdrCode, err.UmsdrMessage, "%LOCAL_REPLY_BODY%", "UMSDR"),
	}
}

func getSoapErrorResponseMappers() []*hcmv3.ResponseMapper {
	return []*hcmv3.ResponseMapper{
		genSoap12ErrorResponseMapper(err.NotFoundCode, err.NotFoundCode, err.NotFoundMessage, err.NotFoundDescription, "NR"),
		genSoap12ErrorResponseMapper(500, err.UaexCode, err.UaexMessage, err.UaexDecription, "UAEX"),
		genSoap12ErrorResponseMapper(503, err.UfCode, err.UfMessage, "%LOCAL_REPLY_BODY%", "UF"),
		genSoap12ErrorResponseMapper(504, err.UtCode, err.UtMessage, "%LOCAL_REPLY_BODY%", "UT"),
		genSoap12ErrorResponseMapper(503, err.UoCode, err.UoMessage, "%LOCAL_REPLY_BODY%", "UO"),
		genSoap12ErrorResponseMapper(500, err.UrxCode, err.UrxMessage, "%LOCAL_REPLY_BODY%", "URX"),
		genSoap12ErrorResponseMapper(500, err.NcCode, err.NcMessage, "%LOCAL_REPLY_BODY%", "NC"),
		genSoap12ErrorResponseMapper(503, err.UhCode, err.UhMessage, "%LOCAL_REPLY_BODY%", "UH"),
		genSoap12ErrorResponseMapper(503, err.UrCode, err.UrMessage, "%LOCAL_REPLY_BODY%", "UR"),
		genSoap12ErrorResponseMapper(503, err.UcCode, err.UcMessage, "%LOCAL_REPLY_BODY%", "UC"),
		genSoap12ErrorResponseMapper(503, err.LrCode, err.LrMessage, "%LOCAL_REPLY_BODY%", "LR"),
		genSoap12ErrorResponseMapper(400, err.IhCode, err.IhMessage, "%LOCAL_REPLY_BODY%", "IH"),
		genSoap12ErrorResponseMapper(500, err.SiCode, err.SiMessage, "%LOCAL_REPLY_BODY%", "SI"),
		genSoap12ErrorResponseMapper(500, err.DpeCode, err.DpeMessage, "%LOCAL_REPLY_BODY%", "DPE"),
		genSoap12ErrorResponseMapper(500, err.UpeCode, err.UpeMessage, "%LOCAL_REPLY_BODY%", "UPE"),
		genSoap12ErrorResponseMapper(500, err.UmsdrCode, err.UmsdrMessage, "%LOCAL_REPLY_BODY%", "UMSDR"),
		genSoap11ErrorResponseMapper(err.NotFoundCode, err.NotFoundCode, err.NotFoundMessage, err.NotFoundDescription, "NR"),
		genSoap11ErrorResponseMapper(500, err.UaexCode, err.UaexMessage, err.UaexDecription, "UAEX"),
		genSoap11ErrorResponseMapper(503, err.UfCode, err.UfMessage, "%LOCAL_REPLY_BODY%", "UF"),
		genSoap11ErrorResponseMapper(504, err.UtCode, err.UtMessage, "%LOCAL_REPLY_BODY%", "UT"),
		genSoap11ErrorResponseMapper(503, err.UoCode, err.UoMessage, "%LOCAL_REPLY_BODY%", "UO"),
		genSoap11ErrorResponseMapper(500, err.UrxCode, err.UrxMessage, "%LOCAL_REPLY_BODY%", "URX"),
		genSoap11ErrorResponseMapper(500, err.NcCode, err.NcMessage, "%LOCAL_REPLY_BODY%", "NC"),
		genSoap11ErrorResponseMapper(503, err.UhCode, err.UhMessage, "%LOCAL_REPLY_BODY%", "UH"),
		genSoap11ErrorResponseMapper(503, err.UrCode, err.UrMessage, "%LOCAL_REPLY_BODY%", "UR"),
		genSoap11ErrorResponseMapper(503, err.UcCode, err.UcMessage, "%LOCAL_REPLY_BODY%", "UC"),
		genSoap11ErrorResponseMapper(503, err.LrCode, err.LrMessage, "%LOCAL_REPLY_BODY%", "LR"),
		genSoap11ErrorResponseMapper(400, err.IhCode, err.IhMessage, "%LOCAL_REPLY_BODY%", "IH"),
		genSoap11ErrorResponseMapper(500, err.SiCode, err.SiMessage, "%LOCAL_REPLY_BODY%", "SI"),
		genSoap11ErrorResponseMapper(500, err.DpeCode, err.DpeMessage, "%LOCAL_REPLY_BODY%", "DPE"),
		genSoap11ErrorResponseMapper(500, err.UpeCode, err.UpeMessage, "%LOCAL_REPLY_BODY%", "UPE"),
		genSoap11ErrorResponseMapper(500, err.UmsdrCode, err.UmsdrMessage, "%LOCAL_REPLY_BODY%", "UMSDR"),
	}
}

func genErrorResponseMapperJSON(statusCode uint32, errorCode int32, message string, description string, flag string) *hcmv3.ResponseMapper {
	errorMsgMap := make(map[string]*structpb.Value)
	errorMsgMap["code"] = structpb.NewStringValue(strconv.FormatInt(int64(errorCode), 10))
	errorMsgMap["message"] = structpb.NewStringValue(message)
	errorMsgMap["description"] = structpb.NewStringValue(description)

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			// TODO: (VirajSalaka) Decide if the status code needs to be checked in addition to flags
			FilterSpecifier: &access_logv3.AccessLogFilter_ResponseFlagFilter{
				ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
					Flags: []string{flag},
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

func genSoap12ErrorResponseMapper(statusCode uint32, errorCode int32, message string, description string, flag string) *hcmv3.ResponseMapper {
	msg, _ := soaputils.GenarateSoapFaultMessage(soap12ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_AndFilter{
				AndFilter: &access_logv3.AndFilter{
					Filters: []*access_logv3.AccessLogFilter{
						{
							FilterSpecifier: &access_logv3.AccessLogFilter_HeaderFilter{
								HeaderFilter: &access_logv3.HeaderFilter{
									Header: &envoy_config_route_v3.HeaderMatcher{
										Name: contentTypeHeaderName,
										HeaderMatchSpecifier: &envoy_config_route_v3.HeaderMatcher_ExactMatch{
											ExactMatch: contenttypeHeaderSoap,
										},
									},
								},
							},
						},
						{
							FilterSpecifier: &access_logv3.AccessLogFilter_ResponseFlagFilter{
								ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
									Flags: []string{flag},
								},
							},
						},
					},
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
			ContentType: contenttypeHeaderSoap,
		},
	}
	return mapper
}

func genSoap11ErrorResponseMapper(statusCode uint32, errorCode int32, message string, description string, flag string) *hcmv3.ResponseMapper {
	msg, _ := soaputils.GenarateSoapFaultMessage(soap11ProtocolVersion, message, description, strconv.Itoa(int(errorCode)))

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_AndFilter{
				AndFilter: &access_logv3.AndFilter{
					Filters: []*access_logv3.AccessLogFilter{
						{
							FilterSpecifier: &access_logv3.AccessLogFilter_HeaderFilter{
								HeaderFilter: &access_logv3.HeaderFilter{
									Header: &envoy_config_route_v3.HeaderMatcher{
										Name: contentTypeHeaderName,
										HeaderMatchSpecifier: &envoy_config_route_v3.HeaderMatcher_ExactMatch{
											ExactMatch: contentTypeHeaderXML,
										},
									},
								},
							},
						},
						{
							FilterSpecifier: &access_logv3.AccessLogFilter_HeaderFilter{
								HeaderFilter: &access_logv3.HeaderFilter{
									Header: &envoy_config_route_v3.HeaderMatcher{
										Name: soapActionHeaderName,
										HeaderMatchSpecifier: &envoy_config_route_v3.HeaderMatcher_PresentMatch{
											PresentMatch: true,
										},
									},
								},
							},
						},
						{
							FilterSpecifier: &access_logv3.AccessLogFilter_ResponseFlagFilter{
								ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
									Flags: []string{flag},
								},
							},
						},
					},
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
			ContentType: contentTypeHeaderXML,
		},
	}
	return mapper
}
