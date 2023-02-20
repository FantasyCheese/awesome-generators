package api

import (
	"context"
	_ "embed"
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/getkin/kin-openapi/openapi3filter"
	"github.com/getkin/kin-openapi/routers"
	"github.com/getkin/kin-openapi/routers/gorillamux"
	"net/http"
	"sync"
)

//go:embed spec.json
var specData []byte

var instance *routers.Router
var once sync.Once

func getKinRouter() *routers.Router {
	once.Do(func() {
		doc, _ := openapi3.NewLoader().LoadFromData(specData)
		_ = doc.Validate(context.Background())
		router, _ := gorillamux.NewRouter(doc)

        fileTypes := []string{"image/jpeg", "image/png", "video/mp4"}
        for _, fileType := range fileTypes {
            openapi3filter.RegisterBodyDecoder(fileType, openapi3filter.FileBodyDecoder)
        }

		instance = &router
	})
	return instance
}

func validateRequest(w http.ResponseWriter, r *http.Request) bool {
	route, pathParams, _ := (*getKinRouter()).FindRoute(r)
	err := openapi3filter.ValidateRequest(context.Background(), &openapi3filter.RequestValidationInput{
		Request:    r,
		PathParams: pathParams,
		Route:      route,
		Options:    &openapi3filter.Options{},
	})
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(err.Error()))
		return false
	}
	return true
}
