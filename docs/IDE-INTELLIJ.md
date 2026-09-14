# Opening the project in IntelliJ IDEA

> ⚠️ **First of all: IntelliJ must use JDK 8.** ForgeGradle 2.3 and Gradle 4.10.3 fail on any
> newer JDK. This is the first thing to set, and the cause of almost every import failure.

## 1. Import

1. Close the dev client if it is running.
2. In IntelliJ: **File → Open**, then pick the folder
   `C:\Users\Julien\Desktop\PackagedAutoTerminals`.
3. IntelliJ detects `build.gradle` and offers the Gradle import. Accept it.

## 2. Set the two JDKs

| Setting | Value | Where |
|---|---|---|
| **Gradle JVM** | `C:\Program Files\Eclipse Adoptium\jdk-8.0.472.8-hotspot` | Settings → Build, Execution, Deployment → Build Tools → Gradle |
| **Project SDK** | the same JDK 8, language level **8** | File → Project Structure → Project |
| **Gradle distribution** | **Use Gradle from: gradle-wrapper.properties** | same page as the Gradle JVM |

> If IntelliJ refuses Gradle 4.10.3 because it is too old, keep the wrapper and ignore the
> warning. **Never** update the wrapper: ForgeGradle 2.3 does not go beyond it.

## 3. The four run configurations provided

They are versioned in `.idea/runConfigurations/` and appear in the drop-down menu at the top
right.

| Configuration | Type | What it does |
|---|---|---|
| **Gradle runClient** | Gradle | starts the dev client through the `runClient` task. **This is the recommended path.** |
| **Gradle build** | Gradle | builds the jar |
| **Minecraft Client** | Application | starts `GradleStart` directly, for step debugging |
| **Minecraft Server** | Application | starts `GradleStartServer` |

All of them work in the `run` folder, so the world, the key bindings and the `PAT_Dev` user
name are kept from one session to the next.

### Which configuration to choose

- **To test**: `Gradle runClient`. It goes through ForgeGradle, so the mod version is
  injected and the resources are placed correctly.
- **To debug**: `Minecraft Client`, with the *Debug* button. Breakpoints work, and so does
  hot swapping of methods.

> With `Minecraft Client`, the mod version shows as `@MOD_VERSION@` in the mod list. That is
> expected: the substitution is done by ForgeGradle, which this configuration bypasses.
> Nothing else changes.

## 4. If an "Application" configuration does not start

The module name must match the one IntelliJ created at import time, normally
`packagedautoterminals.main`. If it differs, open the configuration and pick the right module
from the list.

You can also let ForgeGradle regenerate them, once the project is imported:

```
gradlew genIntellijRuns
```

This task writes nothing until IntelliJ has imported the project. That is why the four files
above are provided up front.

## 5. What Git tracks, and what it ignores

`.gitignore` ignores all of `.idea/`, **except** `.idea/runConfigurations/`. The run
configurations are therefore shared, and the personal settings stay local.
