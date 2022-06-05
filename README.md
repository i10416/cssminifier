# Simple CSS Minifier in Pure Scala

[![Release](https://github.com/i10416/cssminifier/actions/workflows/release.yml/badge.svg)](https://github.com/i10416/cssminifier/actions/workflows/release.yml)

| scala 2.13                                                                                                                                                                                                                                                                                                                                                                                                        | scala 3                                                                                                                                                                                                                                                                                                                                                                                               |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/dev.i10416/cssminifier_2.13.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/i10416/cssminifier_2.13/)<br/>[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.i10416/cssminifier_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.i10416/cssminifier_2.13) | [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/dev.i10416/cssminifier_3.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/i10416/cssminifier_3/)<br/>[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.i10416/cssminifier_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.i10416/cssminifier_3) |


This library is a simple, dependency free css minifier supporting Scala 2.13, 3 on JVM, JS and Native Platform.

Algorithm is written in reference to YUI
or.

Following features are supported.

- remove leading whitespace-like chars
- remove trailing whitespace-like chars
- remove last semi-colon in braces
- remove repeated semi-colons
- remove comments except ones start with `!`
- remove empty rules
- compress zeros(e.g. `margin:0 0 0 0; => margin:0`,`border:none;=>border:0`)
- collect `@charset` and keep only the first one

## Install

```scala
libraryDependencies += "dev.i10416" %% "cssminifier" % "0.0.2"
```

For JS or Native platform, use `%%%` instead of `%%`.

```scala
libraryDependencies += "dev.i10416" %%% "cssminifier" % "0.0.2"
```

## Run

```scala
import dev.i10416.CSSMinifier

CSSMinifier.run("<css string>")
```

## How to contribute?

- Give it a star⭐
- Drop the feedback to the author @i10416
- Send a PR with fixes of typos/bugs/etc🐛

## License

Licensed under the Apache License, Version 2.0.
