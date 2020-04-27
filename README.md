![Java CI](https://github.com/JKatzwinkel/tla-es/workflows/Java%20CI/badge.svg)
![LINE](https://img.shields.io/badge/line--coverage-56%25-orange.svg)
![METHOD](https://img.shields.io/badge/method--coverage-54%25-orange.svg)

Initialize the project by creating a `.env` file with at least the corpus data source specified:

    SAMPLE_URL=http://example.org/sample.tar.gz


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


