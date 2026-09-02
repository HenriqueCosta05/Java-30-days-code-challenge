# Task Tracker CLI

Command-line task tracker. Stores tasks in `tasks.json` in this directory.
No external libraries — JSON parsing/writing is hand-rolled.

Built with a Clean Architecture layering:

```
src/
  domain/          entities, invariants (Task, TaskStatus)
  application/      use cases + TaskRepository port
  adapters/cli/      CLI controller (arg parsing, output)
  infrastructure/json/ JSON file repository (implements the port)
  Main.java          composition root
```

## Build

```bash
javac -d out $(find src -name "*.java")
```

(PowerShell: `javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`)

## Run

```bash
java -cp out Main <command> [arguments]
```

## Commands

```bash
task-cli add "Buy groceries"
task-cli update 1 "Buy groceries and cook dinner"
task-cli delete 1

task-cli mark-in-progress 1
task-cli mark-done 1

task-cli list
task-cli list done
task-cli list todo
task-cli list in-progress
```

Non-zero exit code on error (bad/missing args, unknown task id, unknown command).
