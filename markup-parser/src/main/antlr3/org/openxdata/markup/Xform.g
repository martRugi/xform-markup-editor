grammar Xform;

@header {
package org.openxdata.markup;
}



@lexer::header {
package  org.openxdata.markup;
}

@parser::members{
	
	public void emitErrorMessage(String msg) {
		System.err.println(msg);
		throw new RuntimeException(msg);
	}
}

@lexer::members{
	
	public String rl(String s){
		if(s == null) return s;
		return s.replaceAll("\n","").trim();
	}
}



study returns [Study result = new Study()]
scope 						{ Study scopeStudy;}
	:	NEWLINE* STUDYNAME		{
						$study::scopeStudy = result;
						result.setName($STUDYNAME.text);
						}
		(frm = form			{result.addForm(frm);})+
		SPACE*
	;

form returns [Form rv]
scope						{Form scopeForm;}
	:	(attrib = ATTRIBUTE)*	FORMNAME 
						{
						rv = new Form($FORMNAME.text);
						$form::scopeForm = rv;
						rv.setStudy($study::scopeStudy);
						if(attrib != null)
						Attrib.addAttributeToForm(rv,attrib.getText(),attrib.getLine());
						} 

		(
		(rpt = repeatQn			{rv.addQuestion(rpt);}
		|txt = txtQn			{rv.addQuestion(txt);}
		|single = singleSelQn		{rv.addQuestion(single);}
		|multi = multipleSelQn		{rv.addQuestion(multi);}
		|dynInstance = dynamicQnInstance{rv.addQuestion(dynInstance);}
		|dynamic = dynamicQn		{dynamic.addQuestionsToForm(rv);}
		|csvImp = csvImport		{csvImp.addQuestionsToForm(rv);}	
		)+
		|(pg = page)+
		)
	;
	
page returns [Page rv = new Page()]
	:	PAGE
		
						{
						rv.setName($PAGE.text);
						($form::scopeForm).addPage(rv);
						rv.setStudy($study::scopeStudy);
						} 

		(rpt = repeatQn			{rv.addQuestion(rpt);}
		|txt = txtQn			{rv.addQuestion(txt);}
		|single = singleSelQn		{rv.addQuestion(single);}
		|multi = multipleSelQn		{rv.addQuestion(multi);}
		|dynInstance = dynamicQnInstance{rv.addQuestion(dynInstance);}
		|dynamic = dynamicQn		{dynamic.addQuestionsToForm(rv);}
		|csvImp = csvImport		{csvImp.addQuestionsToForm(rv);}	
		)+		
	;


repeatQn returns [RepeatQuestion rv = new RepeatQuestion()]
	:	
	(ATTRIBUTE				{Attrib.addAttribute(rv,$ATTRIBUTE.text,$ATTRIBUTE.line);})*
	BEGINREPEATMARKER  			{
						rv.setText($BEGINREPEATMARKER.text);
						rv.setLine($BEGINREPEATMARKER.line);
						rv.setParent($form::scopeForm);
						}
	(
	 rpt = repeatQn				{rv.addQuestion(rpt);}
	|txt = txtQn				{rv.addQuestion(txt);}
	|single = singleSelQn			{rv.addQuestion(single);}
	|multi = multipleSelQn			{rv.addQuestion(multi);}
	|dynInstance = dynamicQnInstance	{rv.addQuestion(dynInstance);}
	|dynamic = dynamicQn			{dynamic.addQuestionsToForm(rv);}
	)+ (ENDREPEATMARKER|LEFTBRACE)
	;

dynamicQn returns [DynamicBuilder rv] 
	:	dyn1 = DYNAMICMARKER		{
						rv = new DynamicBuilder();
						rv.setLine(dyn1.getLine());
						}
		(LINECONTENTS			{rv.appendLine($LINECONTENTS.text);})+ 
		(DYNAMICMARKER|LEFTBRACE)
	;
	
csvImport returns [DynamicBuilder rv = new DynamicBuilder()]
	:	CSVIMPORT			{
						rv.setCsvFile($CSVIMPORT.text);
						rv.setLine($CSVIMPORT.line);
						}
	;
	
singleSelQn returns [SingleSelectQuestion rv = new SingleSelectQuestion() ]
	:	(ATTRIBUTE			{Attrib.addAttribute(rv,$ATTRIBUTE.text,$ATTRIBUTE.line);})*
		LINECONTENTS 			{
						rv.setText($LINECONTENTS.text);
						rv.setLine($LINECONTENTS.line);
						}
		(SINGLEOPTION			{rv.getOptions().add(new Option($SINGLEOPTION.text,$SINGLEOPTION.line));})+
					
	;
	
	
dynamicQnInstance returns [DynamicQuestion rv = new DynamicQuestion() ]
	:	(ATTRIBUTE			{Attrib.addAttribute(rv,$ATTRIBUTE.text,$ATTRIBUTE.line);})*
		LINECONTENTS 			{
						rv.setText($LINECONTENTS.text);
						rv.setLine($LINECONTENTS.line);
						}
		DYNAMICOPTION			{rv.setDynamicInstanceId($DYNAMICOPTION.text);}
					
	;
	
multipleSelQn returns [MultiSelectQuestion rv = new MultiSelectQuestion()]
	:	(ATTRIBUTE			{Attrib.addAttribute(rv,$ATTRIBUTE.text,$ATTRIBUTE.line);})*
		LINECONTENTS 			{
						rv.setText($LINECONTENTS.text);
						rv.setLine($LINECONTENTS.line);
						}
		(MULTIPLEOPTION			{rv.addOption(new Option($MULTIPLEOPTION.text,$MULTIPLEOPTION.line));})+
	;
	
	
txtQn	returns [TextQuestion rv = new TextQuestion()]
	:	(ATTRIBUTE			{Attrib.addAttribute(rv,$ATTRIBUTE.text,$ATTRIBUTE.line);})*
		LINECONTENTS 			{
						rv.setText($LINECONTENTS.text);
						rv.setLine($LINECONTENTS.line);
						}
	;
	
	
ATTRIBUTE
	:	SPACE '@' SPACE LINECONTENTS		{setText(rl($LINECONTENTS.text));}
	;
	
	
STUDYNAME
	: 	SPACE '###' LINECONTENTS	{setText(rl($LINECONTENTS.text));}
	;
	
FORMNAME:	SPACE '##' LINECONTENTS		{setText(rl($LINECONTENTS.text));}
	;
	
PAGE	:	SPACE '#>' LINECONTENTS		{setText(rl($LINECONTENTS.text));}
	;

DYNAMICMARKER
	: 	SPACE ('>>>'|'dynamic{') NEWLINE
	;
	
LEFTBRACE
	: 	SPACE '}' SPACE NEWLINE	
	;
BEGINREPEATMARKER
	:	SPACE ('>>>>'|'repeat{') LINECONTENTS	{setText(rl($LINECONTENTS.text));}
	;

ENDREPEATMARKER
	:	SPACE '>>>>' NEWLINE
	;
	
MULTIPLEOPTION	
	:	SPACE '>>' LINECONTENTS 	{setText(rl($LINECONTENTS.text));}
	;
	
DYNAMICOPTION	
	:	SPACE '$>' LINECONTENTS 	{setText(rl($LINECONTENTS.text));}
	;

SINGLEOPTION	
	:	SPACE '>' LINECONTENTS 		{setText(rl($LINECONTENTS.text));}
	;
	
CSVIMPORT
	:	'csv:import' LINECONTENTS     	{setText(rl($LINECONTENTS.text));}	
	;

SPACE	:	('\t'|' ')*
	;

	
EMPTYLINECOMMENT
	:	('\t'|' ')+ (NEWLINE) 		{$channel=HIDDEN;}
	|	SPACE '//' (NEWLINE | LINECONTENTS){$channel=HIDDEN;}
	;
	
LINECONTENTS 
	:	LINEXTERS+ NEWLINE  		{setText(rl(getText()));}
	;

NEWLINE :	    ((('\r')? '\n' )+) | EOF
	;
	
fragment LINEXTERS 
	:	~('\r'|'\n')
	;
