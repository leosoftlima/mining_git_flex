mining_git
==========

Java program to search for GitHub repositories using Gherkin-based acceptance test tool, such as Cucumber, that maintain a link among tasks and code changes. Such a link is defined by the occurrence of task ID in commit messages. That is, if it possible to identify changes at the production code and at the test code related to a same task, which in turn is identified by an ID. The task is a programming activity, such as the development of a new feature, a feature change, bug fix or refactoring.

Search mechanism
-
The search mechanism is very simple. The program takes as input the search criteria: project id (id of the repository to run the queries under Google BigQuery service) and programming language.
As result, the program outputs a csv file (/input/projects.csv) containing the URL (column repository_url) and master branch (column repository_master_branch) of found repositories.
Then, the program downloads each repository's source code (a zip file) and verifies if it contains Gherkin files (extension .feature). If a repository does contain Gherkin files, the program downloads all repository data and searches for commit messages containing task ID.
Finally, for each found commit, the program identifies the changed code (production and test code) by using GitMiner and Gremlin. 

The output could be accessed in /output folder.
candidate-projects.csv provides data about repositories containing Gherkin files.
selected-projects.csv provides data about repositories containing Gherkin files and commit messages with task ID. 

More about Gherkin and Cucumber: https://github.com/cucumber/cucumber/wiki/Gherkin.

More about Google BigQuery: https://cloud.google.com/bigquery/what-is-bigquery.

More about GitMiner:https://github.com/pridkett/gitminer

More about Gremlin:https://github.com/tinkerpop/gremlin/wiki

Requirement to use Google BigQuery API
-
The environment variable GOOGLE_APPLICATION_CREDENTIALS is checked. If this variable is specified it should point to a file that defines the credentials. The simplest way to get a credential for this purpose is to create a service account using the Google Developers Console in the section APIs & Auth, in the sub-section Credentials. Create a service account or choose an existing one and select Generate new JSON key. Set the environment variable to the path of the JSON file downloaded.

https://developers.google.com/identity/protocols/application-default-credentials

It is also necessary to provide searching criteria data (project id and language) by configuring the properties file at src/resources path (properties spgroup.bigquery.project.id and spgroup.language). 

Requirement to use GitMiner
-
It is necessary to provide GitHub user data (login, password, e-mail and security token) by configuring the properties file at src/resources path (properties net.wagstrom.research.github.login, net.wagstrom.research.github.password, net.wagstrom.research.github.email and net.wagstrom.research.github.token).

Compilation
-
This project uses Apache Maven to manage all dependencies and versioning. 

More about Maven: https://maven.apache.org/

Execution
-

(1) Generate the jar file (MiningGit-1.0-jar-with-dependencies.jar)by using Maven

(2) Locate the jar and the configuration.properties file at target folder

(3) Configure the properties file

(4) Run the jar by command line: java -jar MiningGit-1.0-jar-with-dependencies.jar


To remember: The jar and the properties file must be at the same folder.