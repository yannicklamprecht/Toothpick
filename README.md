# Toothpick

You really don't want to touch this yet.

This project will allow you to create a paper fork using mojang mappings, rather than spigot mappings.  
This means, that **every single** class/method/field is renamed to the origonal name, the minecraft developers use.  
This makes it much easier to work on stuff.  

An intended effect of this is, that all plugins build against this project, will also be able (or be forced, depends on your view, lol) to use mojang mappings.  
That means, that almost every single public plugin breaks (every plugin that accesses nms), but makes it makes it much easier to write plugins that use internals.

## TODO

* Long2ObjectMap patch shit
* figure out why not all classes end up remapped
* copy missing files to server folder (pom.xml for example) (actually, it needs to be a git repo, upps, copy everything, delete src)
* make it compile, duh

## Building

Figure it out yourself or GTFO (for now, pls come back later when this is usable :))

Tasks:
* setupUpstream -> does paper stuff
* mojangMappings -> does the special juice
* applyPatches -> applies toothpick patches ontop of mojang mappings
* rebuildPatches -> rebuilds patches, duh
* cleanUp -> deletes everything in the work dirs and you will have to run ^ again

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
