call mvnw clean deploy
echo ok to continue with releasing?
pause

call mvnw -N jreleaser:full-release
pause
