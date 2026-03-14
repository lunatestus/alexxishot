@ECHO OFF
SETLOCAL
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Missing gradle-wrapper.jar at %WRAPPER_JAR%
  EXIT /B 1
)

IF DEFINED JAVA_HOME (
  "%JAVA_HOME%\bin\java" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
) ELSE (
  java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
)
ENDLOCAL
