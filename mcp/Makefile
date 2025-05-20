# Variables
PROJECT_ROOT := $(realpath $(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
BINARY_NAME := transform-mcp
GO := go
GOFMT := gofmt
GOVET := go vet
GOTEST := go test
GOLINT := golangci-lint
GOCOV := go tool cover
BUILD_DIR := build

# Source files
SRC := $(shell find . -name "*.go" -not -path "./vendor/*")
PKGS := $(shell go list ./... | grep -v /vendor/)

# Set build options
BUILD_OPTS := -v

# Set test options
TEST_OPTS := -v -race

.PHONY: all clean test fmt lint vet coverage help

# Default target
all: lint test build-linux build-linux-arm build-darwin

build: clean test build-linux build-linux-arm build-darwin

build-linux:
	mkdir -p $(BUILD_DIR)/linux
	GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -ldflags "-X main.version=$(BUILD_VERSION)" \
	-o $(BUILD_DIR)/linux/transform-mcp $(PROJECT_ROOT)/main.go

build-linux-arm:
	mkdir -p $(BUILD_DIR)/linux-arm
	GOOS=linux GOARCH=arm CGO_ENABLED=0 go build -ldflags "-X main.version=$(BUILD_VERSION)" \
	-o $(BUILD_DIR)/linux-arm/transform-mcp $(PROJECT_ROOT)/main.go

build-darwin:
	mkdir -p $(BUILD_DIR)/darwin
	GOOS=darwin GOARCH=amd64 CGO_ENABLED=0 go build -ldflags "-X main.version=$(BUILD_VERSION)" \
	-o $(BUILD_DIR)/darwin/transform-mcp $(PROJECT_ROOT)/main.go

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@rm -rf $(BUILD_DIR)
	@rm -f coverage.out

# Run tests
test:
	@echo "Running tests..."
	$(GOTEST) $(TEST_OPTS) ./...

# Run tests with coverage report
coverage:
	@echo "Running tests with coverage..."
	@$(GOTEST) -coverprofile=coverage.out ./...
	@$(GOCOV) -func=coverage.out
	@$(GOCOV) -html=coverage.out -o coverage.html
	@echo "Coverage report generated in coverage.html"

# Run gofmt
fmt:
	@echo "Running gofmt..."
	@$(GOFMT) -w -s $(SRC)

# Run go vet
vet:
	@echo "Running go vet..."
	@$(GOVET) ./...

# Show help
help:
	@echo "Available targets:"
	@echo "  all        : Run lint, test, and build"
	@echo "  build      : Build the application"
	@echo "  clean      : Clean build artifacts"
	@echo "  test       : Run tests"
	@echo "  coverage   : Run tests with coverage report"
	@echo "  fmt        : Run gofmt"
	@echo "  vet        : Run go vet"
	@echo "  help       : Show this help message"
