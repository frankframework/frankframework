{
	"errorCode": ds.jsonpath.select(payload, "$..params[?(@.name=='errorCode')].value")[0],
	"errorMessage": ds.jsonpath.select(payload, "$..params[?(@.name=='errorMessage')].value")[0],
	"origin": payload.errorMessage.location.name
}
