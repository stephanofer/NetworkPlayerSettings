cloud-paper#
cloud-paper is an extension of cloud-bukkit with additional support for Paper-based platforms. cloud-paper maintains support for all platforms supported by cloud-bukkit, and therefore is the recommended dependency to use Cloud on any Bukkit-based platform. The following documentation is written with the assumption that you have already read and understand the cloud-bukkit documentation.

Links#
 Source Code
 JavaDoc
 Example Plugin
Installation#
Cloud for Paper is available through Maven Central.


Maven
Gradle (Kotlin)
Gradle (Groovy)

implementation(platform("org.incendo:cloud-minecraft-bom:2.0.0"))
implementation("org.incendo:cloud-paper")

Usage#
cloud-paper has two different command manager implementations:

PaperCommandManager: Paper command API
LegacyPaperCommandManager: Legacy command API
PaperCommandManager should be preferred when targeting Paper 1.20.6+ exclusively. The new manager allows registering commands at bootstrapping time in addition to onEnable, which allows for using those commands in datapack functions.

If the plugin is targeting older Paper versions or non-paper servers, then LegacyPaperCommandManager should be used.

Plugin Configuration Files

Do not register your commands in your plugin.yml or paper-plugin.yml, Cloud handles the registration itself and doing it yourself will cause issues.

Legacy#
The legacy command manager can be instantiated in two different ways.

With a custom sender type:


LegacyPaperCommandManager<YourSenderType> commandManager = new LegacyPaperCommandManager<>(
  yourPlugin, /* 1 */
  executionCoordinator, /* 2 */
  senderMapper /* 3 */
);
Or, using Bukkit’s CommandSender:


LegacyPaperCommandManager<CommandSender> commandManager = LegacyPaperCommandManager.createNative(
  yourPlugin, /* 1 */
  executionCoordinator /* 2 */
);
You need to pass an instance of the plugin that is constructing the command manager. This is used to register the commands and the different event listeners.
Information about execution coordinators in general can be found here. See below for info specific to Bukkit-based platforms.
The sender mapper is a two-way mapping between Bukkit’s CommandSender and your custom sender type. Using SenderMapper.identity() is equivalent to the createNative static factory method.
Modern#
The modern command manager is created using a builder. You may either use the native CommandSourceStack:


PaperCommandManager<CommandSourceStack> commandManager = PaperCommandManager.builder()
  .executionCoordinator(executionCoordinator)
  .buildOnEnable(javaPlugin);
  // or: .buildBootstrapped(bootstrapContext);
or a custom type:


PaperCommandManager<YourSenderType> commandManager = PaperCommandManager.builder(senderMapper)
  .executionCoordinator(executionCoordinator)
  .buildOnEnable(javaPlugin);
  // or: .buildBootstrapped(bootstrapContext);
Brigadier#
Paper exposes Brigadier, which means that you may use the features from cloud-brigadier on Paper servers. When using the modern Paper manager, you do not need to explicitly enable Brigadier.

When using the legacy command manager you may enable Brigadier mappings using LegacyPaperCommandManager#registerBrigadier(). You should make use of the capability system to make sure that Brigadier is available on the server your plugin is running on:


if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
  commandManager.registerBrigadier();
}
Asynchronous completions#
Note

You should not use asynchronous completions together with Brigadier. Brigadier suggestions are already non-blocking, and the asynchronous completion API reduces the fidelity of suggestions compared to Brigadier alone.

Paper allows for non-blocking suggestions. You are highly recommended to make use of this, as Cloud will invoke the argument parsers during suggestion generation which ideally should not take place on the main server thread.

You may enable asynchronous completions using LegacyPaperCommandManager#registerAsynchronousCompletions(). You should make use of the capability system to make sure that this is available on the server your plugin is running on:


if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
  commandManager.registerAsynchronousCompletions();
}
Execution coordinators#
Due to Bukkit blocking the main thread for suggestion requests, it’s potentially unsafe to use anything other than ExecutionCoordinator.nonSchedulingExecutor() for ExecutionCoordinator.Builder#suggestionsExecutor(Executor). Once the coordinator, a suggestion provider, parser, or similar routes suggestion logic off of the calling (main) thread, it won’t be possible to schedule further logic back to the main thread without a deadlock. When Brigadier support is active, this issue is avoided, as it allows for non-blocking suggestions. Paper’s asynchronous completions API can also be used to avoid this issue, however when Brigadier is available it should be preferred (for reasons mentioned above).

Example code to avoid this problem:


if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
  commandManager.registerBrigadier();
} else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
  commandManager.registerAsynchronousCompletions();
}
// else: we can't avoid the problem, very old Paper or Spigot
Parsers#
cloud-paper has access to all the parsers from cloud-bukkit.

Provided Sender Mapper#
Cloud includes a built-in sender mapper designed for the command manager. Due to the CommandSourceStack having no exposed implementations it can be difficult to work, here’s an example of creating a command manager with the sender mapper and using the provided mapped sender:


PaperCommandManager<Source> commandManager = PaperCommandManager
  .builder(PaperSimpleSenderMapper.simpleSenderMapper())
  .executionCoordinator(executionCoordinator)
  .buildOnEnable(javaPlugin);
  // or: .buildBootstrapped(bootstrapContext);

  // this command will only be available to players, and the player type is directly available.
  commandManager.command(commandManager.commandBuilder("player_command")
    .senderType(PlayerSource.class)
    .handler(context -> {
      Player player = context.sender().source();
      player.sendMessage("Hello, player!");
    })
  );
This will give you access to Source with the included extensions: PlayerSource, ConsoleSource, EntitySource and GenericSource
