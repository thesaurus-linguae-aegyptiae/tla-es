![Java CI](https://github.com/JKatzwinkel/tla-es/workflows/Java%20CI/badge.svg)
![LINE](https://img.shields.io/badge/line--coverage-55%25-orange.svg)
![METHOD](https://img.shields.io/badge/method--coverage-54%25-orange.svg)

Thesaurus Linguae Aegyptiae (TLA) backend.

## Installation

Requirements:

- Java 11
- Docker Compose


### 1. Install and populate search engine

The TLA backend app requires Elasticsearch 6.8.4. It can be installed via Docker Compose using the
configuration shipped with this repository:

    docker-compose up -d elasticsearch

Before continuing, make sure Elasticsearch is running by checking the output of `docker ps --all` or
accessing [its REST interface](http://localhost:9200) in a browser (change `9200` in case that you
set a different port via the `ES_PORT` environment variable).

Once Elasticsearch is up and running, TLA corpus data needs to be loaded into it. In order to do so,
you must set the `SAMPLE_URL` environment variable to a URL pointing to a tar-compressed TLA corpus data
file. One way to do this is to create a `.env` file in the directory containing this README, and setting
the variable `SAMPLE_URL` in there:

    SAMPLE_URL=http://example.org/sample.tar.gz

Finally, download and store TLA corpus data from the specified source by running the `populate` gradle task:


    gradle populate

(If you are on a Windows machine, you probably need to execute the `gradlew.bat` wrapper shipped with this
project explicitly.)


### 2. Run the app

Run the app via the `bootRun` task:

    gradle bootrun

(If you are on a Windows machine, you probably need to execute the `gradlew.bat` wrapper shipped with this
project explicitly.)


## Misc

Run a dockerized setup using Docker Compose:

    docker-compose up -d elasticsearch
    ./gradlew downloadSample
    docker-compose up -d backend


In order to only use the Elasticsearch container and run the application via gradle:

    docker-compose up -d elasticsearch
    ./gradlew populate bootRun

*Note:* You can configure the Elasticsearch HTTP port to which the application will try to connect.
Both the Docker Compose configuration and the `bootRun` and `test` gradle tasks are going to read
it from the local `.env` file.

When running the application using the  `bootRun` task, comma-separated arguments can be passed via
`args` property in the following ways:

    ./gradlew bootRun --Pargs=--data-file=sample.tar.gz,--foo=bar
    ./gradlew bootRun --args="--data-file=sample.tar.gz --foo=bar"


Populate database with a corpus dump and shut down after:

    ./gradlew bootRun --args="--date-file=sample.tar.gz --shutdown"

There is a gradle task for populating the backend app's elasticsearch indices with corpus data obtained
from a URL specified via the `SAMPLE_URL` environment variable:

    ./gradlew populate


You can check for the newest version of package dependencies by running:

    ./gradlew dependencyUpdates


