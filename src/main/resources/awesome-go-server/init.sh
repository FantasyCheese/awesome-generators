find . -name "*200_response*" -delete
docker run --volume "$PWD":/usr/src/app -w /usr/src/app golang:1.20 go mod download && go fmt ./api
