/* helper functions */
/* shorthand version of frequently used dom methods */
function $(/*string*/uid)  			{ return document.getElementById(uid); }
function $t(/*string*/tag) 			{ return document.getElementsByTagName(tag); }

function saveResults(formName, cmd) {
	var confirmed = false;
	if (cmd == "indentWindiff") {
		var confirmed = true;
	} else {
		var msg = "Are you sure you want to overwrite expected result with actual result?";
		confirmed = confirm(msg);
	}
	if (confirmed) {
		window.open("saveResultToFile.jsp?init=true&cmd=" + cmd, "saveResultWindow", 'width=200,height=200,left=400,top=400');
		document.forms[formName].target = "saveResultWindow";
		document.forms[formName]["cmd"].value = cmd
		document.forms[formName].submit();
	}
}

function xmlFormat(elementId) {
	var value = $(elementId).value;
	// Remove all whitespace (spaces, tabs and newlines) except whitespace in content between tags
	var outsideComment = true;
	var outsideTag = true;
	var attributeValueDelimiter = "";
	var tagContent = "";
	for (var i = 0; i < value.length; i++) {
		if (outsideTag && value.charAt(i) == '<' && i + 3 < value.length && value.charAt(i + 1) == '!' && value.charAt(i + 2) == '-' && value.charAt(i + 3) == '-') {
			outsideComment = false;
			tagContent = tagContent + value.charAt(i) + value.charAt(i + 1) + value.charAt(i + 2) + value.charAt(i + 3);
			value = value.substring(0, i) + value.substring(i + 4, value.length);
			i--;
		} else if (outsideTag && !outsideComment && value.charAt(i) == '-' && i + 2 < value.length && value.charAt(i + 1) == '-' && value.charAt(i + 2) == '>') {
			outsideComment = true;
			tagContent = tagContent + value.charAt(i) + value.charAt(i + 1) + value.charAt(i + 2);
			value = value.substring(0, i) + value.substring(i + 3, value.length);
			i--;
		} else if (outsideComment && value.charAt(i) == '<') {
			outsideTag = false;
			tagContent = trim(tagContent);
			value = value.substr(0, i) + tagContent + value.substring(i, value.length);
			i = i + tagContent.length;
			tagContent = "";
		} else if (outsideComment && !outsideTag && value.charAt(i) == '>') {
			outsideTag = true;
		} else if (outsideTag) {
			tagContent = tagContent + value.charAt(i);
			if (i + 1 < value.length) {
				value = value.substring(0, i) + value.substring(i + 1, value.length);
				i--;
			} else {
				value = value.substring(0, i);
			}
		} else if (attributeValueDelimiter == "" && (value.charAt(i) == '\n' || value.charAt(i) == '\r' || value.charAt(i) == '\t' || value.charAt(i) == ' ')) {
			// Normalise spaces around attribute names and values
			if (i + 1 < value.length) {
				var newSpace = " ";
				if (i > 0 && value.charAt(i - 1) == ' ') {
					newSpace = "";
				}
				for (var j = i + 1; newSpace != "" && j < value.length && (value.charAt(j) == '\n' || value.charAt(j) == '\r' || value.charAt(j) == '\t' || value.charAt(j) == ' ' || value.charAt(j) == '=' || value.charAt(j) == '/' || value.charAt(j) == '>'); j++) {
					if (value.charAt(j) == '=' || value.charAt(j) == '/' || value.charAt(j) == '>') {
						newSpace = "";
					}
				}
				for (var j = i - 1; newSpace != "" && j < value.length && (value.charAt(j) == '\n' || value.charAt(j) == '\r' || value.charAt(j) == '\t' || value.charAt(j) == ' ' || value.charAt(j) == '='); j--) {
					if (value.charAt(j) == '=') {
						newSpace = "";
					}
				}
				value = value.substring(0, i) + newSpace + value.substring(i + 1, value.length);
				if (newSpace == "") {
					i--;
				}
			} else {
				value = value.substring(0, i);
			}
		} else if (value.charAt(i) == '\'' || value.charAt(i) == '"') {
			if (attributeValueDelimiter == "") {
				attributeValueDelimiter = value.charAt(i);
			} else {
				attributeValueDelimiter = "";
			}
		}
	}
	// Indent everything
	var indent = 0;
	var tagStart = -1;
	var currentTag = "";
	var previousTag = "";
	var isFirst = true;
	var newlineInComment = false;
	for (var i = 0; i < value.length; i++) {
		if (tagStart != -1) {
			if (currentTag == "OpenTag" || currentTag == "CloseTag" || currentTag == "XmlTag") {
				if (value.charAt(i) == '>') {
					if (value.charAt(i - 1) == '/') {
						currentTag = "OpenCloseTag";
					}
					if (currentTag == "OpenTag") {
						if (!isFirst) {
							if (previousTag == "OpenTag") {
								indent++;
							}
							var whitespace = "";
							for (var j = 0; j < indent; j++) {
								whitespace = whitespace + "  ";
							}
							var add = "\n" +  whitespace;
							value = value.substring(0, tagStart)+ add + value.substring(tagStart);
							i = i + add.length;
						}
					} else if (currentTag == "CloseTag") {
						if (previousTag != "OpenTag") {
							indent--;
							var whitespace = "";
							for (var j = 0; j < indent; j++) {
								whitespace = whitespace + "  ";
							}
							var add = "\n" +  whitespace;
							value = value.substring(0, tagStart) + add + value.substring(tagStart);
							i = i + add.length;
						}
					} else if (currentTag == "OpenCloseTag") {
						if (previousTag == "OpenTag") {
							indent++;
						}
						var whitespace = "";
						for (var j = 0; j < indent; j++) {
							whitespace = whitespace + "  ";
						}
						var add = "\n" +  whitespace;
						value = value.substring(0, tagStart)+ add + value.substring(tagStart);
						i = i + add.length;
					}
					tagStart = -1;
					previousTag = currentTag;
					currentTag = "";
					isFirst = false;
				}
			} else if (currentTag == "CommentTag") {
				if (value.charAt(i) == '>' && value.charAt(i - 1) == '-' && value.charAt(i - 2) == '-' && i > tagStart + 5) {
					if (newlineInComment) {
						value = value.substring(0, tagStart)+ "\n" + value.substring(tagStart);
						i++;
					}
					tagStart = -1;
					currentTag = "";
					newlineInComment = false;
				} else if (value.charAt(i) == '\n' || value.charAt(i) == '\r') {
					newlineInComment = true;
				}
			} else {
				if (value.charAt(i) == '/') {
					currentTag = "CloseTag";
				} else if (value.charAt(i) == '?') {
					currentTag = "XmlTag";
				} else if (value.charAt(i) == '!') {
					currentTag = "CommentTag";
				} else {
					currentTag = "OpenTag";
				}
			}
		} else {
			if (value.charAt(i) == '<') {
				tagStart = i;
			}
		}
	}
	$(elementId).value = value;
}

function jsonFormat(elementId) {
	var value = $(elementId).value;
	var jsonObject = JSON.parse(value);
	$(elementId).value = JSON.stringify(jsonObject, null, 4); //indent with 4 spaces
}

/** Searches for the differences in Result and Expected.
    First it shows the result line and then the expected line with the difference colored.
**/
function showDiffs(elementId, elementIdResult, elementIdExpected) {
	var value = "";
	var valueResult   = $(elementIdResult).value;
	var valueExpected = $(elementIdExpected).value;
	var numberOfCharsResult = 0;
	var numberOfCharsExpected = 0;

	// style should be in a stylesheet... 
	var fontTagStart = "<FONT STYLE=\"color:white; background-color: red\">";
	var fontTagEnd = "</FONT>";

	while (numberOfCharsResult < valueResult.length) {
		var lineResult = readLine(valueResult, numberOfCharsResult);
		var lineExpected = readLine(valueExpected, numberOfCharsExpected);
		numberOfCharsResult   = numberOfCharsResult + lineResult.length;
		numberOfCharsExpected = numberOfCharsExpected + lineExpected.length;
		
		if (lineResult == lineExpected) {
			value = value + escapeChars(lineResult) + "<BR/>";
		} else {
			// show result line
			value = value + "<SPAN ID=\"styleLineExpected\">" + escapeChars(lineExpected) + "</SPAN><BR/>";
			// show expected line
			value = value + "<SPAN ID=\"styleLineResult\"><FONT STYLE=\"color: green\">" ;

			// check each character and color it when it differs.
			var minLength;
			if (lineExpected.length < lineResult.length) {
				minLength = lineExpected.length;
			}
			else {
				minLength = lineResult.length;
			}
			for (var i=0; i < minLength; i++) {
				if ( lineExpected.charAt(i) == lineResult.charAt(i) ) {
					value += escapeChars(lineResult.charAt(i));
				}
				else {
					value += fontTagStart;
					value += escapeChars(lineResult.charAt(i));
					value += fontTagEnd;
				}
			}
			// if lineResult > lineExpected then write and color the remaining chars
			if (lineResult.length > lineExpected.length) {
					value += fontTagStart;
					value += escapeChars( lineResult.substring(minLength) );
					value += fontTagEnd;
			}
			// en een nieuwe regel
			value = value + "</FONT></SPAN><BR/>";

			// Forceer einde while, als enkel firstDiff wil laten zien.
			// numberOfCharsExpected = valueExpected.length;
		}
	}
	
	$(elementId).innerHTML = value;
}

/** Checks the input line on various characters and escapes
    found chars with html-friendly replacements.
**/
function escapeChars(lineInput) {
	/* De snelle/efficiente oplossing werkt alleen in Firefox.
	IE6 vervangt slechts de eerste occurrance. *zucht*
	
	var line = lineInput.replace("<", "&lt;","g");
	line = line.replace(">", "&gt;","g");
	line = line.replace(" " , "&nbsp;","g");
	line = line.replace("\n", "");
	*/

	// ... dus dan maar char voor char...
	var line = "";
	for (var i=0; i < lineInput.length; i++) {
		switch (lineInput.charAt(i)) {
		case "<":
			line += "&lt;";
			break;
		case ">":
			line += "&gt;";
			break;
		case " ":
			line += "&nbsp;";
			break;
		case "\n":
			line += "";
			break;
		case ('&') :
			line += "&amp;";
			break;
		default:
			line += lineInput.charAt(i);
		}			
	}
	
	return line;
}

function readLine(string, i) {
	var line = "";
	while (i < string.length) {
		line = line + string.charAt(i);
		if (string.charAt(i) == '\n' || string.charAt(i) == '\r') {
			if (string.charAt(i) == '\r' && i + 1 < string.length && string.charAt(i + 1) == '\n') {
				line = line + string.charAt(i + 1);
			}
			i = string.length;
		} else {
			i++;
		}
	}
	return line;
}

function isWhitespaceChar(character) {
	if (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
		return true;
	} else {
		return false;
	}
}

function trim(string) {
	var ignore = false;
	for (var i = 0; i < string.length; i++) {
		if (isWhitespaceChar(string.charAt(i)) && !ignore) {
			string = string.substring(i + 1, string.length);
			i--;
		} else {
			ignore = true;
		}
	}
	ignore = false;
	for (var i = string.length - 1; i > 0; i--) {
		if (isWhitespaceChar(string.charAt(i)) && !ignore) {
			string = string.substring(0, i);
		} else {
			ignore = true;
		}
	}
	return string;
}

function resizeElement(id, widthDelta, heightDelta)
{	
	var el = $(id);
	var p = $(id).parentNode;
	var isIE = document.uniqueID;
	
	if (widthDelta == "content" || heightDelta == "content")
	{
		if (isIE)
		{
			if (widthDelta == "content")
				el.parentNode.style.width = (el.scrollWidth > parseInt(el.offsetWidth - 18)) ? parseInt(el.scrollWidth + 20) + "px" : parseInt(el.offsetWidth + 2) + "px";
			if (heightDelta == "content")
				el.parentNode.style.height = el.style.height = (el.scrollHeight > parseInt(el.offsetHeight - 18)) ? parseInt(el.scrollHeight + 21) + "px" : el.offsetHeight + "px";
		}
		else
		{
			if (widthDelta == "content")
				el.parentNode.style.width = (el.scrollWidth > parseInt(el.offsetWidth - 18)) ? parseInt(el.scrollWidth + 20) + "px" : el.offsetWidth + "px";
			if (heightDelta == "content")
				el.parentNode.style.height = (el.scrollHeight > parseInt(el.offsetHeight - 18)) ? parseInt(el.scrollHeight + 21) + "px" : el.offsetHeight + "px";
		}
	}
	else
	{	
		if (isIE)
		{
			el.style.height = (heightDelta != 0) ? parseInt(el.offsetHeight + heightDelta) + "px" : el.offsetHeight + "px";
			p.style.width  = (widthDelta != 0) ? parseInt(p.offsetWidth + widthDelta) + "px" : p.offsetWidth + "px";
		}
		else
		{
			if (heightDelta != 0)
			{
				p.style.height = parseInt(p.offsetHeight + heightDelta) + "px";
				p.style.marginBottom = "40px";
			}
			if (widthDelta != 0)
				p.style.width = parseInt(p.offsetWidth + widthDelta) + "px";
		}
	}
}

function scrollToBottom() {
	window.scrollTo(0, document.body.scrollHeight);
}

function copyContents(elementId)
{
	$(elementId).select();
	document.execCommand('copy');
}

function indentCompare(sources, result)
{
	var s = eval(sources);

	for (var i = 0; i < s.length; i++){
		var elementId = s[i];
		var text = $(elementId).value;

		try{
			if(text.startsWith("<") || text.startsWith(escapeChars("<"))){ // if text is xml
				xmlFormat(elementId);
			} else if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) { // TODO: something smarter for detecting json like text
				jsonFormat(elementId);
			}
		} catch(error){
			var message = "Formatting the content of element ["+elementId+"] is failed: "+error.message;
			alert(message);
		}
	}

	showDiffs(result, s[0], s[1]);
	
	if ($(result).nodeName.toLowerCase() == "pre")
		$(result).style.height = "200px";
}

function addCommands()
{
	var allLinks = $t("a");
	var relLinks = new Array();
	var instr, trg, cmd, params;
	
	for (var i = 0; i < allLinks.length; i++)
		if (allLinks[i].className.indexOf("|") != -1)
			relLinks.push(allLinks[i]);
			
	for (var i = 0; i < relLinks.length; i++)
	{
		relLinks[i].onclick = function()
		{
			instr  = this.className.split("|");
			trg    = instr[0];
			cmd    = instr[1];
			params = instr[2];
	
			switch(cmd)
			{
				case "widthDown"     : resizeElement(trg, -60, 0); break;
				case "widthExact"    : resizeElement(trg, "content", 0); break;
				case "widthUp"       : resizeElement(trg, 60, 0); break;
				case "heightDown"    : resizeElement(trg, 0, -60); break;
				case "heightExact"   : resizeElement(trg, 0, "content"); break;
				case "heightUp"      : resizeElement(trg, 0, 60); break;
				case "copy"          : copyContents(trg); break;
				case "xmlFormat"     : xmlFormat(trg); break;
				case "saveResults"   : saveResults(trg, cmd); break;
				case "indentCompare" : indentCompare(trg, params); break;
				case "indentWindiff" : saveResults(trg, cmd); break;
				default              : void(0);
			}
		}
	}
}

function updateScenarios() {
	document.getElementById("submit").click();
}

function addSynchScrolling()
{
	var allTA = $t("textarea");
	var isIE = document.uniqueID;
	
	function ss(obj)
	{
		if (obj.id.indexOf('Result') != -1)
			$(obj.id.replace(/Result/,'Expected')).scrollTop = obj.scrollTop;
		else
			$(obj.id.replace(/Expected/,'Result')).scrollTop = obj.scrollTop;
	}
	
	for (var i = 0; i < allTA.length; i++)
		if (allTA[i].id.indexOf('ResultBox') != -1 || allTA[i].id.indexOf('ExpectedBox') != -1)
			if (isIE)
				allTA[i].onscroll = function() { ss(this); }
			else
				allTA[i].onmousemove = function() { ss(this); }
}

function init()
{
	addCommands();
	addSynchScrolling();
}

window.onload = init;