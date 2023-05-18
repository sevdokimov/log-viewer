@echo off

cd %cd%

java -ea -Dlog-viewer.config-file=%cd%/config.conf -jar %cd%/lib/log-viewer-cli-${project.version}.jar startup
