function f2(x,y){
	var result = x + y;
	return "" + result;
}

function f4(x,y) {
	var a = x * 5;
	var b = y * 2;

	log(a);
	log(b);

	return a-b;
}

function f5(x, y){
	var a = x * y;
	var b = echoFunction(a);

	return b;
}