mining_git
==========

Java code to search for GitHub projects using Gherkin-based acceptance test tool, such as Cucumber.
It takes as input a csv file (/input/projects.csv) identifying the projects by URL (column repository_url) and master branch (column repository_master_branch).
The search process is very simple: The code downloads the project's source code (a zip file) and verifies if it contains feature files (extension .feature).
The output could be accessed in /zipped and /unzipped folders.

More about Gherkin and Cucumber: https://github.com/cucumber/cucumber/wiki/Gherkin.
