#parse("File Header Java Class.java")

#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

/*--------------------------------------
    IMPORT LIST
--------------------------------------*/
#if (${IMPORT_BLOCK} != "")${IMPORT_BLOCK}
#end
#if (${VISIBILITY} == "PUBLIC")public #end #if (${ABSTRACT} == "TRUE")abstract #end #if (${FINAL} == "TRUE")final #end class ${NAME} #if (${SUPERCLASS} != "")extends ${SUPERCLASS} #end #if (${INTERFACES} != "")implements ${INTERFACES} #end {
#parse("Java Class Sections")

}
