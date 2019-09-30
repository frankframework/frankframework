function main(){
	return "0";
}

function f1(){
	var result = 8 - 7;
	return ""+result;
}

function f2(x,y){
	var result = x + y;
	return ""+result;
}

function f3(x,y,z){
	var result = 0;
	if(z) {
		result = x + y;
	}
	return ""+result;
}

function f4(x,y) {
	var a = x * 5;
	var b = y * 2;

	var c = myFunction(a);
	var d = myFunction(b);

	return a == c && b == d;
}

//Used to test the performance.
function performance(x){
	var data = [];
	for (var i = 0; i < x; i++) {
		data[i] = i;
	}

	for (var i = 0; i < data.length; i++) {
		for (var j = 0; j < data.length; j++){
			if (data[i] < data[j]) {
				var tmp = data[i];
				data[i] = data[j];
				data[j] = tmp;
			}
		}
	}
	return "1";
}


