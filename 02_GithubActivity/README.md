# GitHub Activity CLI

Prints the recent public activity of a GitHub user in the terminal.
No external libraries — the HTTP call uses `java.net.http`, and the JSON
reader is hand-rolled.

Built with a Clean Architecture layering:

```
src/
  domain/                       entities and invariants
                                (GithubUsername, RepositoryName,
                                 ActivityEvent, ActivityType, ActivityFeed)
  application/showuseractivity/ the use case, its request/response models
                                and the ports it needs
                                (GithubActivityGateway, ActivityCache)
  adapters/cli/                 argument parsing, presenter, exit codes
  infrastructure/github/        GitHub REST gateway + event mapping
  infrastructure/json/          the JSON reader
  infrastructure/cache/         the on-disk cache
  Main.java                     composition root
test/                           the suite, no framework required
```

Dependencies point inward only: the domain knows nothing about HTTP, JSON,
files or the terminal, and the use case talks to GitHub and to the cache
through ports it owns.

## Build

```bash
javac -d out $(find src -name "*.java")
```

(PowerShell: `javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`)

## Run

```bash
java -cp out Main <username> [options]
```

## Usage

```bash
github-activity kamranahmedse
github-activity kamranahmedse --limit=10
github-activity kamranahmedse --type=push
github-activity kamranahmedse --refresh
github-activity --help
```

Output:

```
Recent activity for kamranahmedse:
- Pushed 3 commits to kamranahmedse/developer-roadmap
- Opened a new issue in kamranahmedse/developer-roadmap
- Starred kamranahmedse/developer-roadmap
```

Repeated events of the same kind against the same repository are rolled up,
so three separate one-commit pushes read as one line of three commits.

## Options

| Option | Meaning |
| --- | --- |
| `--type=<activity>` | show only one kind of activity |
| `--limit=<number>` | show at most this many lines |
| `--refresh` | ignore the cache and ask GitHub again |
| `--help` | show the usage |

Activity types: `push`, `issue-opened`, `issue-closed`, `issue-reopened`,
`issue-comment`, `pr-opened`, `pr-closed`, `pr-review`, `star`, `fork`,
`create`, `delete`, `release`, `public`, `member`.

## Caching

A fetched feed is kept for 10 minutes under `.github-activity-cache/`, one
tab separated file per user. A cached answer is labelled with its age, and
`--refresh` skips it. A missing, unreadable or corrupted cache file is
treated as a miss, never as a failure.

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | the activity was printed |
| 1 | the command line was wrong (bad option, bad username) |
| 2 | no such GitHub user |
| 3 | GitHub could not be reached, or refused the request |

## Tests

```bash
javac -d out $(find src -name "*.java")
javac -cp out -d test-out $(find test -name "*.java")
java -cp "out;test-out" TestRunner
```

(On Linux and macOS the classpath separator is `:` rather than `;`.)

The domain, the use case, the presenter and the controller are tested
without network, files or a framework; the JSON reader, the event mapping
and the cache are tested against their real details.
