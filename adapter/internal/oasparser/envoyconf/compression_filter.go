/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"errors"
	"fmt"
	"strings"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	gzip_compressor "github.com/envoyproxy/go-control-plane/envoy/extensions/compression/gzip/compressor/v3"
	compressor3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/compressor/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"google.golang.org/protobuf/runtime/protoiface"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func getGzipConfigurations(config config.Config) *gzip_compressor.Gzip {
	var compressionStrategy string

	if v, ok := config.Envoy.Filters.Compression.LibraryProperties["compressionStrategy"].(string); ok {
		compressionStrategy = strings.ToLower(v)
	}

	var gzipConf = &gzip_compressor.Gzip{
		MemoryLevel:         wrapperspb.UInt32(uint32(3)),
		WindowBits:          wrapperspb.UInt32(uint32(12)),
		CompressionLevel:    getGzipCompressionLevel(9),
		CompressionStrategy: getGzipCompressionStrategy("defaultStrategy"),
		ChunkSize:           wrapperspb.UInt32(uint32(4096)),
	}
	memoryLevel, err1 := getUInt32Value(config.Envoy.Filters.Compression.LibraryProperties["memoryLevel"])
	if memoryLevel != 0 {
		gzipConf.MemoryLevel = wrapperspb.UInt32(memoryLevel)
	} else {
		logger.LoggerXds.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while parsing the gzip configuration value for the memory level: %v", err1.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 2235,
		})
	}
	windowBits, err2 := getUInt32Value(config.Envoy.Filters.Compression.LibraryProperties["windowBits"])
	if windowBits != 0 && err2 == nil {
		gzipConf.WindowBits = wrapperspb.UInt32(windowBits)
	} else {
		logger.LoggerXds.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while parsing the gzip configuration value for the window bits: %v", err2.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 2236,
		})
	}
	compressionLevel, err3 := getUInt32Value(config.Envoy.Filters.Compression.LibraryProperties["compressionLevel"])
	if compressionLevel != 0 && err3 == nil {
		gzipConf.CompressionLevel = getGzipCompressionLevel(compressionLevel)
	} else {
		logger.LoggerXds.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while parsing the gzip configuration value for the compression level: %v", err3.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 2237,
		})
	}
	chunkSize, err4 := getUInt32Value(config.Envoy.Filters.Compression.LibraryProperties["chunkSize"])
	if chunkSize != 0 && err4 == nil {
		gzipConf.ChunkSize = wrapperspb.UInt32(chunkSize)
	} else {
		logger.LoggerXds.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while parsing the gzip configuration value for the chunk size: %v", err4.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 2238,
		})
	}
	gzipConf.CompressionStrategy = getGzipCompressionStrategy(compressionStrategy)
	logger.LoggerAPI.Debug("gzip configuration values parsed successfully.")
	return gzipConf
}

func getCompressorFilter() (*hcmv3.HttpFilter, error) {
	configRead, _ := config.ReadConfigs()
	var responseDirectionContentTypes []string
	var requestDirectionContentTypes []string
	var libraryConfig protoiface.MessageV1

	for _, val := range configRead.Envoy.Filters.Compression.ResponseDirection.ContentType {
		responseDirectionContentTypes = append(responseDirectionContentTypes, val)
	}
	for _, val := range configRead.Envoy.Filters.Compression.RequestDirection.ContentType {
		requestDirectionContentTypes = append(requestDirectionContentTypes, val)
	}

	compressionLibrary := strings.ToLower(configRead.Envoy.Filters.Compression.Library)
	if compressionLibrary == "gzip" {
		libraryConfig = getGzipConfigurations(*configRead)
	}
	marshalldConfig, err := ptypes.MarshalAny(libraryConfig)
	if err != nil {
		return nil, errors.New("Error occurred while marshalling compression library configurations. " + err.Error())
	}
	conf := &compressor3.Compressor{
		ResponseDirectionConfig: &compressor3.Compressor_ResponseDirectionConfig{
			CommonConfig: &compressor3.Compressor_CommonDirectionConfig{
				MinContentLength: &wrapperspb.UInt32Value{
					Value: uint32(configRead.Envoy.Filters.Compression.ResponseDirection.MinimumContentLength),
				},
				ContentType: responseDirectionContentTypes,
				Enabled: &corev3.RuntimeFeatureFlag{
					DefaultValue: &wrapperspb.BoolValue{
						Value: configRead.Envoy.Filters.Compression.ResponseDirection.Enabled,
					},
					RuntimeKey: "response_compressor_enabled",
				},
			},
			DisableOnEtagHeader: configRead.Envoy.Filters.Compression.ResponseDirection.EnableForEtagHeader,
		},
		RequestDirectionConfig: &compressor3.Compressor_RequestDirectionConfig{
			CommonConfig: &compressor3.Compressor_CommonDirectionConfig{
				Enabled: &corev3.RuntimeFeatureFlag{
					DefaultValue: &wrapperspb.BoolValue{
						Value: configRead.Envoy.Filters.Compression.RequestDirection.Enabled,
					},
					RuntimeKey: "request_compressor_enabled",
				},
				MinContentLength: &wrapperspb.UInt32Value{
					Value: uint32(configRead.Envoy.Filters.Compression.RequestDirection.MinimumContentLength),
				},
				ContentType: requestDirectionContentTypes,
			},
		},
		CompressorLibrary: &corev3.TypedExtensionConfig{
			Name:        "text_optimized",
			TypedConfig: marshalldConfig,
		},
	}
	compressorConfig, err := ptypes.MarshalAny(conf)
	if err != nil {
		return nil, errors.New("Error occurred while marshalling compression filter configurations. " + err.Error())
	}
	compressorFilter := hcmv3.HttpFilter{
		Name: compressorFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: compressorConfig,
		},
	}
	logger.LoggerAPI.Debugf("compression filter configured successfully")
	return &compressorFilter, nil
}

func getUInt32Value(s interface{}) (uint32, error) {
	if v, ok := s.(int64); ok {
		return uint32(v), nil
	}
	return 0, errors.New("error occurred while processing gzip configuration value")
}

func getGzipCompressionLevel(v uint32) gzip_compressor.Gzip_CompressionLevel {
	switch v {
	case 1:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_1)
	case 2:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_2)
	case 3:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_3)
	case 4:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_4)
	case 5:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_5)
	case 6:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_6)
	case 7:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_7)
	case 8:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_8)
	case 9:
		return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_9)
	}
	return gzip_compressor.Gzip_CompressionLevel(gzip_compressor.Gzip_COMPRESSION_LEVEL_9)
}

func getGzipCompressionStrategy(s string) gzip_compressor.Gzip_CompressionStrategy {
	switch s {
	case "defaultstrategy":
		return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_DEFAULT_STRATEGY)
	case "gzipfiltered":
		return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_FILTERED)
	case "gziphuffmanonly":
		return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_HUFFMAN_ONLY)
	case "gziprle":
		return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_RLE)
	case "gzipfixed":
		return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_FILTERED)
	}
	return gzip_compressor.Gzip_CompressionStrategy(gzip_compressor.Gzip_DEFAULT_STRATEGY)
}
