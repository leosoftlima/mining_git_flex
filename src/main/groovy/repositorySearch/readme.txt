Requirement to use Google BigQuery API

The environment variable GOOGLE_APPLICATION_CREDENTIALS is checked.
If this variable is specified it should point to a file that defines the credentials.
The simplest way to get a credential for this purpose is to create a service account using the Google Developers Console
in the section APIs & Auth, in the sub-section Credentials. Create a service account or choose an existing one and select
Generate new JSON key. Set the environment variable to the path of the JSON file downloaded.

https://developers.google.com/identity/protocols/application-default-credentials