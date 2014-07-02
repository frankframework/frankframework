Release on Windows 7 behind a proxy

- Download and install http://git-scm.com/download/win (1.8.3 used at time of
  writing) in e.g. D:\Software\Installed\Git
- Download and install http://maven.apache.org/download.cgi (3.0.5 used at time
 of writing) in e.g. D:\Software\Installed\Maven\apache-maven-3.0.5
- Adjust D:\Software\Installed\Maven\apache-maven-3.0.5\conf\settings.xml with
  your proxy settings.
- Start cmd.exe
- Adjust Screen Buffer Size (Width: 140, Height: 9999) and Window Size
  (Width: 140, Height: 75)
- set JAVA_HOME=D:\Software\Installed\Java\jdk1.5.0_22
- set PATH=D:\Software\Installed\Git\bin\;D:\Software\Installed\Maven\apache-maven-3.0.5\bin
- set MAVEN_OPTS=-Xmx512m -XX:MaxPermSize=128m
- git config --list
- Check and when needed set the following settings.
- git config --global user.name "[first-name] [last-name]"
- git config --global user.email [first-name].[last-name]@ibissource.org
- git config --global http.proxy http://[proxy-username]:[proxy-password]@[proxy-host]:[proxy-port]
- git config --global https.proxy http://[proxy-username]:[proxy-password]@[proxy-host]:[proxy-port]
- git config --global core.autocrlf true
- cd D:\Temp (or any other folder)
- git clone https://github.com/ibissource/iaf.git
- git clone https://github.com/ibissource/mvn-repo.git
- git clone https://[user]@bitbucket.org/ibissource/mvn-repo-proprietary.git
- cd iaf
- mvn release:prepare -DpushChanges=false
- git push
- git push --tags
- mkdir target
- cd target
- git clone https://github.com/ibissource/mvn-repo.git
- git clone https://[user]@bitbucket.org/ibissource/mvn-repo-proprietary.git
- cd ..
- mvn release:perform
- cd target
- cd mvn-repo
- git add .
- git commit
- git status
- i, Release ibis-adapterframework-parent-[version], esc, :, wq, enter
- git push
