local importedLib = import 'ErrorMessageFormatters/imported.libsonnet';
{
	"errorId": importedLib.makeId(),
	"error": payload.errorMessage.message,
	"messageId": payload.errorMessage.originalMessage.messageId
}
