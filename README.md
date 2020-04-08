![Java CI](https://github.com/JKatzwinkel/tla-es/workflows/Java%20CI/badge.svg)
![LINE](https://img.shields.io/badge/line--coverage-51%25-orange.svg)
![METHOD](https://img.shields.io/badge/method--coverage-47%25-orange.svg)

You can check for the newest version of package dependencies by running:

    ./gradlew dependencyUpdates


Run a dockerized setup using Docker Compose:

    docker-compose up -d


In order to only use the Elasticsearch container and run the application via gradle:

    docker-compose up -d elasticsearch
    ./gradlew bootRun

*Note:* You can configure the Elasticsearch HTTP port to which the application will try to connect.
Both the Docker Compose configuration and the `bootRun` and `test` gradle tasks are going to read
it from the local `.env` file.

When running the application using the  `bootRun` task, comma-separated arguments can be passed via
`args` property like this:

    ./gradlew bootRun -Pargs=--data-file=sample.tar.gz,--foo=bar

Populate database with a corpus dump:

    ./gradlew bootRun -Pargs=--date-file=sample.tar.gz
