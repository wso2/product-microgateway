FROM golang:1.13-alpine3.11

ENV LANG=C.UTF-8

ENV ENV=${USER_HOME}"/.ashrc"

ENV APP_HOME /go/src/controlPlane

RUN mkdir -p $APP_HOME
WORKDIR $APP_HOME
COPY . .

RUN go mod download
RUN go mod verify

RUN go build -o pilot

CMD ./pilot