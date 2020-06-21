# Toothpick

You really don't want to touch this yet.

This project will allow you to create a paper fork using mojang mappings, rather than spigot mappings.  
This means, that **every single** class/method/field is renamed to the original name, the minecraft developers use.  
This makes it much easier to work on stuff.  

An intended effect of this is, that all plugins built against this project, will also be able (or be forced, depends on your view, lol) to use mojang mappings.  
That means, that almost every single public plugin breaks (every plugin that accesses nms), but makes it makes it much easier to write plugins that use internals.

## TODO

* cleanup
* fix tests (need to remap tests)

## Building

This uses gradle. Interesting Tasks:
* `setupUpstream` -> does paper stuff
* `mojangMappings` -> does the special juice
* `applyPatches` -> applies toothpick patches ontop of mojang mappings
* `rebuildPatches` -> rebuilds patches, duh
* `cleanUp` -> deletes everything in the work dirs and you will have to run ^ again

You can run those in both windows and linux env, but you shouldn't fix and match (so once you ran `setupUpstream` in WSL you can't run it again in windows until you run `cleanUp`)

### Steps for building

    Pre-requirements
    - JDK-1.8
    - JDK-11+
    - Maven (and Gradle)
    - setup JAVA_HOME

1. `git clone https://github.com/CadixDev/Atlas` (could require JDK-1.8 for compiling)
2. `cd Atlas && ./gradlew build install`
3. `git clone https://github.com/CadixDev/Lorenz/` (could require JDK-1.8 for compiling)
4. `git checkout fix/mercury-14`
5. `cd Mercury/ && ./gradlew build install`
6. After building the dependencies `git clone https://github.com/MiniDigger/Toothpick` (requires JDK-11+ for compiling)
7. run (under Windows use `./gradle.bat` instead of `./gralew` 
    1. `./gradlew setupUpstream`
    2. `./gradlew mojangMappings`
    3. `./gradlew applyPatches`
    4. `./gradlew shadowJar`

### maven local install

 1. Execute `./gradlew installLocalMaven` after step 7.4 in [Steps for building](#steps-for-building) 

### maven repo deploy

 *Requirements*
  
- having a valid `settings.xml` in the `~/.m2/`

1. Modify the constants in `src/main/kotlin/stuff/Constants.kt`

    ```
    const val repoId = "01"
    const val repoUrl = "https://nexus.server-project.net/repository/maven-snapshots/"
    ```
2. Execute `./gradlew deployMaven` after step 7.4 in [Steps for building](#steps-for-building)



## License

MIT

## Acknowledgements

I would like to thank @jamierocks and the whole team behind @CadixDev for their fantastic libraries and for holding my hand using them.

Without them, this would have been much harder.
In particular, Toothpick uses:
 * [Lorenz](https://github.com/CadixDev/Lorenz) to read, write and convert between different mapping formats
 * [Mercury](https://github.com/CadixDev/Mercury) for applying those mappings to the paper source
 * [Atlas](https://github.com/CadixDev/Atlas) for applying those mappings to the vanilla server jar
 
I would also like to thank all upstream projects for providing the base for this project.
 
Also a huge thanks for Dinnerbone and the team at mojang for a) making this game and b) giving us the obfuscation maps.
