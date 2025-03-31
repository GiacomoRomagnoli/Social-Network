FROM node:22.13.1-alpine
RUN apk add --no-cache curl
WORKDIR /home/app
COPY  ./build/dist/ /home/app/dist/
COPY ./node_modules/ /home/app/node_modules/
ENTRYPOINT ["node", "/home/app/dist/main/typescript/main.js"]
