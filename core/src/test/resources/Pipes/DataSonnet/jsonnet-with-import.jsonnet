local importedLib1 = import '/Pipes/DataSonnet/imported1.ds';
local importedLib2 = import '/Pipes/DataSonnet/imported2.libsonnet';

{
"greetings": payload.greetings,
"uid": userData.userId,
"uname": userData.name,
"foo": importedLib1.toInt('123'),
"bar": importedLib2.uppercase('bar')
}
