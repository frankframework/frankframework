
function changeBg(obj,isOver) {
var color1="#8D0022";
var color2="#b4e2ff";
if (isOver) {
    obj.style.backgroundColor=color1;
    obj.style.color=color2;
} else {
    obj.style.backgroundColor=color2;
    obj.style.color=color1;
}
}

function setAll(theElement,boxname,value) {
 var theForm = theElement.form, z = 0;
 for(z=0; z<theForm.length;z++){
  if(theForm[z].type == 'checkbox' ){
  	theForm[z].checked = value;
  }
 }
}