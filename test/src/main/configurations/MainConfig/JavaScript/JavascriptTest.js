function main(){
	java.lang.System.out.println("This is the main function");
	return 0;
}

function f1(){
	java.lang.System.out.println("HelloWorld!");
	return 1;
}

function f2(x,y){
	var result = x + y;
	return result;
}

function f3(x,y,z){
	var result = 0;
	if(z) {
		result = x + y;
	}
	return result;
}

//Used to test the performance of Rhino, when x is 100,000 the function takes 2 minutes 15 seconds to finish.
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
	return data.length
}
