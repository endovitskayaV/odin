# Odin server

[`processors`](https://github.com/clulab/processors) library version is 6.3.0, used as submodule

## What is it?

An `akka-http` server exposing a REST API for text annotation via the [`processors`](https://github.com/clulab/processors) library forked from [processors server](https://github.com/clu-ling/processors-server)

## Requirements
1. [Java 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
2. [`sbt`](http://www.scala-sbt.org/download.html)
3. [`npm`](https://docs.npmjs.com/cli/install)

## Running `processors-server`

Fire up the server. This may take a several minutes or so to load the large model files and parse rules.<br>
Use run configuration _main_ for IntellijIdea or execute the following:
```bash
cd odins_runes
sbt "runMain NLPServer"
```

By default, the server will run on port `8888` and `localhost`, though you can start the server using a different port and host:

```bash
sbt "runMain NLPServer --host <your favorite host here> --port <your favorite port here>"
```

For further details visit [processors server](https://github.com/clu-ling/processors-server)


**!!!Review annotators src/main/scala/org/clulab/processors/Processor.scala. You may need to add extra (recognizeNamedEntities, for example)!!!**