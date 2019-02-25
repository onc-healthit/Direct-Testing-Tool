# Direct-Testing-Tool
Direct Testing Tool provides capabilities to validate your Direct implementation to applicable standards-specifications. To verify basic Direct send capabilities of your system send a message to the Direct email address.

Getting Started - Local Development


1. Installation
  To get started locally, follow these instructions:

  1.	If you haven't done it already, make a fork of this repo.
  2.	Clone to your local computer using git or svn.
2. Dependency
  1.	Java jdk/jre 1.6 or higher.
  2.	Maven.
  3.	MySQL 5.x or higher
3. Installation Steps
      NOTE: Text in RED BOLD are specific to your local install directory / information and needs to replaced with the corresponding
      values.
  1)	Install Java
      Note: Make sure you have “jre” directory under your JDK install 
       Eg: if your java install director is “c:\java\jdk1.8.0_91”
       
    a.	Set JAVA_HOME variable in the environment variable.
       JAVA_HOME= c:\java\jdk1.8.0_91
    b.	Add Jre to your PATH variable.
        C: \Java\jdk1.8.0_91\jre\bin
  2)	Install maven
    a.	SET M2_HOME
       M2_HOME = C: \apache-maven-3.3.9
    b.	Add maven to your PATH variable
       C:\apache-maven-3.3.9\bin
  3)	Install MySQL 5.6 or higher
     https://dev.mysql.com/downloads/mysql/5.6.html#downloads
     NOTE: please note down the root password 

    a.	Add MYSQL bin to your PATH Variable
       “C:\Program Files\MySQL\MySQL Server 5.7\bin”
    b.	Run DB script
    
    Steps
       In the command problem execute the following command
       c:\> mysql --user=root --password=<provide your root password>
       mysql> create database direct;
       mysql> use direct;
       mysql> create user 'direct'@'localhost' identified by ‘’;
       mysql> GRANT ALL PRIVILEGES ON direct.* TO 'direct'@'localhost';
       mysql> source C:\<dtt_download\database\>\direct_schema.sql
       mysql> show tables;
4.	Creating New Packages
  1)	Download code from Git Hub
    a.	https://github.com/siteadmin/Direct-Testing-Tool
    b.	You should see the following structure
  2)	Compile code
     Eg: if you download the code to “C:\testing\dtt”
  
      Execute the following command from the command prompt twice
      1st time you will see some FAILURE, execute the same command again and you should see all SUCCESS.
      
      mvn clean install -DskipTests
5.	Packages
  Above steps will create a target folder under webapp directory and will have the jar file to run the application.
  1)	Execute application
    a.	Cd webapp\target
    b.	Copy ..\application.properties .
    c.	Copy ..\config.properties .
    d.	Copy ..\announcements.txt .
    e.	Copy ..\release_notes.txt .
    f.	java -jar webapp-0.0.1-SNAPSHOT.jar
 
Once you see the “Started Application” message
Open a browser the type the following URL
  http://localhost:8081/dtt
you should see the application running.
 
Edit properties
1)	Change version number, release date.
   Please edit config.properties file to change the version number and release date.
 
2)	Change Inquires or question email and googlegroup address.
   Please edit applicaiton.properties file to change the Inquires or question email and googlegroup address.

3)	Change Announcements content.
   Please edit announcements.txt file to change content of Announcements
 
4)	Change Release Notes content.
   Please edit release_notes.txt file to change content of Release Notes
 
