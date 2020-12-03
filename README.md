![build](https://github.com/JKatzwinkel/tla-es/workflows/build/badge.svg)
![LINE](https://img.shields.io/badge/line--coverage-60%25-orange.svg)
![METHOD](https://img.shields.io/badge/method--coverage-73%25-yellow.svg)

Thesaurus Linguae Aegyptiae (TLA) backend.

## Overview

The TLA backend server is a Spring Boot application using an Elasticsearch instance as a search engine.


## Installation

> **TL;DR:** run `ES_PORT=9200 SAMPLE_URL=http://aaew64.bbaw.de/resources/sample/sample201111-5000t.tar.gz docker-compose up -d`

There are two methods for getting this thing up and running.

1. [As a Docker container setup](#1-using-docker)
2. [Run or build with Gradle](#2-using-gradle)


### 1. Using Docker

Requirements:

- Docker Compose

#### 1.1. Prerequesites

1. Create an environment variable template file `.env` based on the template coming with this repo:
   ```
   cp .env.template .env
   ```
2. Choose values for the environment variables `ES_HOST` and `ES_PORT` to your liking, e.g. as `localhost` and `9200`
   respectively (or wherever you desire to be able to connect to your Elasticsearch container).

3. Specify the location where a TLA corpus data archive can be downloaded using the `SAMPLE_URL` environment variable, e.g.:
   ```
   SAMPLE_URL=http://example.org/sample.tar.gz
   ```

#### 1.2. Run Setup

Start the docker container setup configured in `docker-compose.yml`:

    docker-compose up -d

This will build and run three containers:

- `tla-es`: Elasticsearch container
- `tla-ingest`: temporarily executed instance of the backend application, used for populating the Elasticsearch container
- `tla-backend`: the actual backend app

The `tla-ingest` container will take its time downloading the TLA corpus data archive file and uploading it into Elasticsearch.
You can check its progress by taking a look into its log output:

    docker logs -f tla-ingest


### 2. Using Gradle

Requirements:

- Java 11
- Elasticsearch 7.9.3 *or* Docker Compose

#### 2.1. Prerequesites

1. This method requires you to provide a running Elasticsearch instance. If you have Docker Compose, you can simply start one in a
   container by using the configuration coming with this repository:
   ```
   docker-compose up -d es
   ```
   Before continuing, make sure Elasticsearch is running by checking the output of `docker ps --all` or
   accessing [its REST interface](http://localhost:9200) in a browser (change `9200` in case that you
   set a different port via the `ES_PORT` environment variable).

2. Nicely done! Now follow [the instructions above](#11-prerequesites) to make sure you have set the environment variables `ES_HOST`, `ES_PORT` and `SAMPLE_URL`.

3. Once Elasticsearch is up and running, TLA corpus data needs to be loaded into it. In order to do so,
you must set the `SAMPLE_URL` environment variable to a URL pointing to a tar-compressed TLA corpus data
file. One way to do this is to create a `.env` file in the directory containing this README, and setting
the variable `SAMPLE_URL` in there:
   ```
   SAMPLE_URL=http://example.org/sample.tar.gz
   ```

4. Finally, download and store TLA corpus data from the specified source by running the `populate` gradle task:
   ```
   ./gradlew populate
   ```
> If you are on a Windows machine, you have to use the `gradlew.bat` wrapper instead.)

#### 2.2. Run application

Run the app using the `bootRun` task:

    ./gradlew bootrun

> If you are on a Windows machine, you need to execute the `gradlew.bat` wrapper shipped with this repository.


## Misc

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

