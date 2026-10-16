# Avengers Deployment Facade

Assignment 4 – Facade pattern. A Java console app where one call, "deploy this drone on this mission", runs three separate subsystems in order. State is saved to disk, and if a step fails, the earlier steps are undone.

## How it works

The chain has three steps, each in its own subsystem:

**A – Inventory:** checks the drone ID and reserves the drone.
**B – Diagnostics:** runs a diagnostic and saves the report.
**C – Clearance:** checks the mission code and approves the mission.

Each subsystem has an entity with a status, a service with the rules, and a repository that saves to disk. They don't know about each other. Only the facade uses all three.

`DeploymentFacade` runs A → B → C, saves progress after each step, and undoes things if a step fails.

`AdminFacade` is a smaller facade for inspecting a deployment and undoing it by hand.

`Main` is the console menu.

The chain's progress (`STARTED`, `PENDING_B`, `PENDING_C`, `COMPLETED`, `COMPENSATED`) is also saved, so it survives a restart.

## Undoing a failed deployment

Undo goes in reverse order (C, B, A). Each undo step looks for its own saved record and skips itself if there isn't one, so only what was really saved gets undone.

- **Fails at B:** only inventory is undone.
- **Fails at C:** diagnostics, then inventory, are undone.
- **Bad drone ID or mission code:** rejected before anything is saved.

Quick demo (mission `M-AVENGER7`): option 1 with `DRONE-01` succeeds, option 2 fails at B, option 3 fails at C, option 4 inspects a chain ID, option 5 undoes a completed deployment. After a failure, the chain ID is the file name in `data/chains/`.

Least Knowledge: before and after
Before, the client had to use everything itself:

InventoryService inventory = new InventoryService();
DiagnosticService diagnostics = new DiagnosticService();
ClearanceService clearance = new ClearanceService();

After, it talks to one object:

DeploymentFacade deployment = new DeploymentFacade(Path.of("data"));
String id = deployment.deploy("DRONE-01", "M-AVENGER7", false, false);

## Facade vs Adapter

An Adapter changes one interface into another. Nothing here needs that, because the three services are fine on their own. The problem is that using them together means getting the order right and knowing what to undo when B or C fails. A Facade hides exactly that behind one `deploy` call.

## Tests

```bash
mvn test
```