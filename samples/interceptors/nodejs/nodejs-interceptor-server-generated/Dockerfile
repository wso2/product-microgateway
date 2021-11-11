FROM node:16.13-alpine
ARG CERT_DIR=certs

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./
RUN npm install

# Bundle app source
COPY ./ .
COPY $CERT_DIR certs/

EXPOSE 9091
CMD [ "node", "index.js" ]
