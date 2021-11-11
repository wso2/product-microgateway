FROM golang:1.15.15 AS build
WORKDIR /go/src
COPY go ./go
COPY main.go .

ENV GOROOT=/usr/local/go
ENV GOPATH=$HOME/go
ENV PATH=$PATH:$GOROOT/bin

ENV CGO_ENABLED=0
RUN go get -d -v ./...

RUN go build -a -installsuffix cgo -o swagger .

FROM scratch AS runtime
COPY --from=build /go/src/swagger ./
COPY certs ./certs
EXPOSE 9081/tcp
ENTRYPOINT ["./swagger"]
