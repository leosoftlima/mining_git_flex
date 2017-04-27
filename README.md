mining_git
==========

Groovy program to search tasks from GitHub repositories. In such context, a task is a set of commits whose message contains an specific ID or
a set of commits of a merge scenario. The program can filter repositories by program language, creation date, stars, ID in commit message and file type.
In case of Ruby projects, the program can also filter repositories by required gems.
In this sense, the program is organized into 3 steps: (i) searching for repositories; (ii) filtering repositories; (iii) searching for tasks.
Respectively, the output of each step is saved into separated folders: _1-github_, _2-filtered_, _3-tasks_.
The program also generates auxiliary folders to internal usage: _repositories_, _zipped_, _unzipped_.
The execution logging can be verified at _output/execution.log_.

Mechanism
-
The mechanism of searching for repositories depends the search criteria adopted.
To search repositories by language, creation date and stars, the program uses _GitHub Search API_.
Because GitHub API does not support searching by commit message, in such a case the program uses _Google BigQuery_.
Then, to filter repositories by file type and required gems, the program downloads each repository's zipball 
(considering the current version of main branch) and verifies file extensions and Gemfile content, respectively.
Finally, to search for tasks (by ID or merge scenario), the program clones each repository that satisfies search criteria and groups its commits. 
Then, for each task, the program identifies changed code (production and test code) by using _JGit_. 

More about _Google BigQuery_: https://cloud.google.com/bigquery/what-is-bigquery.

More about _GitHub Search API_: https://developer.github.com/v3/search/.

More about _JGit_: https://eclipse.org/jgit/.

Requirement to use Google BigQuery API
-
The environment variable GOOGLE_APPLICATION_CREDENTIALS is checked. If this variable is specified it should point to a file that defines the credentials. The simplest way to get a credential for this purpose is to create a service account using the Google Developers Console in the section APIs & Auth, in the sub-section Credentials. Create a service account or choose an existing one and select Generate new JSON key. Set the environment variable to the path of the JSON file downloaded.

https://developers.google.com/identity/protocols/application-default-credentials

It is also necessary to provide searching criteria data (project id and language) by configuring the properties file at src/resources path (properties spgroup.bigquery.project.id and spgroup.language). 

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


**To remember: The jar and the properties file must be at the same folder.**

Configuration (configuration.properties file)
-
`spgroup.bigquery.project.id`: Identification of the repository used to run queries under Google BigQuery service.

`spgroup.project.search.commit.message`: Criteria to filter repositories by commit message.
Valid values: _default_ (ID according to issue style), pivotal (ID according to PivotalTracker style), _false_ or _empty_.

`spgroup.github.user`: Username of valid GitHub account.

`spgroup.github.password`: Password of valid GitHub account.

`spgroup.project.search.stars`: Maximum value of repository's stars.

`spgroup.project.search.language`: Repository's program language.

`spgroup.project.search.year`: Minimum year for repository's creation date.

`spgroup.project.filter.gems`: Gems of interest (only for Rails projects).

`spgroup.project.filter.file`: File type of interest. 

`spgroup.search.projects`: Enable/disable the search mechanism for GitHub repositories. Valid values: true or false.

`spgroup.filter.projects`: Enable/disable the filter mechanism for GitHub repositories. Valid values: true or false.

`spgroup.search.tasks`: Enable/disable the search mechanism for tasks. Valid values: true or false.